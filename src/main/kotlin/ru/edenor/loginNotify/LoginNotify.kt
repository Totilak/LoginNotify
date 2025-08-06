package ru.edenor.loginNotify

import io.papermc.paper.command.brigadier.Commands.literal
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import ru.edenor.loginNotify.command.LogNotifyCommand
import ru.edenor.loginNotify.data.ConfigStorage
import ru.edenor.loginNotify.handler.PlayerJoinHandler


class LoginNotify : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        val storage = ConfigStorage(this)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) {
            val logCommand = LogNotifyCommand.command(storage)
            it.registrar().register(logCommand)
            it.registrar().register(literal("lnn").redirect(logCommand).build())
        }

        server.pluginManager.registerEvents(PlayerJoinHandler(storage), this)

    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    companion object {
        const val notificationPermission = "loginnotify.notify"
        const val editPermission = "loginnotify.edit"
        const val listPermission = "loginnotify.list"

    }
}

