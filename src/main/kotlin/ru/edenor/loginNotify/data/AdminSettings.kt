package ru.edenor.loginNotify.data

data class AdminSettings(val adminName: String, val toggled: Boolean) {
  companion object {
    @JvmStatic
    fun defaultAdminSettings(adminName: String): AdminSettings {
      return AdminSettings(adminName, toggled = true)
    }
  }
}