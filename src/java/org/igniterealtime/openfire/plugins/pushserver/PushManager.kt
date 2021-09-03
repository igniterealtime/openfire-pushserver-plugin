package org.igniterealtime.openfire.plugins.pushserver

import org.igniterealtime.openfire.plugins.pushserver.models.PushRecord
import org.igniterealtime.openfire.plugins.pushserver.services.APNSPushService
import org.igniterealtime.openfire.plugins.pushserver.services.FCMPushService
import java.util.*

class PushManager {

    enum class Service {
        FCM, APNS;

        companion object {
            fun of(node: String, command: PushServerIQHandler.Command): Result<Service> {
                if (command.prefix.length >= node.length) {
                    return Result.failure(Exception("Command node too short."))
                }

                val service = node.drop(command.prefix.length).uppercase(Locale.US)
                return values().firstOrNull { it.name == service }?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("$service is not a known push service"))
            }

            fun of(type: PushRecord.Type) = when(type) {
                PushRecord.Type.ios -> APNS
                PushRecord.Type.android -> FCM
                PushRecord.Type.none -> null
            }
        }

    }

    private val fcmPushService: FCMPushService? = FCMPushService.invoke()
    private val apnsPushService: APNSPushService? = APNSPushService.invoke()

    fun sendPush(service: Service?, messageId: String, token: String, isSandbox: Boolean): Boolean? {
        return when(service) {
            Service.FCM     -> fcmPushService?.push(messageId, token, isSandbox)
            Service.APNS    -> apnsPushService?.push(messageId, token, isSandbox)
            else -> null
        }
    }

}