package org.igniterealtime.openfire.plugins.pushserver.services

import org.igniterealtime.openfire.plugins.pushserver.PushServerProperty
import org.slf4j.LoggerFactory
import us.raudi.pushraven.Message
import us.raudi.pushraven.Pushraven
import us.raudi.pushraven.configs.AndroidConfig
import java.io.File
import java.io.IOException

class FCMPushService private constructor(): PushService {

    companion object {
        private val logger = LoggerFactory.getLogger(FCMPushService::class.java)

        operator fun invoke(): FCMPushService? {
            return try {
                val fcmCredentialFile = File(PushServerProperty.FCM_CREDENTIAL_FILE_PATH)
                Pushraven.setCredential(fcmCredentialFile)

                PushServerProperty.Property.PROPERTY_NAME_FCM_PROJECT_ID.value?.let { id ->
                    Pushraven.setProjectId(id)
                    FCMPushService()
                } ?: kotlin.run {
                    logger.error("${PushServerProperty.Property.PROPERTY_NAME_FCM_PROJECT_ID.key} should be set.")
                    null
                }
            } catch (e: IOException) {
                logger.error("FCMPushService couldn't be initialized", e)
                null
            }
        }
    }

    override fun push(messageId: String?, token: String, isSandbox: Boolean): Boolean {
        return Pushraven.push(
            Message()
                .data(mapOf("messageId" to messageId))
                .token(token)
                .android(AndroidConfig().priority(AndroidConfig.Priority.HIGH))
        )?.let { fcmResponse ->
            if (fcmResponse.responseCode == 200) {
                true
            } else {
                logger.error("FCM response is not successful for messageId: $messageId, (${fcmResponse.responseCode}) ${fcmResponse.message}")
                false
            }
        } ?: kotlin.run {
            logger.error("FCM response is null for messageId: $messageId")
            false
        }
    }
}