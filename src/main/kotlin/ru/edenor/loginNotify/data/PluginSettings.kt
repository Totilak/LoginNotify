package ru.edenor.loginNotify.data

data class PluginSettings(
  val webhookEnabled: Boolean,
  val discordWebhookUrl: String
)
