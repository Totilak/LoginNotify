package ru.edenor.loginNotify.handler

import net.kyori.adventure.text.minimessage.MiniMessage.miniMessage
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import ru.edenor.loginNotify.LoginNotify
import ru.edenor.loginNotify.LoginNotify.Companion.toLoginNotifyFormat
import ru.edenor.loginNotify.data.Storage

class PlayerJoinHandler(private val storage: Storage) : Listener {

  @EventHandler
  fun onPlayerJoin(event: PlayerJoinEvent) {
    val notificationRecord = storage.getPlayer(event.player.name) ?: return
    TrackedPlayerJoinEvent(event.player, notificationRecord).callEvent()
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
  fun onTrackedPlayerJoin(event: TrackedPlayerJoinEvent) {
    val (playerName, comment, createdAt, addedBy) = event.notificationRecord
    val formattedDate = createdAt.toLoginNotifyFormat()
    val message =
        miniMessage()
            .deserialize(
                "<bold><dark_red>$playerName зашел.</dark_red></bold><br>" +
                    "<red>Добавил в список <gold>$addedBy</gold> $formattedDate <br>" +
                    "<red>C комментарием <gold>'$comment'")

    Bukkit.getOnlinePlayers()
        .filter { it.hasPermission(LoginNotify.NOTIFICATION_PERMISSION) }
        .filter { storage.shouldNotify(it.name) }
        .forEach {
          it.sendMessage(message)
          it.playSound(
              it, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.UI, 1f, 1f)
        }
  }

  @EventHandler
  fun toggleMatrixNotificationsOnJoin(event: PlayerJoinEvent) {
    if (!event.player.hasPermission(LoginNotify.MATRIX_TOGGLE_PERMISSION)) {
      return
    }
    val (_, _, toggleMatrix) = storage.getSettings(event.player.name)
    if (toggleMatrix) {
      event.player.performCommand("matrix togglenotify")
    }
  }
}
