package org.igniterealtime.openfire.plugins.pushserver

import org.jivesoftware.openfire.container.Plugin
import org.jivesoftware.openfire.container.PluginManager
import org.slf4j.LoggerFactory
import org.xmpp.component.ComponentException
import org.xmpp.component.ComponentManagerFactory
import java.io.File

class PushServerPlugin: Plugin {

    private val logger = LoggerFactory.getLogger(PushServerPlugin::class.java)

    override fun initializePlugin(p0: PluginManager?, p1: File?) {
        ComponentManagerFactory.getComponentManager().apply {

            try {
                this.addComponent(PushServerComponent.NAME, PushServerComponent())
            } catch (e: ComponentException) {
                logger.error("PushServerComponent couldn't be added.", e)
            }
        }
    }

    override fun destroyPlugin() {
        ComponentManagerFactory.getComponentManager().apply {
            try {
                this.removeComponent(PushServerComponent.NAME)
            } catch (e: ComponentException) {
                logger.error("PushServerComponent couldn't be removed.", e)
            }
        }
    }
}