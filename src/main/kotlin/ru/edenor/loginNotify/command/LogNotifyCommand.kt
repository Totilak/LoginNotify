package ru.edenor.loginNotify.command

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent.runCommand
import net.kyori.adventure.text.event.ClickEvent.suggestCommand
import net.kyori.adventure.text.event.HoverEvent.showText
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.TextDecoration
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
import kotlin.math.min

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
      literal("list")
          .requiresPermission(LIST_PERMISSION)
          .then(
              argument("filter", FilterArgumentType())
                  .then(
                      argument("page", IntegerArgumentType.integer(1))
                          .simplyRun(::sendListWithFilterAndPage))
                  .simplyRun(::sendListWithFilter))
          .then(
              argument("page", IntegerArgumentType.integer(1))
                  .simplyRun(::sendListWithPage))
          .simplyRun(::sendList)

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
        "<green><bold>LoginNotify</bold> <gray>(${plugin.pluginMeta.version})</gray> <dark_aqua>- Позволяет уведомлять администрацию о заходе игрока на сервер")

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

  private fun sendListWithFilter(context: CommandContext<CommandSourceStack>) {
    val sender = context.source.sender
    val filter = StringArgumentType.getString(context, "filter")
    sendList(sender, filter)
  }

  private fun sendListWithPage(context: CommandContext<CommandSourceStack>) {
    val sender = context.source.sender
    val page = context.getArgument("page", Int::class.java)
    sendList(sender, null, page)
  }

  private fun sendListWithFilterAndPage(
      context: CommandContext<CommandSourceStack>
  ) {
    val sender = context.source.sender
    val filter = StringArgumentType.getString(context, "filter")
    val page = context.getArgument("page", Int::class.java)
    sendList(sender, filter, page)
  }

  private fun sendList(
      sender: CommandSender,
      filter: String? = null,
      page: Int = 1
  ) {
    var players = storage.getPlayers().sortedByDescending { it.createdAt }

    players = filterPlayers(players, filter)

    if (players.isEmpty()) {
      sender.sendRichMessage(
          "<red>Нет игроков${if (filter != null) " по фильтру: $filter" else ""}")
      return
    }

    val pageSize = 5
    val totalPages = (players.size + pageSize - 1) / pageSize
    val currentPage = page.coerceIn(1, totalPages)

    sender.sendRichMessage("<yellow>LoginNotify - Просмотр списка:")

    val (startIndex, endIndex) = paginate(currentPage, pageSize, players.size)
    for (i in startIndex until endIndex) {
      sender.sendMessage(getRecordLine(players[i]))
    }

    if (totalPages > 1) {
      sendPageNav(sender, currentPage, totalPages, filter)
    }
  }

  private fun filterPlayers(
      players: List<NotificationRecord>,
      filter: String?
  ): List<NotificationRecord> {
    return when (filter?.lowercase()) {
      "online" -> players.filter { isPlayerOnline(it.playerName) }
      "offline" -> players.filter { !isPlayerOnline(it.playerName) }
      else -> players
    }
  }

  private fun paginate(page: Int, pageSize: Int, total: Int): Pair<Int, Int> {
    val startIndex = (page - 1) * pageSize
    val endIndex = min(startIndex + pageSize, total)
    return startIndex to endIndex
  }

  private fun sendPageNav(
      sender: CommandSender,
      currentPage: Int,
      totalPages: Int,
      filter: String?
  ) {
    val baseCommand = buildString {
      append("/lognotify list")
      if (!filter.isNullOrBlank()) append(" $filter")
      append(" ")
    }

    val nav =
        text()
            .append(text("Страница ", NamedTextColor.GREEN))
            .append(text("$currentPage", NamedTextColor.WHITE))
            .append(text("/", NamedTextColor.WHITE))
            .append(text("$totalPages ", NamedTextColor.WHITE))
            .append(text("▶ ", NamedTextColor.GRAY)) // пробел после стрелки

    val maxShown = 8
    val startPage = ((currentPage - 1) / maxShown) * maxShown + 1
    val endPage = min(startPage + maxShown - 1, totalPages)

    nav.append(text("(", NamedTextColor.GRAY))

    for (p in startPage..endPage) {
      if (p > startPage) nav.append(text(" | ", NamedTextColor.GRAY))

      val pageText =
          text(p.toString(), NamedTextColor.WHITE)
              .decoration(TextDecoration.UNDERLINED, p == currentPage)
              .clickEvent(runCommand("$baseCommand$p"))
              .hoverEvent(
                  showText(text("Перейти на страницу $p", NamedTextColor.GRAY)))

      nav.append(pageText)
    }

    nav.append(text(")", NamedTextColor.GRAY))
    if (endPage < totalPages) nav.append(text(" …", NamedTextColor.GRAY))

    sender.sendMessage(nav.build())
  }

  private fun getRecordLine(record: NotificationRecord): TextComponent {
    val (playerName, comment, createdAt, addedBy) = record
    val formattedDate = createdAt.toLoginNotifyFormat()
    val message = text()
    if (isPlayerOnline(playerName)) {
      val playerNameComponent =
          if (plugin.isEdenorWarningsEnabled()) {
            text(playerName, GREEN)
                .hoverEvent(showText(text("Нажми, чтобы вызвать")))
                .clickEvent(suggestCommand("/ew send check $playerName"))
          } else {
            text(playerName, GREEN)
          }
      message.append(playerNameComponent)
    } else {
      message.append(text(playerName, RED))
    }
    return message
        .append(text(": добавил"))
        .appendSpace()
        .append(text(addedBy, NamedTextColor.GRAY))
        .appendSpace()
        .append(text("в $formattedDate"))
        .appendNewline()
        .append(text("Описание: $comment"))
        .build()
  }

  private fun isPlayerOnline(playerName: String): Boolean =
      Bukkit.getPlayer(playerName) != null
}
