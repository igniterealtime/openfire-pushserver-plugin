package org.igniterealtime.openfire.plugins.pushserver

import org.jivesoftware.util.PropertyEventDispatcher
import org.slf4j.LoggerFactory

import org.xmpp.packet.IQ
import org.xmpp.packet.JID
import org.xmpp.packet.Packet
import org.xmpp.component.Component
import org.xmpp.component.ComponentManager
import org.xmpp.component.ComponentException

class PushServerComponent: Component {

    private val logger = LoggerFactory.getLogger(PushServerComponent::class.java)

    private var componentManager: ComponentManager? = null
    private var pushServerProperty: PushServerProperty = PushServerProperty()
    private var pushServerIQHandler: PushServerIQHandler = PushServerIQHandler(PushManager())

    companion object {
        val NAME = PushServerProperty.serviceName
        private const val DESC = "Send push notifications to mobile devices through FCM or APNS"
    }

    override fun getName(): String = NAME

    override fun getDescription() = DESC

    override fun processPacket(packet: Packet?) {
        val iq = packet as? IQ ?: return

        when (iq.type) {
            IQ.Type.error, IQ.Type.result, null -> return
            else -> {}
        }

        val resultIQ = pushServerIQHandler.handleIQ(iq)
        try {
            componentManager?.sendPacket(this, resultIQ)
        } catch (e: ComponentException) {
            logger.error("Packet couldn't be sent: $resultIQ", e)
        }
    }

    override fun initialize(jid: JID?, componentManager: ComponentManager?) {
        this.componentManager = componentManager
    }

    override fun start() {
        PropertyEventDispatcher.addListener(pushServerProperty)
    }

    override fun shutdown() {
        PropertyEventDispatcher.removeListener(pushServerProperty)
    }
}