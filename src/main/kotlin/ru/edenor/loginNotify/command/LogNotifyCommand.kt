package ru.edenor.loginNotify.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import ru.edenor.loginNotify.LoginNotify
import ru.edenor.loginNotify.LoginNotify.Companion.editPermission
import ru.edenor.loginNotify.LoginNotify.Companion.listPermission
import ru.edenor.loginNotify.LoginNotify.Companion.notificationPermission
import ru.edenor.loginNotify.data.AdminSettings
import ru.edenor.loginNotify.data.NotificationRecord
import ru.edenor.loginNotify.data.Storage
import java.time.Instant

object LogNotifyCommand {
    fun commands(storage: Storage): Array<LiteralCommandNode<CommandSourceStack>> {
        val logNotifyCommand = logNotifyCommand(storage)
        val lnnCommand = literal("lnn").requires {
            it.sender.hasPermission(listPermission)
                    || it.sender.hasPermission(editPermission)
                    || it.sender.hasPermission(notificationPermission)
        }.executes {
            sendHelp(it.source.sender)
            Command.SINGLE_SUCCESS
        }
            .redirect(logNotifyCommand).build()
        return arrayOf(logNotifyCommand, lnnCommand)
    }

    fun logNotifyCommand(storage: Storage) = literal("lognotify")
        .requires {
            it.sender.hasPermission(listPermission)
                    || it.sender.hasPermission(editPermission)
                    || it.sender.hasPermission(notificationPermission)
        }.executes {
            sendHelp(it.source.sender)
            return@executes Command.SINGLE_SUCCESS
        }
        .then(
            literal("add")
                .requires {
                    it.sender.hasPermission(editPermission)
                }
                .then(
                    argument("name", ArgumentTypes.playerProfiles())
                        .then(
                            argument("comment", StringArgumentType.greedyString())
                                .executes {
                                    add(it, storage)
                                    return@executes Command.SINGLE_SUCCESS
                                }
                        )
                )
        )
        .then(
            literal("remove")
                .requires { it.sender.hasPermission(editPermission) }
                .then(
                    argument("name", NotificationRecordArgumentType(storage))
                        .executes {
                            remove(it, storage)
                            return@executes Command.SINGLE_SUCCESS
                        }
                )
        )
        .then(
            literal("list")
                .requires { it.sender.hasPermission(listPermission) }
                .executes {
                    sendList(it.source.sender, storage)
                    return@executes Command.SINGLE_SUCCESS
                }
        ).then(
            literal("toggle")
                .requires { it.sender.hasPermission(notificationPermission) }
                .executes {
                    toggleNotifications(it.source.sender, storage)
                    return@executes Command.SINGLE_SUCCESS
                }
        ).build()

    fun toggleNotifications(sender: CommandSender, storage: Storage) {
        val (_, toggled) = storage.getSettings(sender.name)
        storage.setSettings(AdminSettings(sender.name, !toggled))
        val stateText = if (!toggled) "<green>Включены</green>" else "<red>Выключены</red>"
        sender.sendRichMessage("Уведомления о входе: $stateText")
    }

    fun sendHelp(sender: CommandSender) {

        sender.sendRichMessage(
            "<green><bold>LoginNotify</bold><dark_aqua> - Позволяет уведомлять администрацию о заходе игрока на сервер</green>"
        )

        if (sender.hasPermission(editPermission)) {
            sender.sendRichMessage(
                "<green>/lognotify add <username> <comment> <yellow>- Отслеживать игрока<br>" +
                        "<green>/lognotify remove <username> <yellow>- Перестать отслеживать"
            )
        }
        if (sender.hasPermission(listPermission)) {
            sender.sendRichMessage(
                "<green>/lognotify list <yellow>- Показать список отслеживаемых"
            )
        }
        if (sender.hasPermission(notificationPermission)) {
            sender.sendRichMessage(
                "<green>/lognotify toggle <yellow>- Вкл/Выкл. уведомлений"
            )
        }
    }

    fun remove(context: CommandContext<CommandSourceStack>, storage: Storage) {
        val name = NotificationRecordArgumentType.getArgument("name", context)
        val sender = context.source.sender

        storage.removePlayer(name)
        sender.sendRichMessage("<green>Игрок <gold>$name</gold> больше не отслеживается!")
    }

    fun add(context: CommandContext<CommandSourceStack>, storage: Storage) {
        val profilesResolver = context.getArgument("name", PlayerProfileListResolver::class.java)
        val foundProfiles = profilesResolver.resolve(context.getSource())
        val comment = StringArgumentType.getString(context, "comment")
        val time = Instant.now()
        val sender = context.source.sender
        val senderName = sender.name
        for (profile in foundProfiles) {
            val name = profile.name!!
            if (!Regex("\\w+").matches(name)) {
                sender.sendRichMessage("<dark_red>Невозможный никнейм <red>$name</red>!")
                return
            }

            val record = NotificationRecord(name, comment, time, senderName)
            storage.addPlayer(record)
            sender.sendRichMessage("<green>Игрок <gold>$name</gold> теперь отслеживается!")
        }

    }

    fun sendList(sender: CommandSender, storage: Storage) {
        val players = storage.getPlayers().sortedBy { it.createdAt }

        if (players.isEmpty()) {
            sender.sendRichMessage("<red>Список пуст!")
        }

        for ((playerName, comment, createdAt, addedBy) in players) {
            val formattedDate = LoginNotify.dateTimeFormatter.format(createdAt)

            val name = if (isPlayerOnline(playerName)) "<green>$playerName</green>" else "<red>$playerName</red>"

            sender.sendRichMessage(
                "$name: добавил <gold>$addedBy</gold> в $formattedDate <br>Описание: $comment"
            )
        }
    }


    fun isPlayerOnline(playerName: String): Boolean {
        val player = Bukkit.getPlayer(playerName)
        return player != null
    }
}

