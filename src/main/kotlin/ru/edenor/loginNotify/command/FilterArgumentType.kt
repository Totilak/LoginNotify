package ru.edenor.loginNotify.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import net.kyori.adventure.text.Component
import java.util.concurrent.CompletableFuture

class FilterArgumentType() : CustomArgumentType<String, String> {
  companion object {
    @JvmStatic
    val ERROR_BAD_SOURCE =
        SimpleCommandExceptionType(
            MessageComponentSerializer.message()
                .serialize(
                    Component.text(
                        "The source needs to be a CommandSourceStack!")))

    @JvmStatic
    val ERROR_FILTER_NOT_FOUND = DynamicCommandExceptionType {
      MessageComponentSerializer.message()
          .serialize(Component.text("$it не является фильтром!"))
    }

    fun getArgument(
        argument: String,
        context: CommandContext<CommandSourceStack>
    ): String = context.getArgument(argument, String::class.java)

    val filterList: List<String> =
        listOf("-online", "+online", "-offline", "+offline")
  }

  override fun parse(reader: StringReader): String = reader.readUnquotedString()

  override fun getNativeType(): ArgumentType<String> = StringArgumentType.word()

  override fun <S : Any> parse(reader: StringReader, source: S): String {
    if (source !is CommandSourceStack) {
      throw ERROR_BAD_SOURCE.create()
    }

    val filter = nativeType.parse(reader)

    if (!filterList.contains(filter)) {
      throw ERROR_FILTER_NOT_FOUND.create(filter)
    }

    return filter
  }

  override fun <S : Any> listSuggestions(
      context: CommandContext<S>,
      builder: SuggestionsBuilder
  ): CompletableFuture<Suggestions> {
    filterList
        .filter { it.startsWith(builder.remaining) }
        .forEach { builder.suggest(it) }

    return builder.buildFuture()
  }
}
