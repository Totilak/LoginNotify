package ru.edenor.loginNotify.data

interface Storage {
    fun addPlayer(record: NotificationRecord)
    fun removePlayer(username: String)
    fun getPlayers(): List<NotificationRecord>
    fun getPlayer(username: String): NotificationRecord?
}