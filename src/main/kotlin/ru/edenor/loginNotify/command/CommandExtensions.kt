package ru.edenor.loginNotify.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import ru.edenor.loginNotify.LoginNotify.Companion.EDIT_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.LIST_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.MATRIX_TOGGLE_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.NOTIFICATION_PERMISSION
import ru.edenor.loginNotify.LoginNotify.Companion.RELOAD_PERMISSION

object CommandExtensions {
  internal fun LiteralArgumentBuilder<CommandSourceStack>.requiresPermission(
      permission: String
  ) = this.requires { it.sender.hasPermission(permission) }

  internal fun LiteralArgumentBuilder<CommandSourceStack>.requiresPermissions(
      vararg permissions: String
  ) = this.requires { ctx -> permissions.all { ctx.sender.hasPermission(it) } }

  internal fun LiteralArgumentBuilder<CommandSourceStack>
      .requiresAnyPermission() =
      this.requiresPermissions(
          LIST_PERMISSION,
          EDIT_PERMISSION,
          NOTIFICATION_PERMISSION,
          RELOAD_PERMISSION,
          MATRIX_TOGGLE_PERMISSION)

  internal fun LiteralArgumentBuilder<CommandSourceStack>.simplyRun(
      block: (CommandSender) -> Unit
  ) =
      this.executes {
        block.invoke(it.source.sender)
        Command.SINGLE_SUCCESS
      }

  internal fun <S, T> RequiredArgumentBuilder<S, T>.simplyRun(
      block: (CommandContext<S>) -> Unit
  ) =
      this.executes {
        block.invoke(it)
        Command.SINGLE_SUCCESS
      }
}
