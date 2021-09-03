package org.igniterealtime.openfire.plugins.pushserver.services

import com.eatthepath.pushy.apns.ApnsClient
import com.eatthepath.pushy.apns.ApnsClientBuilder
import com.eatthepath.pushy.apns.auth.ApnsSigningKey
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification
import com.eatthepath.pushy.apns.util.TokenUtil
import org.igniterealtime.openfire.plugins.pushserver.PushServerProperty
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ExecutionException

class APNSPushService private constructor(
    private val apnsClient: ApnsClient
    , private val sandboxApnsClient: ApnsClient
    , private val bundleId: String
): PushService {

    companion object {
        private val logger = LoggerFactory.getLogger(APNSPushService::class.java)

        operator fun invoke(): APNSPushService? {
            val apnsCredentialFile = File(PushServerProperty.APNS_PKCS8_FILE_PATH)

            val teamId = PushServerProperty.Property.PROPERTY_NAME_APNS_TEAM_ID.value ?: kotlin.run {
                logger.error("${PushServerProperty.Property.PROPERTY_NAME_APNS_TEAM_ID.key} should be set.")
                return null
            }

            val key = PushServerProperty.Property.PROPERTY_NAME_APNS_KEY.value ?: kotlin.run {
                logger.error("${PushServerProperty.Property.PROPERTY_NAME_APNS_KEY.key} should be set.")
                return null
            }

            val bundleId = PushServerProperty.Property.PROPERTY_NAME_APNS_BUNDLE_ID.value ?: kotlin.run {
                logger.error("${PushServerProperty.Property.PROPERTY_NAME_APNS_BUNDLE_ID.key} should be set.")
                return null
            }

            return try {
                val apnsClientBuilder = ApnsClientBuilder()
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(apnsCredentialFile, teamId, key))

                val apnsClient = apnsClientBuilder.setApnsServer(ApnsClientBuilder.PRODUCTION_APNS_HOST).build()
                val sandboxApnsClient = apnsClientBuilder.setApnsServer(ApnsClientBuilder.DEVELOPMENT_APNS_HOST).build()

                APNSPushService(apnsClient, sandboxApnsClient, bundleId)
            } catch (e: Exception) {
                logger.error("APNSPushService couldn't be initialized", e)
                null
            }
        }
    }

    override fun push(messageId: String?, token: String, isSandbox: Boolean): Boolean {
        val payloadBuilder = SimpleApnsPayloadBuilder()
            .setAlertTitle("message")
            .setCategoryName("message")
            .setSound("default")
            .setMutableContent(true)

        messageId?.let { payloadBuilder.addCustomProperty("messageId", messageId) }

        val payload = payloadBuilder.build()

        val tokenSanitized = TokenUtil.sanitizeTokenString(token)
        val pushNotification = SimpleApnsPushNotification(
            tokenSanitized, bundleId, payload
        )

        val prefix: String = if (isSandbox) {
            "(Sandbox) "
        } else {
            "(Production) "
        }

        return try {
            if (isSandbox) {
                sandboxApnsClient
            } else {
                apnsClient
            }.sendNotification(pushNotification)
                .get()
                ?.let { response ->

                    if (response.isAccepted) {
                        true
                    } else {
                        logger.error("${prefix}Notification has been rejected by the APNs gateway: ${response.rejectionReason}, token: $token")

                        response.tokenInvalidationTimestamp.ifPresent {
                            logger.error("\t…and the token is invalid as of $it, token: $token")
                        }

                        false
                    }
                } ?: kotlin.run {
                    logger.error("${prefix}Notification shouldn't be sent to device: $token")
                    false
                }
        } catch (e: InterruptedException) {
            logger.error("${prefix}Sending push notification has been interrupted", e)
            false
        } catch (e: ExecutionException) {
            logger.error("${prefix}Sending push notification has been failed", e)
            false
        }
    }
}