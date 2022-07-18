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

    enum class Message(val isSuccess: Boolean, val value: String) {
        CSRFError(false, "push.server.settings.csrf.error")
        , IOSBundleIdMissing(false, "push.server.settings.ios.bundleId.missing.error")
        , IOSBundleIdInvalid(false, "push.server.settings.ios.bundleId.invalid.error")
        , IOSBundleIdSaved( true, "push.server.settings.ios.bundleId.save.success")
        , IOSTeamIdMissing( false, "push.server.settings.ios.teamId.missing.error")
        , IOSTeamIdInvalid( false, "push.server.settings.ios.teamId.invalid.error")
        , IOSTeamIdSaved( true, "push.server.settings.ios.teamId.save.success")
        , IOSApnsKeyIdMissing( false,  "push.server.settings.ios.apns.keyId.missing.error")
        , IOSApnsKeyIdInvalid( false, "push.server.settings.ios.apns.keyId.invalid.error")
        , IOSApnsKeyIdSaved( true, "push.server.settings.ios.apns.keyId.save.success")
        , IOSEncryptionKeyMissing( false,  "push.server.settings.ios.apns.encryptionKey.missing.error")
        , IOSEncryptionKeySaveError( false, "push.server.settings.ios.apns.encryptionKey.save.error")
        , IOSEncryptionKeySaved( true, "push.server.settings.ios.apns.encryptionKey.save.success")
        , AndroidProjectIdMissing( false, "push.server.settings.android.projectId.missing.error")
        , AndroidProjectIdInvalid( false, "push.server.settings.android.projectId.invalid.error")
        , AndroidProjectIdSaved( true, "push.server.settings.android.projectId.save.success")
        , AndroidServiceAccountKeyMissing( false, "push.server.settings.android.serviceAccountKey.missing.error")
        , AndroidServiceAccountKeySaveError( false, "push.server.settings.android.serviceAccountKey.save.error")
        , AndroidServiceAccountKeySaved( true, "push.server.settings.android.serviceAccountKey.save.success")
    }

    @JvmStatic var FCM_CREDENTIAL_FILE_PATH = PushServerProperty.FCM_CREDENTIAL_FILE_PATH

    @JvmStatic fun setIosSettings(bundleId: String?, teamId: String?, keyId: String?, encryptionKeyContent: String?): List<Message> {
        val messageList = mutableListOf<Message>()

        when {
            bundleId == null -> messageList.add(Message.IOSBundleIdMissing)
            !isValidApnsBundleId(bundleId) -> messageList.add(Message.IOSBundleIdInvalid)
            else -> {
                JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_IOS_BUNDLE_ID.key, bundleId)
                messageList.add(Message.IOSBundleIdSaved)
            }
        }

        when {
            teamId == null -> messageList.add(Message.IOSTeamIdMissing)
            !isValidApnsTeamId(teamId) -> messageList.add(Message.IOSTeamIdInvalid)
            else -> {
                JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_IOS_TEAM_ID.key, teamId)
                messageList.add(Message.IOSTeamIdSaved)
            }
        }

        when {
            keyId == null -> messageList.add(Message.IOSApnsKeyIdMissing)
            !isValidApnsKey(keyId) -> messageList.add(Message.IOSApnsKeyIdInvalid)
            else -> {
                JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_IOS_APNS_KEY_ID.key, keyId)
                messageList.add(Message.IOSApnsKeyIdSaved)
            }
        }

        when (encryptionKeyContent) {
            null -> {
                messageList.add(Message.IOSEncryptionKeyMissing)
            }
            else -> {
                runCatching {
                    writeCredentialFileContent(
                        path = PushServerProperty.APNS_PKCS8_FILE_PATH
                        , content = encryptionKeyContent
                    )
                }.onSuccess {
                    messageList.add(Message.IOSEncryptionKeySaved)
                }.onFailure {
                    messageList.add(Message.IOSEncryptionKeySaveError)
                }
            }
        }

        return messageList
    }

    @JvmStatic fun setAndroidSettings(projectId: String?, serviceAccountKeyContent: String?): List<Message> {
        val messageList = mutableListOf<Message>()

        when {
            projectId == null -> messageList.add(Message.AndroidProjectIdMissing)
            !isValidFcmProjectId(projectId) -> messageList.add(Message.AndroidProjectIdInvalid)
            else -> {
                JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_ANDROID_PROJECT_ID.key, projectId)
                messageList.add(Message.AndroidProjectIdSaved)
            }
        }

        when (serviceAccountKeyContent) {
            null -> {
                messageList.add(Message.AndroidServiceAccountKeyMissing)
            }
            else -> {
                runCatching {
                    writeCredentialFileContent(
                        path = PushServerProperty.FCM_CREDENTIAL_FILE_PATH
                        , content = serviceAccountKeyContent
                    )
                }.onSuccess {
                    messageList.add(Message.AndroidServiceAccountKeySaved)
                }.onFailure {
                    messageList.add(Message.AndroidServiceAccountKeySaveError)
                }
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

    @JvmStatic private fun writeCredentialFileContent(path: String, content: String) {
        val file = File(path)
        var writer: Writer? = null
        runCatching {
            Files.createDirectories(Paths.get(file.parent))

            if (file.exists()) {
                file.delete()
            }

            writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8))
            writer?.write(content)
        }

        writer?.close()
    }

}