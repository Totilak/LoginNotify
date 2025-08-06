package ru.edenor.loginNotify.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import org.bukkit.command.CommandSender
import ru.edenor.loginNotify.LoginNotify.Companion.editPermission
import ru.edenor.loginNotify.LoginNotify.Companion.listPermission
import ru.edenor.loginNotify.data.NotificationRecord
import ru.edenor.loginNotify.data.Storage
import java.time.Instant

/**
/lognotify (list | edit)
/lognotify add <username> (edit)
/lognotify remove <username> (edit)
/lognotify list (list)
 */
object LogNotifyCommand {
  fun command(storage: Storage): LiteralCommandNode<CommandSourceStack> {
    return literal("lognotify")
      .requires {
        it.sender.hasPermission(listPermission) || it.sender.hasPermission(editPermission)
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
            argument("name", StringArgumentType.word())
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
            argument("name", StringArgumentType.word())
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
      ).build()
  }

  fun sendHelp(sender: CommandSender) {
    if (sender.hasPermission(editPermission)) {
      sender.sendRichMessage(
        "<green>Команды <bold>LoginNotify</bold>:</green> " +
            "<green>/lognotify add <username> <comment> - Добавить игрока в список</green> " +
            "<green>/lognotify remove <username> - Удалить игрока из списка</green> " +
            "<green>/lognotify list - Показать список отслеживаемых</green>"
      )
    } else if (sender.hasPermission(listPermission)) {
      sender.sendMessage(
        "<green>Команды <bold>LoginNotify</bold>:</green> " +
            "<green>/lognotify list - Показать список отслеживаемых</green>"
      )
    }
  }

  fun remove(context: CommandContext<CommandSourceStack>, storage: Storage) {
    val name = StringArgumentType.getString(context, "name")
    val sender = context.source.sender

    storage.removePlayer(name)
    sender.sendRichMessage("<green>Игрок <gold>$name</gold> больше не отслеживается!")
  }

  fun add(context: CommandContext<CommandSourceStack>, storage: Storage) {
    val name = StringArgumentType.getString(context, "name")
    val comment = StringArgumentType.getString(context, "comment")
    val time = Instant.now()
    val sender = context.source.sender
    val senderName = sender.name

    if (!Regex("\\w+").matches(name)) {
      sender.sendRichMessage("<dark_red>Невозможный никнейм <red>$name</red>!")
      return
    }

    val record = NotificationRecord(name, comment, time, senderName)
    storage.addPlayer(record)
    sender.sendRichMessage("<green>Игрок <gold>$name</gold> теперь отслеживается!")
  }

  fun sendList(sender: CommandSender, storage: Storage) {
    val players = storage.getPlayers().sortedBy { it.createdAt }

    if (players.isEmpty()) {
      sender.sendRichMessage("<red>Список пуст!")
    }

    for ((playerName, comment, createdAt, addedBy) in players) {
      sender.sendRichMessage("$playerName: добавлен $addedBy $createdAt: $comment")
    }
  }
}

