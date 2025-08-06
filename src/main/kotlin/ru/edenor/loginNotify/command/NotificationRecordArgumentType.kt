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
import ru.edenor.loginNotify.data.Storage
import java.util.concurrent.CompletableFuture


class NotificationRecordArgumentType(private val storage: Storage) : CustomArgumentType<String, String> {


    override fun parse(reader: StringReader): String {
        return reader.readUnquotedString()
    }

    override fun getNativeType(): ArgumentType<String> {
        return StringArgumentType.word()
    }

    override fun <S : Any> parse(reader: StringReader, source: S): String {

        if (source !is CommandSourceStack) {
            throw ERROR_BAD_SOURCE.create()
        }

        val playerName = nativeType.parse(reader)

        if (storage.getPlayer(playerName) == null) {
            throw ERROR_PLAYER_NOT_TRACKED.create(playerName)
        }

        return playerName

    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val players = storage.getPlayers()
        for (player in players) {
            if (player.playerName.startsWith(builder.remaining)) {
                builder.suggest(player.playerName)
            }
        }
        return builder.buildFuture()
    }

    companion object {
        @JvmStatic
        val ERROR_BAD_SOURCE = SimpleCommandExceptionType(
            MessageComponentSerializer.message()
                .serialize(Component.text("The source needs to be a CommandSourceStack!"))
        )

        val ERROR_PLAYER_NOT_TRACKED = DynamicCommandExceptionType(
            {
                MessageComponentSerializer.message()
                    .serialize(Component.text("$it не отслеживается!"))
            }
        )

        fun getArgument(argument: String, context: CommandContext<CommandSourceStack>): String {
            return context.getArgument(argument, String::class.java)
        }
    }
}

