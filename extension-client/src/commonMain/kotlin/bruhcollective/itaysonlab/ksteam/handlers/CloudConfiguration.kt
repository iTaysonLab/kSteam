package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.util.executeSteamOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import steam.webui.cloudconfigstore.*

internal class CloudConfiguration(
    private val steamClient: ExtendedSteamClient
) {
    private val currentState = MutableStateFlow<Map<Int, CCloudConfigStore_NamespaceData>>(emptyMap())

    suspend fun request(namespace: Int): Flow<List<CCloudConfigStore_Entry>> {
        if (currentState.value.containsKey(namespace).not()) {
            downloadAndSet(namespace)
        }

        return currentState.map { namespaces ->
            namespaces[namespace]?.entries ?: emptyList()
        }
    }

    private suspend fun downloadAndSet(namespace: Int, version: Long = 0) {
        steamClient.grpc.cloudConfigStore.Download().executeSteamOrNull(
            data = CCloudConfigStore_Download_Request(
                versions = listOf(
                    CCloudConfigStore_NamespaceVersion(
                        enamespace = namespace,
                        version = version
                    )
                )
            )
        )?.data_?.firstOrNull().let { newNamespace ->
            steamClient.logger.logDebug("CloudConfig:Rpc") { "Namespace received: $newNamespace" }
            currentState.update { map ->
                map.toMutableMap().apply {
                    put(
                        namespace,
                        newNamespace ?: CCloudConfigStore_NamespaceData(
                            version = version,
                            enamespace = namespace,
                            horizon = 0L
                        )
                    )
                }
            }
        }
    }

    init {
        steamClient.onRpc("CloudConfigStoreClient.NotifyChange#1") { packet ->
            CCloudConfigStore_Change_Notification.ADAPTER.decode(packet.payload).let { notification ->
                notification.versions.forEach { updatedNamespace ->
                    steamClient.logger.logDebug("CloudConfig:Rpc") { "Namespace ${updatedNamespace.enamespace} will be updated to version ${updatedNamespace.version}" }
                    downloadAndSet(updatedNamespace.enamespace ?: return@forEach)
                }
            }
        }
    }
}