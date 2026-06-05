package app.aaps.core.interfaces.notifications

enum class NotificationLevel(val priority: Int) {
    IMPORTANT(0),
    NORMAL(1),
    LOW(2),
    INFO(3),
    ANNOUNCEMENT(4)
}
