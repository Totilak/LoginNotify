package ru.edenor.loginNotify.handler

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import ru.edenor.loginNotify.LoginNotify
import ru.edenor.loginNotify.LoginNotify.Companion.toLoginNotifyFormat
import ru.edenor.loginNotify.data.NotificationRecord
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class WebhookDispatcher(val httpClient: HttpClient) {
  private val gson = GsonBuilder()
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()

  fun sendWebhook(record: NotificationRecord, uri: URI) =
    sendWebhook(toRequest(record), uri)

  /**
   * @throws RuntimeException if response status code is non-2xx
   */
  fun sendWebhook(request: DiscordWebhookRequest, uri: URI) {
    val request = HttpRequest.newBuilder(uri)
      .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(request)))
      .header("Content-Type", "application/json")
      .build()

    val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    if ((response.statusCode() / 100) != 2) {
      throw RuntimeException(
        "Webhook request failed with non-2xx status code ${response.statusCode()}: ${response.body()}")
    }
  }

  fun toRequest(record: NotificationRecord): DiscordWebhookRequest {
    val request = DiscordWebhookRequest()
    val embed = DiscordEmbed()
    embed.title = "`${record.playerName}` зашел на сервер."
    embed.description = "Добавил ${record.addedBy}"
    embed.color = 10290694 // Red-ish color
    embed.author = EmbedAuthor(LoginNotify::class.java.simpleName)
    embed.fields = listOf(EmbedField("Comment:", record.comment),
      EmbedField("Добавлен в", record.createdAt.toLoginNotifyFormat()))
    request.embeds = listOf(embed)
    return request
  }


  /**
   * https://discord.com/developers/docs/resources/webhook#execute-webhook
   */
  @Suppress("unused")
  class DiscordWebhookRequest {
    var content: String? = null
    var username: String? = null
    var avatarUrl: String? = null
    var embeds: List<DiscordEmbed>? = null
    // ... Won't be used
  }

  @Suppress("unused")
  class EmbedAuthor(var name: String) {
    var url: String? = null
    var iconUrl: String? = null
    var proxyIconUrl: String? = null
  }

  @Suppress("unused")
  class EmbedField(var name: String, var value: String) {
    var inline: Boolean? = false
  }

  @Suppress("unused")
  class DiscordEmbed {
    var title: String? = null
    var description: String? = null
    var type: String? = "rich" // Always 'rich' for webhook embeds
    var color: Int? = null
    var author: EmbedAuthor? = null
    var fields: List<EmbedField>? = null
    // ...
  }
}