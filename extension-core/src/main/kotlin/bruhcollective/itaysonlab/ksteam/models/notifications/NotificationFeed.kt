package bruhcollective.itaysonlab.ksteam.models.notifications

sealed class NotificationFeed {
    object Loading : NotificationFeed()

    class Loaded internal constructor(
        val notifications: List<Notification>
    ) : NotificationFeed()
}