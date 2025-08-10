package ru.edenor.loginNotify

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import ru.edenor.loginNotify.command.LogNotifyCommand
import ru.edenor.loginNotify.data.ConfigStorage
import ru.edenor.loginNotify.handler.PlayerJoinHandler
import ru.edenor.loginNotify.handler.WebhookListener
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class LoginNotify : JavaPlugin() {

  override fun onEnable() {
    val storage = ConfigStorage(this)

    lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
      LogNotifyCommand(storage).commands().forEach { commands.registrar().register(it) }
    }

    server.pluginManager.registerEvents(PlayerJoinHandler(storage), this)
    server.pluginManager.registerEvents(WebhookListener(this, storage), this)
  }

  companion object {
    const val NOTIFICATION_PERMISSION = "loginnotify.notify"
    const val EDIT_PERMISSION = "loginnotify.edit"
    const val LIST_PERMISSION = "loginnotify.list"
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter
      .ofPattern("dd.MM.yyyy")
      .withZone(ZoneId.systemDefault())

    fun Instant.toLoginNotifyFormat(): String = dateTimeFormatter.format(this)
  }
}

