package ru.edenor.loginNotify.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import ru.edenor.loginNotify.LoginNotify

object CommandExtensions {
  internal fun LiteralArgumentBuilder<CommandSourceStack>.requiresPermission(permission: String) =
    this.requires { it.sender.hasPermission(permission) }

  internal fun LiteralArgumentBuilder<CommandSourceStack>.requiresPermissions(vararg permissions: String) =
    this.requires { ctx -> permissions.all { ctx.sender.hasPermission(it) } }

  internal fun LiteralArgumentBuilder<CommandSourceStack>.requiresAnyPermission() =
    this.requiresPermissions(
      LoginNotify.Companion.LIST_PERMISSION,
      LoginNotify.Companion.EDIT_PERMISSION,
      LoginNotify.Companion.NOTIFICATION_PERMISSION
    )

  internal fun LiteralArgumentBuilder<CommandSourceStack>.simplyRun(
    block: (CommandSender) -> Unit
  ) =
    this.executes { block.invoke(it.source.sender); Command.SINGLE_SUCCESS }

  internal fun <S, T> RequiredArgumentBuilder<S, T>.simplyRun(
    block: (CommandContext<S>) -> Unit
  ) =
    this.executes { block.invoke(it); Command.SINGLE_SUCCESS }
}