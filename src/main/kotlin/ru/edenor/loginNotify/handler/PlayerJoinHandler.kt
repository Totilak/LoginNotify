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
        val (playerName, comment, createdAt, addedBy) = storage.getPlayer(event.player.name) ?: return
        val formattedDate = LoginNotify.dateTimeFormatter.format(createdAt)

        val message = miniMessage().deserialize(
            "<bold><dark_red>$playerName зашел.</dark_red></bold><br>" +
                    "<red>Добавил в список <gold>$addedBy</gold> $formattedDate <br>" +
                    "<red>C комментарием <gold>'$comment'"
        )

        Bukkit.getOnlinePlayers()
            .filter { it.hasPermission(LoginNotify.notificationPermission) }
            .filter { storage.shouldNotify(it.name) }
            .forEach {
                it.sendMessage(message)
                it.playSound(it, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.UI, 1f, 1f)
            }

    }
}