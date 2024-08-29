package bruhcollective.itaysonlab.ksteam.models.clientcomm

import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.platform.Immutable

/**
 * Describes the entry in the application list.
 */
@Immutable
data class DeviceAppListEntry (
    /**
     * Application ID.
     */
    val id: AppId,

    /**
     * Application name.
     */
    val name: String,

    /**
     * Application state on the remote device.
     */
    val state: State,

    /**
     * Is this app marked as favorite?
     */
    val favorite: Boolean,
) {
    @Immutable
    sealed interface State {
        /**
         * Application is installed and ready to launch.
         */
        @Immutable
        data class Installed (
            /**
             * Is this application running right now?
             */
            val isPlaying: Boolean,

            /**
             * Size of the application on the storage drive.
             */
            val size: Long
        ): State

        /**
         * Application is being installing or updated
         */
        @Immutable
        data class Downloading (
            /**
             * Is this download paused?
             */
            val paused: Boolean,

            /**
             * Amount of bytes already downloaded.
             */
            val bytesDownloaded: Long,

            /**
             * Amount of bytes that will be downloaded.
             */
            val bytesToDownload: Long,

            /**
             * Amount of bytes already staged (written on disk).
             *
             * Due to Steam downloads being compressed, this will be larger than download byte count.
             */
            val bytesStaged: Long,

            /**
             * Amount of bytes that is left to be staged (written on disk).
             *
             * Due to Steam downloads being compressed, this will be larger than download byte count.
             */
            val bytesToStage: Long,

            /**
             * Network byte transfer rate, in bytes per second (?).
             */
            val byteTransferRate: Long,

            /**
             * Seconds ETA reported by the remote client
             */
            val estimatedSecondsLeft: Long,

            /**
             * Source build ID. Can be used to determine if the app is updating (if app was installed, this will be non-zero).
             */
            val sourceBuildId: Long,

            /**
             * Target build ID.
             */
            val targetBuildId: Long,

            /**
             * Position in queue, where 0 means top of the list.
             */
            val queuePosition: Int,
        ): State {
            val isUpdate: Boolean
                get() = sourceBuildId != 0L
        }

        /**
         * Application is being uninstalled.
         */
        @Immutable
        data object Uninstalling: State

        /**
         * Application is available to install.
         */
        @Immutable
        data class Available (
            /**
             * Reported application size, in bytes
             */
            val bytesRequired: Long
        ): State

        /**
         * Application is unavailable due to lack of storage space.
         */
        @Immutable
        data class RequiresMoreStorageSpace (
            /**
             * Reported application size, in bytes
             */
            val bytesRequired: Long,

            /**
             * Available storage size, in bytes
             */
            val bytesAvailable: Long
        ): State {
            /**
             * How much storage space should be freed, in bytes
             */
            val bytesUnavailable: Long
                get() = bytesRequired - bytesAvailable
        }

        /**
         * Application is unavailable due to platform support.
         */
        @Immutable
        data object UnsupportedPlatform: State
    }
}