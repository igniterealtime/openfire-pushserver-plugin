package org.igniterealtime.openfire.plugins.pushserver

import org.dom4j.Element
import org.dom4j.QName
import org.igniterealtime.openfire.plugins.pushserver.dao.PushServerDao
import org.igniterealtime.openfire.plugins.pushserver.models.PushRecord
import org.slf4j.LoggerFactory
import org.xmpp.forms.DataForm
import org.xmpp.forms.FormField
import org.xmpp.packet.IQ
import org.xmpp.packet.PacketError

class PushServerIQHandler(private val pushManager: PushManager) {

    private val logger = LoggerFactory.getLogger(PushServerIQHandler::class.java)

    enum class Node {
        pubsub, command;
    }

    enum class Command(val prefix: String) {
        register("register-push-"), unregister("unregister-push-");

        companion object {
            fun from(code: String) = values().firstOrNull { code.startsWith(it.prefix) }
        }
    }

    fun handleIQ(iq: IQ): IQ {
        return iq.childElement?.let { childElement ->
            when(Node.values().firstOrNull { it.name == childElement.name }) {
                Node.command -> childElement.attributeValue("node")?.let { commandCode ->
                    when (Command.from(commandCode)) {
                        Command.register -> register(iq, commandCode)
                        Command.unregister -> unregister(iq)
                        else -> null
                    }
                } ?: iq.createError(PacketError.Condition.bad_request)
                Node.pubsub -> sendPush(iq)
                else -> iq.createError(PacketError.Condition.bad_request)
            }
        } ?: IQ.createResultIQ(iq).apply {
            error = PacketError(PacketError.Condition.bad_request, PacketError.Type.modify
                , "IQ stanzas of type 'get' and 'set' MUST contain one and only one child element (RFC 3920 section 9.2.3).")
        }
    }

    private fun register(iq: IQ, commandCode: String): IQ {
        val service = PushManager.Service.of(commandCode, Command.register).getOrElse {
            logger.error("Service couldn't be initialized.", it)
            return iq.createError(PacketError.Condition.bad_request)
        }

        val data = iq.data()
        val token = data["token"]
        val deviceId = data["device-id"]

        if (token == null || deviceId == null) {
            logger.error("'token' and 'device-id' should be set.")
            return iq.createError(PacketError.Condition.bad_request)
        }

        val domain = iq.from.domain

        return PushServerDao.addPushRecord(PushRecord(domain, deviceId, token, service))?.let { pushRecord ->
            IQ.createResultIQ(iq).apply {
                this.setChildElement("command", "http://jabber.org/protocol/commands").apply {
                    addAttribute("action", "complete")
                    addAttribute("node", commandCode)

                    add(DataForm(DataForm.Type.form).apply {
                        addField("node", null, FormField.Type.text_single).addValue(pushRecord.node)
                        addField("secret", null, FormField.Type.text_single).addValue(pushRecord.secret)
                    }.element)
                }
            }
        } ?: iq.createError(PacketError.Condition.item_not_found)
    }

    fun unregister(iq: IQ): IQ {
        val deviceId = iq.data()["device-id"] ?: kotlin.run {
            logger.error("'device-id' should be set.")
            return iq.createError(PacketError.Condition.bad_request)
        }

        val domain = iq.from.domain
        val isSuccessful = PushServerDao.deletePushRecord(domain, deviceId)

        return if (isSuccessful == true) {
            IQ.createResultIQ(iq).apply {
                setChildElement("command", "http://jabber.org/protocol/commands")
                    .addAttribute("action", "complete")
            }
        } else {
            iq.createError(PacketError.Condition.internal_server_error)
        }
    }

    fun sendPush(iq: IQ): IQ {

        val domain = iq.from.domain

        val pubsubElement = iq.childElement
        val publishElement = pubsubElement.element("publish") ?: kotlin.run {
            logger.error("'publish' element should be set.")
            return iq.createError(PacketError.Condition.bad_request)
        }

        val node = publishElement.attributeValue("node") ?: kotlin.run {
            logger.error("'node' element should be set.")
            return iq.createError(PacketError.Condition.bad_request)
        }

        val messageId = publishElement
            .element("item")
            ?.element(QName.get("notification", "urn:xmpp:push:0"))
            ?.element(QName.get("messageId", "siper:push:0"))
            ?.stringValue ?: kotlin.run {
                logger.error("'messageId' element should be set.")
                return iq.createError(PacketError.Condition.bad_request)
            }

        val publishOptionsElement = pubsubElement.element("publish-options") ?: kotlin.run {
            logger.error("'publish-options' element should be set.")
            return iq.createError(PacketError.Condition.bad_request)
        }

        val data = publishOptionsElement.dataMap()
        val secret = data["secret"]
        val isSandbox = data["sandbox"] == "true"

        return PushServerDao.getPushRecord(domain, node)?.let { pushRecord ->
            if (pushRecord.secret == secret) {
                val isSuccessful = pushManager.sendPush(
                    PushManager.Service.of(pushRecord.type)
                    , messageId
                    , pushRecord.token
                    , isSandbox
                )

                if (isSuccessful == true) {
                    IQ.createResultIQ(iq)
                } else {
                    iq.createError(PacketError.Condition.recipient_unavailable)
                }

            } else {
                iq.createError(PacketError.Condition.forbidden)
            }
        } ?: iq.createError(PacketError.Condition.internal_server_error)
    }

    private fun IQ.data(): Map<String, String> = this.childElement.dataMap()

    private fun IQ.createError(condition: PacketError.Condition) = IQ.createResultIQ(this).apply {
        error = PacketError(condition)
    }

    private fun Element.dataMap(): Map<String, String> = this.element(
        QName.get("x", "jabber:x:data")
    )?.let { formElement ->
        DataForm(formElement).fields.associateBy({ it.variable }) { it.firstValue }
    } ?: mapOf()

}