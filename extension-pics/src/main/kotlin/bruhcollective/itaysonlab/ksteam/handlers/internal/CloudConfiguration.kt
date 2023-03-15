package bruhcollective.itaysonlab.ksteam.handlers.internal

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.debug.logDebug
import bruhcollective.itaysonlab.ksteam.handlers.BaseHandler
import bruhcollective.itaysonlab.ksteam.handlers.unifiedMessages
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import steam.webui.cloudconfigstore.*

internal class CloudConfiguration(
    private val steamClient: SteamClient
) : BaseHandler {
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
        steamClient.unifiedMessages.execute(
            methodName = "CloudConfigStore.Download",
            requestAdapter = CCloudConfigStore_Download_Request.ADAPTER,
            responseAdapter = CCloudConfigStore_Download_Response.ADAPTER,
            requestData = CCloudConfigStore_Download_Request(
                versions = listOf(
                    CCloudConfigStore_NamespaceVersion(
                        enamespace = namespace,
                        version = version
                    )
                )
            ),
        ).dataNullable?.data_?.firstOrNull().let { newNamespace ->
            logDebug("CloudConfig:Rpc", "Namespace received: $newNamespace")
            currentState.update { map ->
                map.toMutableMap().apply {
                    put(
                        namespace,
                        newNamespace ?: CCloudConfigStore_NamespaceData(version = version, enamespace = namespace, horizon = 0L)
                    )
                }
            }
        }
    }

    override suspend fun onRpcEvent(rpcMethod: String, packet: SteamPacket) {
        if (rpcMethod == "CloudConfigStoreClient.NotifyChange#1") {
            packet.getProtoPayload(CCloudConfigStore_Change_Notification.ADAPTER).dataNullable?.let { notification ->
                notification.versions.forEach { updatedNamespace ->
                    logDebug("CloudConfig:Rpc", "Namespace ${updatedNamespace.enamespace} will be updated to version ${updatedNamespace.version}")
                    downloadAndSet(updatedNamespace.enamespace ?: return@forEach)
                }
            }
        }
    }
}