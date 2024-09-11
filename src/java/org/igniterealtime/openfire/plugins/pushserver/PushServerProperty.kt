package org.igniterealtime.openfire.plugins.pushserver

import org.jivesoftware.util.JiveGlobals
import org.jivesoftware.util.PropertyEventListener
import org.slf4j.LoggerFactory
import java.io.File

class PushServerProperty: PropertyEventListener {

    enum class Property(val key: String) {
        PROPERTY_NAME_IOS_BUNDLE_ID("pushserver.apple.apns.bundleId")
        , PROPERTY_NAME_IOS_TEAM_ID("pushserver.apple.apns.teamId")
        , PROPERTY_NAME_IOS_APNS_KEY_ID("pushserver.apple.apns.key")
        , PROPERTY_NAME_ANDROID_PROJECT_ID("pushserver.google.fcm.projectId");

        val value
        get() = properties[this]
    }

    private val logger = LoggerFactory.getLogger(PushServerProperty::class.java)

    companion object {
        val serviceName = JiveGlobals.getProperty("pushserver.name", "push")

        val FCM_CREDENTIAL_FILE_PATH = "${JiveGlobals.getHomePath().resolve("conf").resolve("pushserver-fcm.json")}"
        val APNS_PKCS8_FILE_PATH = "${JiveGlobals.getHomePath().resolve("conf").resolve("pushserver-apns.p8")}"

        private val properties = Property.values().associateBy({it}) { JiveGlobals.getProperty(it.key, null) }.toMutableMap()
    }

    override fun propertySet(property: String, params: MutableMap<String, Any>) {
        Property.values().firstOrNull { it.name == property }?.let { p ->
            val value = params["value"] as? String
            properties[p] = value
            logger.debug("Property '$p' has been set a new value: '$value'")
        } ?: logger.debug("Property '$property' couldn't be found so it hasn't been set.")
    }

    override fun propertyDeleted(property: String, params: MutableMap<String, Any>) {
        Property.values().firstOrNull { it.name == property }?.let { p ->
            properties[p] = null
            logger.debug("Property '$p' has been deleted")
        } ?: logger.debug("Property '$property' couldn't be found so it hasn't been deleted.")
    }

    override fun xmlPropertySet(property: String, params: MutableMap<String, Any>) {}

    override fun xmlPropertyDeleted(property: String, params: MutableMap<String, Any>) {}
}