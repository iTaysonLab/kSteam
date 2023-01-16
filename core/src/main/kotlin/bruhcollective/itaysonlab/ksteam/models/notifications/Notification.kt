package bruhcollective.itaysonlab.ksteam.models.notifications

/**
 * Represents a notification on the Steam network.
 */
sealed class Notification {


    /**
     * kSteam does not know about the notification type.
     *
     * Data is provided in raw form if you want to implement support before kSteam does.
     */
    class Unknown: Notification() {

    }
}