package ru.edenor.loginNotify.data

import org.bukkit.configuration.ConfigurationSection
import ru.edenor.loginNotify.LoginNotify
import java.time.Instant

class ConfigStorage(private val plugin: LoginNotify) : Storage {

    private var cache: MutableMap<String, NotificationRecord>? = null

    private fun getCache(): MutableMap<String, NotificationRecord> {
        if (cache == null) {
            cache = getPlayersUncached().associateBy { it.playerName }.toMutableMap()
        }
        return cache!!
    }

    override fun getPlayer(username: String): NotificationRecord? = getCache()[username]


    override fun addPlayer(record: NotificationRecord) {
        val section = plugin.config.getOrCreateConfigurationSection(sectionName)

        val playerSection = section.getOrCreateConfigurationSection(record.playerName)
        playerSection.set(comment, record.comment)
        playerSection.set(createdAt, record.createdAt.toString())
        playerSection.set(addedBy, record.addedBy)

        plugin.saveConfig()
        getCache()[record.playerName] = record
    }

    override fun removePlayer(username: String) {
        val section = plugin.config.getOrCreateConfigurationSection(sectionName)
        section.set(username, null)

        plugin.saveConfig()
        getCache().remove(username)
    }

    override fun getPlayers(): List<NotificationRecord> = getCache().values.toList()

    fun getPlayersUncached(): List<NotificationRecord> {
        val records = mutableListOf<NotificationRecord>()
        val section = plugin.config.getOrCreateConfigurationSection(sectionName)
        val keys: Set<String> = section.getKeys(false)

        for (key in keys) {
            val playerSection = section.getConfigurationSection(key)!!
            val record = NotificationRecord(
                key,
                playerSection.getString(comment, "Отсутствует")!!,
                Instant.parse(playerSection.getString(createdAt, "2002-09-11T10:15:30.00Z")!!),
                playerSection.getString(addedBy, "Система")!!
            )
            records.add(record)
        }
        return records
    }


    fun ConfigurationSection.getOrCreateConfigurationSection(name: String): ConfigurationSection =
        this.getConfigurationSection(name)
            ?: this.createSection(name)

    companion object {
        const val sectionName = "players"
        const val comment = "comment"
        const val createdAt = "created_at"
        const val addedBy = "added_by"
    }
}
