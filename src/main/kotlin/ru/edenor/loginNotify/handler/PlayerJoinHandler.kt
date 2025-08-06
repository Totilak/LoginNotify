package ru.edenor.loginNotify.handler

import net.kyori.adventure.text.minimessage.MiniMessage.miniMessage
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import ru.edenor.loginNotify.LoginNotify
import ru.edenor.loginNotify.data.Storage


class PlayerJoinHandler(private val storage: Storage) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val record = storage.getPlayer(event.player.name) ?: return

        val message = miniMessage().deserialize(
            "<bold>Игрок <dark_red>${record.playerName}</dark_red> зашел.</bold> <br>" +
                    "Добавил в список <aqua>${record.addedBy}</aqua> ${record.createdAt} <br>" +
                    "C комментарием '${record.comment}'"
        )

        Bukkit.getOnlinePlayers()
            .filter { it.hasPermission(LoginNotify.notificationPermission) }
            .forEach {
                it.sendMessage(message)
                it.playSound(it, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.UI, 1f, 1f)
            }

    }
}