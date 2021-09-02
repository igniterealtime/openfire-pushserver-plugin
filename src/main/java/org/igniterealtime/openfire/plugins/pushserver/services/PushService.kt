package org.igniterealtime.openfire.plugins.pushserver.services

interface PushService {
    fun push(messageId: String?, token: String, isSandbox: Boolean): Boolean
}