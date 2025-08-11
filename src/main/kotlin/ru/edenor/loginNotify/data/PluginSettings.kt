package ru.edenor.loginNotify.data

data class PluginSettings(
  val webhookEnabled: Boolean = false,
  val discordWebhookUrl: String = ""
)
