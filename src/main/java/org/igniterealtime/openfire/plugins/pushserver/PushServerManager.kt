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

    fun getProperty(property: String) = JiveGlobals.getProperty(property, "")

    fun setAndroidSettings(projectId: String) {
        JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_FCM_PROJECT_ID.key, projectId)
    }

    fun setIosSettings(bundleId: String, key: String, teamId: String) {
        JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_APNS_BUNDLE_ID.key, bundleId)
        JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_APNS_KEY.key, key)
        JiveGlobals.setProperty(PushServerProperty.Property.PROPERTY_NAME_APNS_TEAM_ID.key, teamId)
    }

    fun writeCredentialFileContent(content: String, type: PushRecord.Type) {
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