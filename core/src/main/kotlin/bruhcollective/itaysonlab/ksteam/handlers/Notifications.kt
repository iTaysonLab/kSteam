package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.econ.EconItemReference
import bruhcollective.itaysonlab.ksteam.models.notifications.Notification
import bruhcollective.itaysonlab.ksteam.models.notifications.NotificationFeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import steam.webui.steamnotification.CSteamNotification_GetSteamNotifications_Request
import steam.webui.steamnotification.CSteamNotification_GetSteamNotifications_Response
import steam.webui.steamnotification.SteamNotificationType

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Notifications(
    private val steamClient: SteamClient
): BaseHandler {
    private val json = Json { ignoreUnknownKeys = true }

    private val _notifications = MutableStateFlow<NotificationFeed>(NotificationFeed.Loading)
    val notifications = _notifications.asStateFlow()

    private val _confirmationCount = MutableStateFlow(0)
    val confirmationCount = _confirmationCount.asStateFlow()

    /**
     * Requests notifications and confirmation count.
     *
     * @param includeHidden include dismissed notifications
     */
    suspend fun requestNotifications(includeHidden: Boolean) {
        _notifications.value = NotificationFeed.Loading

        val rawNotifications = steamClient.webApi.execute(
            methodName = "SteamNotification.GetSteamNotifications",
            requestAdapter = CSteamNotification_GetSteamNotifications_Request.ADAPTER,
            responseAdapter = CSteamNotification_GetSteamNotifications_Response.ADAPTER,
            requestData = CSteamNotification_GetSteamNotifications_Request(include_hidden = includeHidden)
        )

        _confirmationCount.value = rawNotifications.data.confirmation_count ?: 0

        val parsedNotifications = rawNotifications.data.notifications
            .partition { it.notification_type == SteamNotificationType.Item }
            .let { itemsAndOthers ->
                val itemWithExtras = if (itemsAndOthers.first.isNotEmpty()) {
                    itemsAndOthers.first.maxBy {
                        it.timestamp ?: 0
                    } to (itemsAndOthers.first.size - 1)
                } else {
                    null
                }

                return@let itemsAndOthers.second
                    .map { it to 0 }
                    .toMutableList()
                    .also {
                        if (itemWithExtras != null) {
                            it.add(itemWithExtras)
                        }
                    }
            }.sortedByDescending {
                it.first.timestamp
            }.map { notificationAndExtrasPair ->
                val not = notificationAndExtrasPair.first
                val rawJson = not.body_data ?: "{}"

                when (not.notification_type) {
                    SteamNotificationType.Wishlist -> {
                        val appId = json.decodeFromString<Notification.WishlistSale.Body>(rawJson).appId

                        Notification.WishlistSale(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() ?: false,
                            hidden = not.hidden ?: false,
                            app = if (appId != 0) {
                                steamClient.store.getApp(AppId(appId))
                            } else {
                                null
                            }
                        )
                    }

                    SteamNotificationType.Item -> {
                        Notification.Item(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() ?: false,
                            hidden = not.hidden ?: false,
                            item = EconItemReference(json.decodeFromString<Notification.Item.Body>(rawJson).let {
                                Triple(it.appId, it.contextId, it.assetId)
                            })
                        )
                    }

                    SteamNotificationType.FriendInvite -> {
                        Notification.FriendRequest(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() ?: false,
                            hidden = not.hidden ?: false,
                            requestor = steamClient.persona.persona(
                                SteamId.fromAccountId(
                                    id = json.decodeFromString<Notification.FriendRequest.Body>(rawJson).accountId
                                )
                            )
                        )
                    }

                    SteamNotificationType.Gift -> {
                        Notification.Gift(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() ?: false,
                            hidden = not.hidden ?: false,
                            gifter = steamClient.persona.persona(
                                SteamId.fromAccountId(
                                    id = json.decodeFromString<Notification.Gift.Body>(rawJson).accountId
                                )
                            )
                        )
                    }

                    SteamNotificationType.Promotion -> {
                        val body = json.decodeFromString<Notification.Promotion.Body>(rawJson)

                        Notification.Promotion(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() ?: false,
                            hidden = not.hidden ?: false,
                            title = body.title,
                            description = body.body,
                            iconUrl = body.image,
                            link = body.link
                        )
                    }

                    else -> {
                        Notification.Unknown(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() ?: false,
                            hidden = not.hidden ?: false,
                            rawJsonData = rawJson
                        )
                    }
                }
            }

        _notifications.value = NotificationFeed.Loaded(
            notifications = parsedNotifications
        )
    }

    override suspend fun onEvent(packet: SteamPacket) {
        when (packet.messageId) {
            else -> {}
        }
    }
}