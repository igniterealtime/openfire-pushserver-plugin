package org.igniterealtime.openfire.plugins.pushserver.models

import org.igniterealtime.openfire.plugins.pushserver.PushManager
import org.jivesoftware.util.StringUtils

data class PushRecord(
    val domain      : String
    , val deviceId  : String
    , var token     : String
    , val type      : Type
    , val node      : String = StringUtils.randomString(12)
    , val secret    : String = StringUtils.randomString(24)
) {

    enum class Type {
        ios, android, none;

        companion object {
            fun from(service: PushManager.Service) = when(service) {
                PushManager.Service.FCM -> android
                PushManager.Service.APNS -> ios
            }

            fun from(s: String) = values().firstOrNull { it.name == s } ?: none
        }
    }

    constructor(domain: String, deviceId: String, token: String, service: PushManager.Service) : this(
        domain, deviceId, token, Type.from(service)
    )

    constructor(domain: String, deviceId: String, token: String, type: String, node: String, secret: String) : this(
        domain, deviceId, token, Type.from(type), node, secret
    )
}