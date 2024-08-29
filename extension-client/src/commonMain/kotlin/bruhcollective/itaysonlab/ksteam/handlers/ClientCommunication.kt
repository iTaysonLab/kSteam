package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.clientcomm.ActiveDeviceSession
import bruhcollective.itaysonlab.ksteam.models.clientcomm.RemoteAppListEntry
import bruhcollective.itaysonlab.ksteam.models.clientcomm.RemoteDeviceId
import bruhcollective.itaysonlab.ksteam.models.enums.EGamingDeviceType
import bruhcollective.itaysonlab.ksteam.models.enums.EOSType
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import steam.webui.clientcomm.CClientComm_EnableOrDisableDownloads_Request
import steam.webui.clientcomm.CClientComm_GetAllClientLogonInfo_Request
import steam.webui.clientcomm.CClientComm_GetClientAppList_Request
import steam.webui.clientcomm.CClientComm_InstallClientApp_Request
import steam.webui.clientcomm.CClientComm_LaunchClientApp_Request
import steam.webui.clientcomm.CClientComm_SetClientAppUpdateState_Request
import steam.webui.clientcomm.CClientComm_UninstallClientApp_Request

/**
 * Allows controlling of other running Steam clients.
 */
class ClientCommunication (
    private val steamClient: ExtendedSteamClient,
) {
    /**
     * Return the list of active devices that are running any Steam client and connected to the network.
     *
     * Note that this will return ALL connected clients - including web and kSteam. You might want to filter the results based on OS or device type:
     * ```kotlin
     * steamClient.clientCommunication.getActiveDevices().filter(ActiveDeviceSession::probablyValidClient)
     * ```
     */
    suspend fun getActiveDevices(): List<ActiveDeviceSession> {
        return steamClient.grpc.clientComm.GetAllClientLogonInfo().executeSteam(
            data = CClientComm_GetAllClientLogonInfo_Request()
        ).sessions.mapNotNull { steamSession ->
            ActiveDeviceSession(
                id = steamSession.client_instanceid?.let(::RemoteDeviceId) ?: return@mapNotNull null,
                protocolVersion = steamSession.protocol_version ?: 0,
                osName = steamSession.os_name.orEmpty(),
                deviceName = steamSession.machine_name.orEmpty(),
                osType = EOSType.byEncoded(steamSession.os_type),
                deviceType = EGamingDeviceType.byEncoded(steamSession.device_type),
                realm = steamSession.realm ?: 0,
            )
        }
    }

    /**
     * Return the list of applications of the remote device.
     *
     * This is not the list of downloaded apps, but the whole "available" list with a flag indicating about it being downloaded.
     *
     * @param remoteId remote session ID
     * @param filters [InstalledAppsFilter.Changing] will show "apps in progress" - installing/updating/removing
     */
    suspend fun getAppList(
        remoteId: RemoteDeviceId,
        filters: InstalledAppsFilter = InstalledAppsFilter.None
    ): List<RemoteAppListEntry> {
        val remoteList = steamClient.grpc.clientComm.GetClientAppList().executeSteam(
            data = CClientComm_GetClientAppList_Request(
                client_instanceid = remoteId.id,
                language = steamClient.language.vdfName,
                include_client_info = true,
                fields = "games",
                filters = when (filters) {
                    InstalledAppsFilter.None -> "none"
                    InstalledAppsFilter.Changing -> "changing"
                }
            )
        )

        return remoteList.apps.mapNotNull { app ->
            RemoteAppListEntry(
                id = AppId(app.appid ?: return@mapNotNull null),
                name = app.app.orEmpty(),
                favorite = app.favorite == true,
                state = when {
                    app.uninstalling == true -> {
                        RemoteAppListEntry.State.Uninstalling
                    }

                    app.installed == true && app.bytes_downloaded == app.bytes_to_download && app.bytes_staged == app.bytes_to_stage -> {
                        RemoteAppListEntry.State.Installed(
                            isPlaying = app.running == true,
                            size = app.bytes_staged ?: 0L
                        )
                    }

                    app.changing == true -> {
                        RemoteAppListEntry.State.Downloading(
                            paused = app.download_paused == true,
                            bytesToStage = app.bytes_to_stage ?: 0L,
                            bytesStaged = app.bytes_staged ?: 0L,
                            bytesToDownload = app.bytes_to_download ?: 0L,
                            bytesDownloaded = app.bytes_downloaded ?: 0L,
                            byteDownloadRate = app.bytes_download_rate ?: 0,
                            estimatedSecondsLeft = app.estimated_seconds_remaining ?: 0,
                            sourceBuildId = app.source_buildid ?: 0,
                            targetBuildId = app.target_buildid ?: 0,
                            queuePosition = app.queue_position ?: 0,
                        )
                    }

                    app.available_on_platform == false -> {
                        RemoteAppListEntry.State.UnsupportedPlatform
                    }

                    (app.bytes_required ?: 0L) > (remoteList.bytes_available ?: 0L) -> {
                        RemoteAppListEntry.State.InsufficientStorageSpace(
                            bytesRequired = app.bytes_required ?: 0L,
                            bytesAvailable = remoteList.bytes_available ?: 0L,
                        )
                    }

                    else -> {
                        RemoteAppListEntry.State.Available(
                            bytesRequired = app.bytes_required ?: 0L
                        )
                    }
                }
            )
        }
    }

    /**
     * Adds the application to the remote installation queue.
     */
    suspend fun addToInstallQueue(remoteId: RemoteDeviceId, appId: AppId) {
        steamClient.grpc.clientComm.InstallClientApp().executeSteam(
            data = CClientComm_InstallClientApp_Request(
                appid = appId.value,
                client_instanceid = remoteId.id
            )
        )
    }

    /**
     * Sets the update state of an app in remote installation queue.
     *
     * Action set to true will move the requested app to the top of the queue.
     */
    suspend fun setClientAppUpdateState(remoteId: RemoteDeviceId, appId: AppId, action: Boolean) {
        steamClient.grpc.clientComm.SetClientAppUpdateState().executeSteam(
            data = CClientComm_SetClientAppUpdateState_Request(
                appid = appId.value,
                client_instanceid = remoteId.id,
                action = if (action) 1 else 0
            )
        )
    }

    /**
     * Requests to uninstall the app from the device.
     */
    suspend fun uninstall(remoteId: RemoteDeviceId, appId: AppId) {
        steamClient.grpc.clientComm.UninstallClientApp().executeSteam(
            data = CClientComm_UninstallClientApp_Request(
                appid = appId.value,
                client_instanceid = remoteId.id
            )
        )
    }

    /**
     * Pauses or resumes active download - the first item in the queue.
     */
    suspend fun toggleActiveDownload(remoteId: RemoteDeviceId, active: Boolean) {
        steamClient.grpc.clientComm.EnableOrDisableDownloads().executeSteam(
            data = CClientComm_EnableOrDisableDownloads_Request(
                client_instanceid = remoteId.id,
                enable = active
            )
        )
    }

    /**
     * Launches the application on the remote device.
     */
    suspend fun launch(remoteId: RemoteDeviceId, appId: AppId, parameters: String? = null) {
        steamClient.grpc.clientComm.LaunchClientApp().executeSteam(
            data = CClientComm_LaunchClientApp_Request(
                client_instanceid = remoteId.id,
                appid = appId.value,
                query_params = parameters
            )
        )
    }

    enum class InstalledAppsFilter {
        None, Changing
    }
}