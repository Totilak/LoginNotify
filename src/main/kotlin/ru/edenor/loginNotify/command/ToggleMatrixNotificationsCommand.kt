package ru.edenor.loginNotify.command

import io.papermc.paper.command.brigadier.Commands.literal
import org.bukkit.command.CommandSender
import ru.edenor.loginNotify.LoginNotify.Companion.MATRIX_TOGGLE_PERMISSION
import ru.edenor.loginNotify.command.CommandExtensions.requiresPermission
import ru.edenor.loginNotify.command.CommandExtensions.simplyRun
import ru.edenor.loginNotify.data.AdminSettings
import ru.edenor.loginNotify.data.Storage

class ToggleMatrixNotificationsCommand(private val storage: Storage) {

  val matrixShutUp =
      literal("matrixshutup")
          .requiresPermission(MATRIX_TOGGLE_PERMISSION)
          .simplyRun(::toggleMatrixNotifications)
          .build()

  private fun toggleMatrixNotifications(sender: CommandSender) {
    val settings = storage.getSettings(sender.name)
    val doesNowIgnoreMatrixNotifications = !settings.toggleMatrix
    storage.setSettings(
        AdminSettings(
            sender.name, settings.toggled, doesNowIgnoreMatrixNotifications))

    if (doesNowIgnoreMatrixNotifications) {
      sender.sendRichMessage(
          "<green>Теперь Вы игнорируете уведомления <gray>[<dark_aqua>Matrix</dark_aqua>]</gray>")
    } else {
      sender.sendRichMessage(
          "<green>Вы больше не игнорируете уведомления <gray>[<dark_aqua>Matrix</dark_aqua>]</gray>")
    }
  }
}
