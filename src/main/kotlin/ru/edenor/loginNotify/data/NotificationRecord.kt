package ru.edenor.loginNotify.data

import java.time.Instant

data class NotificationRecord(val playerName: String, val comment: String, val createdAt: Instant, val addedBy: String)