package ru.edenor.loginNotify

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import ru.edenor.loginNotify.command.LogNotifyCommand
import ru.edenor.loginNotify.data.ConfigStorage
import ru.edenor.loginNotify.handler.PlayerJoinHandler
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class LoginNotify : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        val storage = ConfigStorage(this)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            LogNotifyCommand.commands(storage).forEach { commands.registrar().register(it) }
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
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault())

    }


}

