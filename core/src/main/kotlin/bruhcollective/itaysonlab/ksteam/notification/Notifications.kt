package bruhcollective.itaysonlab.ksteam.notification

import bruhcollective.itaysonlab.ksteam.handlers.Pics

/**
 * The kSteam Notification Subsystem
 *
 * There are two types of notifications:
 * - Internal: kSteam-unique system notifications (PICS update)
 * - External: like in Steam (friend went online/started a game/written a message)
 */
class Notifications {

    sealed class Internal {

        /**
         * A PICS (metadata database) state is changed
         */
        class PicsStateChanged(val state: Pics.PicsState)

    }
}