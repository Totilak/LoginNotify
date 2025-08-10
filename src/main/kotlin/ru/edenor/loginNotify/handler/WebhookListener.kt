package ru.edenor.loginNotify.handler

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import ru.edenor.loginNotify.LoginNotify
import ru.edenor.loginNotify.data.Storage
import java.net.URI
import java.net.http.HttpClient

class WebhookListener(private val plugin: LoginNotify, private val storage: Storage): Listener {
  private val httpClient = HttpClient.newHttpClient()

  @EventHandler(ignoreCancelled = true)
  fun sendWebhookOnTrackedPlayerJoin(event: TrackedPlayerJoinEvent) {
    val (webhookEnabled, discordWebhookUrl) = storage.pluginSettings
    if (!webhookEnabled || discordWebhookUrl.isEmpty()) return

    Bukkit.getAsyncScheduler().runNow(plugin) {
      val dispatcher = WebhookDispatcher(httpClient)
      dispatcher.sendWebhook(event.notificationRecord, URI.create(discordWebhookUrl))
    }
  }
}