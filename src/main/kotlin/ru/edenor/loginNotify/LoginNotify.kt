package ru.edenor.loginNotify

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin
import ru.edenor.loginNotify.command.LogNotifyCommand
import ru.edenor.loginNotify.data.ConfigStorage
import ru.edenor.loginNotify.data.Storage
import ru.edenor.loginNotify.handler.PlayerJoinHandler
import ru.edenor.loginNotify.handler.WebhookListener
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LoginNotify : JavaPlugin() {
  lateinit var storage: Storage

  override fun onEnable() {
    storage = ConfigStorage(this)

    lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands
      ->
      LogNotifyCommand(this, storage).commands().forEach {
        commands.registrar().register(it)
      }
    }

    server.pluginManager.registerEvents(PlayerJoinHandler(storage), this)
    server.pluginManager.registerEvents(WebhookListener(this, storage), this)
  }

  fun reload() {
    storage.reload()
  }

  companion object {
    const val NOTIFICATION_PERMISSION = "loginnotify.notify"
    const val EDIT_PERMISSION = "loginnotify.edit"
    const val LIST_PERMISSION = "loginnotify.list"
    const val RELOAD_PERMISSION = "loginnotify.reload"
    val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy")
            .withZone(ZoneId.systemDefault())

    fun Instant.toLoginNotifyFormat(): String = dateTimeFormatter.format(this)
  }
}
