package org.igniterealtime.openfire.plugins.pushserver.services

interface PushService {
    fun push(notificationData: Map<String, String>?, additionalData: Map<String, String>?, token: String, isSandbox: Boolean): Boolean
}