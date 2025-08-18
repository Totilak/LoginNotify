package ru.edenor.loginNotify.command

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
import ru.edenor.loginNotify.LoginNotify.Companion.EDIT_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.LIST_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.MATRIX_TOGGLE_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.NOTIFICATION_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.RELOAD_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.toLoginNotifyFormat
import ru.edenor.loginNotify.command.CommandExtensions.requiresAnyPermission
import ru.edenor.loginNotify.command.CommandExtensions.requiresPermission
import ru.edenor.loginNotify.command.CommandExtensions.simplyRun
import ru.edenor.loginNotify.data.AdminSettings
import ru.edenor.loginNotify.data.NotificationRecord
import ru.edenor.loginNotify.data.Storage
import java.time.Instant

class LogNotifyCommand(
    private val plugin: LoginNotify,
    private val storage: Storage
) {
  fun commands(): Array<LiteralCommandNode<CommandSourceStack>> {
    return arrayOf(logNotify, lnn)
  }

  private val addSection =
      literal("add")
          .requiresPermission(EDIT_PERMISSION)
          .then(
              argument("name", ArgumentTypes.playerProfiles())
                  .then(
                      argument("comment", StringArgumentType.greedyString())
                          .simplyRun(::add)))

  private val removeSection =
      literal("remove")
          .requiresPermission(EDIT_PERMISSION)
          .then(
              argument("name", NotificationRecordArgumentType(storage))
                  .simplyRun(::remove))

  private val listSection =
      literal("list").requiresPermission(LIST_PERMISSION).simplyRun(::sendList)

  private val toggleSection =
      literal("toggle")
          .requiresPermission(NOTIFICATION_PERMISSION)
          .simplyRun(::toggleNotifications)

  private val reloadSection =
      literal("reload")
          .requiresPermission(RELOAD_PERMISSION)
          .simplyRun(::reload)

  private val logNotify =
      literal("lognotify")
          .requiresAnyPermission()
          .simplyRun(::sendHelp)
          .then(addSection)
          .then(removeSection)
          .then(listSection)
          .then(toggleSection)
          .then(reloadSection)
          .build()

  private val lnn =
      literal("lnn")
        .requiresAnyPermission()
        .simplyRun(::sendHelp)
        .then(addSection)
        .then(removeSection)
        .then(listSection)
        .then(toggleSection)
        .then(reloadSection)
        .build()

  private fun toggleNotifications(sender: CommandSender) {
    val newToggled = !storage.getSettings(sender.name).toggled
    storage.setSettings(
        AdminSettings(
            sender.name,
            newToggled,
            storage.getSettings(sender.name).toggleMatrix))
    val toggledText =
        if (newToggled) "<green>Включены</green>" else "<red>Выключены</red>"
    sender.sendRichMessage("Уведомления о входе: $toggledText")
  }


  private fun reload(sender: CommandSender) {
    plugin.reload()
    sender.sendRichMessage("<green>Настройки успешно перезагружены")
  }

  private fun sendHelp(sender: CommandSender) {
    sender.sendRichMessage(
        "<green><bold>LoginNotify</bold> <dark_aqua>- Позволяет уведомлять администрацию о заходе игрока на сервер")

    if (sender.hasPermission(EDIT_PERMISSION)) {
      sender.sendRichMessage(
          "<green>/lognotify add <username> <comment> <yellow>- Отслеживать игрока<br>" +
              "<green>/lognotify remove <username> <yellow>- Перестать отслеживать")
    }
    if (sender.hasPermission(LIST_PERMISSION)) {
      sender.sendRichMessage(
          "<green>/lognotify list <yellow>- Показать список отслеживаемых")
    }
    if (sender.hasPermission(NOTIFICATION_PERMISSION)) {
      sender.sendRichMessage(
          "<green>/lognotify toggle <yellow>- Вкл/Выкл. уведомлений")
    }
    if (sender.hasPermission(RELOAD_PERMISSION)) {
      sender.sendRichMessage(
          "<green>/lognotify reload <yellow>- Перезагрузить настройки")
    }
    if (sender.hasPermission(MATRIX_TOGGLE_PERMISSION)) {
      sender.sendRichMessage(
        "<green>/matrixshutup <yellow>- " +
            "Отключает уведомления <gray>[<dark_aqua>Matrix</dark_aqua>]</gray> <u>при входе</u>")
    }
  }

  private fun remove(context: CommandContext<CommandSourceStack>) {
    val name = NotificationRecordArgumentType.getArgument("name", context)

    storage.removePlayer(name)
    context.source.sender.sendRichMessage(
        "<green>Игрок <gold>$name</gold> больше не отслеживается!")
  }

  private fun add(context: CommandContext<CommandSourceStack>) {
    val foundProfiles =
        context
            .getArgument("name", PlayerProfileListResolver::class.java)
            .resolve(context.source)
    val comment = StringArgumentType.getString(context, "comment")
    val sender = context.source.sender

    foundProfiles
        .map { it.name!! }
        .forEach { name ->
          if (!Regex("\\w+").matches(name)) {
            sender.sendRichMessage(
                "<dark_red>Невозможный никнейм <red>$name</red>!")
            return
          }

          NotificationRecord(name, comment, Instant.now(), sender.name).let {
            storage.addPlayer(it)
          }
          sender.sendRichMessage(
              "<green>Игрок <gold>$name</gold> теперь отслеживается!")
        }
  }

  private fun sendList(sender: CommandSender) {
    val players = storage.getPlayers().sortedBy { it.createdAt }

    if (players.isEmpty()) {
      sender.sendRichMessage("<red>Список пуст!")
    }

    for ((playerName, comment, createdAt, addedBy) in players) {
      val formattedDate = createdAt.toLoginNotifyFormat()

      val name =
          if (isPlayerOnline(playerName)) "<green>$playerName</green>"
          else "<red>$playerName</red>"

      sender.sendRichMessage(
          "$name: добавил <gold>$addedBy</gold> в $formattedDate <br>Описание: $comment")
    }
  }

  private fun isPlayerOnline(playerName: String): Boolean =
      Bukkit.getPlayer(playerName) != null
}
