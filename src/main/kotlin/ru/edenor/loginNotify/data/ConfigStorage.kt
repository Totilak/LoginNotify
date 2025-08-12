package ru.edenor.loginNotify.data

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import ru.edenor.loginNotify.LoginNotify
import ru.edenor.loginNotify.data.AdminSettings.Companion.defaultAdminSettings
import java.io.IOException
import java.time.Instant

class ConfigStorage(private val plugin: LoginNotify) : Storage {

  private var cache: MutableMap<String, NotificationRecord>? = null
  private val settingsFile = plugin.dataFolder.resolve(SETTINGS_FILENAME)
  private var settingsConfig = YamlConfiguration.loadConfiguration(settingsFile)

  private fun getCache(): MutableMap<String, NotificationRecord> {
    if (cache == null) {
      cache = getPlayersUncached().associateBy { it.playerName }.toMutableMap()
    }
    return cache!!
  }

  override fun getPlayer(username: String): NotificationRecord? =
      getCache()[username]

  override fun addPlayer(record: NotificationRecord) {
    val section = plugin.config.getOrCreateConfigurationSection(PLAYER_SECTION)

    val playerSection =
        section.getOrCreateConfigurationSection(record.playerName)
    playerSection.set(COMMENT, record.comment)
    playerSection.set(CREATED_AT, record.createdAt.toString())
    playerSection.set(ADDED_BY, record.addedBy)

    plugin.saveConfig()
    getCache()[record.playerName] = record
  }

  override fun removePlayer(username: String) {
    val section = plugin.config.getOrCreateConfigurationSection(PLAYER_SECTION)
    section.set(username, null)

    plugin.saveConfig()
    getCache().remove(username)
  }

  override fun getPlayers(): List<NotificationRecord> =
      getCache().values.toList()

  override fun getSettings(username: String): AdminSettings {
    val section = plugin.config.getOrCreateConfigurationSection(ADMIN_SECTION)
    val adminSection =
        section.getConfigurationSection(username)
            ?: return defaultAdminSettings(username)

    return AdminSettings(
        username,
        adminSection.getBoolean(TOGGLE_NOTIFY),
        adminSection.getBoolean(TOGGLE_MATRIX_NOTIFY))
  }

  override fun setSettings(settings: AdminSettings) {
    val section = plugin.config.getOrCreateConfigurationSection(ADMIN_SECTION)

    val adminSection =
        section.getOrCreateConfigurationSection(settings.adminName)
    adminSection.set(TOGGLE_NOTIFY, settings.toggled)
    adminSection.set(TOGGLE_MATRIX_NOTIFY, settings.toggleMatrix)

    plugin.saveConfig()
  }

  override var pluginSettings: PluginSettings
    get() {
      val section =
          settingsConfig.getOrCreateConfigurationSection(SETTINGS_SECTION)
      return PluginSettings(
          section.getBoolean(WEBHOOK_ENABLED, false),
          section.getString(DISCORD_WEBHOOK_URL, "")!!)
    }
    set(value) {
      val section =
          settingsConfig.getOrCreateConfigurationSection(SETTINGS_SECTION)
      section.set(WEBHOOK_ENABLED, value.webhookEnabled)
      section.set(DISCORD_WEBHOOK_URL, value.discordWebhookUrl)
      try {
        settingsConfig.save(settingsFile)
      } catch (e: IOException) {
        plugin.slF4JLogger.error("Failed to save settings", e)
      }
    }

  override fun reload() {
    settingsConfig = YamlConfiguration.loadConfiguration(settingsFile)
  }

  fun getPlayersUncached(): List<NotificationRecord> {
    val records = mutableListOf<NotificationRecord>()
    val section = plugin.config.getOrCreateConfigurationSection(PLAYER_SECTION)
    val keys: Set<String> = section.getKeys(false)

    for (key in keys) {
      val playerSection = section.getConfigurationSection(key)!!
      val record =
          NotificationRecord(
              key,
              playerSection.getString(COMMENT, "Отсутствует")!!,
              Instant.parse(
                  playerSection.getString(
                      CREATED_AT, "2002-09-11T10:15:30.00Z")!!),
              playerSection.getString(ADDED_BY, "Система")!!)
      records.add(record)
    }
    return records
  }

  fun ConfigurationSection.getOrCreateConfigurationSection(
      name: String
  ): ConfigurationSection =
      this.getConfigurationSection(name) ?: this.createSection(name)

  init {
    settingsFile.parentFile?.mkdirs()

    if (settingsFile.createNewFile()) {
      pluginSettings = PluginSettings()
    }
  }

  companion object {
    const val COMMENT = "comment"
    const val ADDED_BY = "added_by"
    const val CREATED_AT = "created_at"
    const val TOGGLE_NOTIFY = "toggle_notify"
    const val TOGGLE_MATRIX_NOTIFY = "matrix_toggle_notify"
    const val ADMIN_SECTION = "admins"
    const val PLAYER_SECTION = "players"

    const val SETTINGS_FILENAME = "settings.yml"
    const val SETTINGS_SECTION = "settings"
    const val DISCORD_WEBHOOK_URL = "discord_webhook_url"
    const val WEBHOOK_ENABLED = "webhook_enabled"
  }
}
