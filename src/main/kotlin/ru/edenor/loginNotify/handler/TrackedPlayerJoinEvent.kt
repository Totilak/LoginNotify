package ru.edenor.loginNotify.handler

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import ru.edenor.loginNotify.data.NotificationRecord

class TrackedPlayerJoinEvent(player: Player, val notificationRecord: NotificationRecord) : PlayerEvent(player),
  Cancellable {
  var cancel: Boolean = false
  override fun getHandlers(): HandlerList = HANDLER_LIST
  override fun isCancelled(): Boolean = cancel
  override fun setCancelled(cancel: Boolean) {
    this.cancel = cancel
  }

  companion object {
    @JvmStatic
    private val HANDLER_LIST: HandlerList = HandlerList()

    @JvmStatic
    @Suppress("unused")
    fun getHandlerList() = HANDLER_LIST
  }
}