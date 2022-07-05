package org.igniterealtime.openfire.plugins.pushserver

import org.igniterealtime.openfire.plugins.pushserver.models.PushRecord
import org.jivesoftware.util.JiveGlobals
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object PushServerManager {

    private val logger = LoggerFactory.getLogger(PushServerManager::class.java)

    enum class Message(val isSuccess: Boolean, val key: String, val value: String) {
        ApnsBundleIdMissing(false, "ios.apns.bundle-id.missing", "pushserver.settings.apns.bundle_id.missing.error")
        , ApnsBundleIdInvalid(false, "ios.apns.bundle-id.invalid", "pushserver.settings.apns.bundle_id.invalid.error")
        , ApnsBundleIdSaved( true, "ios.apns.bundle-id.saved", "pushserver.settings.apns.bundle_id.saved_successfully")
        , ApnsKeyMissing( false, "ios.apns.key.missing", "pushserver.settings.apns.key.missing.error")
        , ApnsKeyInvalid( false, "ios.apns.key.invalid", "pushserver.settings.apns.key.invalid.error")
        , ApnsKeySaved( true, "ios.apns.key.saved", "pushserver.settings.apns.key.saved_successfully")
        , ApnsTeamIdMissing( false, "ios.apns.team-id.missing", "pushserver.settings.apns.team_id.missing.error")
        , ApnsTeamIdInvalid( false, "ios.apns.team-id.invalid", "pushserver.settings.apns.team_id.invalid.error")
        , ApnsTeamIdSaved( true, "ios.apns.team-id.saved", "pushserver.settings.apns.team_id.saved_successfully")
        , FcmProjectIdMissing( false, "android.fcm.project-id.missing", "pushserver.settings.fcm.project_id.missing.error")
        , FcmProjectIdInvalid( false, "android.fcm.project-id.invalid", "pushserver.settings.fcm.project_id.invalid.error")
        , FcmProjectIdSaved( true, "android.fcm.project-id.saved", "pushserver.settings.fcm.project_id.saved_successfully")
        , IosCredentialMissing( false, "credential.ios.missing", "pushserver.settings.apns.credential.missing.error")
        , IosCredentialSaved( true, "credential.ios.saved", "pushserver.settings.apns.credential.saved_successfully")
        , AndroidCredentialMissing( false, "credential.android.missing", "pushserver.settings.fcm.credential.missing.error")
        , AndroidCredentialSaved( true, "credential.android.saved", "pushserver.settings.fcm.credential.saved_successfully")
    }

    @JvmStatic fun getFilePath(type: PushRecord.Type) =
        when(type) {
            PushRecord.Type.ios -> PushServerProperty.APNS_PKCS8_FILE_PATH
            PushRecord.Type.android -> PushServerProperty.FCM_CREDENTIAL_FILE_PATH
            PushRecord.Type.none -> null
        }

    @JvmStatic var FCM_CREDENTIAL_FILE_PATH = PushServerProperty.FCM_CREDENTIAL_FILE_PATH

    @JvmStatic fun setAndroidSettings(projectId: String?): Message {
        projectId ?: return Message.FcmProjectIdMissing

        if (!isValidFcmProjectId(projectId)) {
            return Message.FcmProjectIdInvalid
        }

        JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_FCM_PROJECT_ID.key, projectId)
        return Message.FcmProjectIdSaved
    }

    @JvmStatic fun setIosSettings(bundleId: String?, key: String?, teamId: String?): List<Message> {
        val messageList = mutableListOf<Message>()

        when {
            bundleId == null -> messageList.add(Message.ApnsBundleIdMissing)
            !isValidApnsBundleId(bundleId) -> messageList.add(Message.ApnsBundleIdInvalid)
            else -> {
                JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_APNS_BUNDLE_ID.key, bundleId)
                messageList.add(Message.ApnsBundleIdSaved)
            }
        }

        when {
            key == null -> messageList.add(Message.ApnsKeyMissing)
            !isValidApnsKey(key) -> messageList.add(Message.ApnsKeyInvalid)
            else -> {
                JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_APNS_KEY.key, key)
                messageList.add(Message.ApnsKeySaved)
            }
        }

        when {
            teamId == null -> messageList.add(Message.ApnsTeamIdMissing)
            !isValidApnsTeamId(teamId) -> messageList.add(Message.ApnsTeamIdInvalid)
            else -> {
                JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_APNS_TEAM_ID.key, teamId)
                messageList.add(Message.ApnsTeamIdSaved)
            }
        }

        return messageList
    }

    @JvmStatic fun isValidFcmProjectId(id: String): Boolean {
        return id.all { it.isLetterOrDigit() || it == '-' }
    }

    @JvmStatic fun isValidApnsBundleId(id: String): Boolean {
        return id.all { it.isLetterOrDigit() || it == '-' || it == '.' }
    }

    @JvmStatic fun isValidApnsKey(key: String): Boolean {
        return key.all { it.isLetterOrDigit() } && key.length == 10
    }

    @JvmStatic fun isValidApnsTeamId(id: String): Boolean {
        return id.all { it.isLetterOrDigit() } && id.length == 10
    }

    @JvmStatic fun writeCredentialFileContent(content: String, type: PushRecord.Type) {
        val file = when(type) {
            PushRecord.Type.ios -> File(PushServerProperty.APNS_PKCS8_FILE_PATH)
            PushRecord.Type.android -> File(PushServerProperty.FCM_CREDENTIAL_FILE_PATH)
            PushRecord.Type.none -> {
                throw Exception("Unexpected content type while writing file")
            }
        }

        var writer: Writer? = null
        try {
            Files.createDirectories(Paths.get(file.parent))

            if (file.exists()) {
                file.delete()
            }

            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8))
            writer.write(content)
        } catch (e: Exception) {
            logger.error("Credential file content couldn't be written. ($type)", e)
        } finally {
            try {
                writer?.close()
            } catch (e: Exception) {
                logger.error("Writer couldn't be closed.", e)
            }
        }
    }

}