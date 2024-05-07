package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.notifications.Notification
import bruhcollective.itaysonlab.ksteam.models.notifications.NotificationFeed
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import steam.enums.SteamNotificationType
import steam.webui.steamnotification.CSteamNotification_GetSteamNotifications_Request

/**
 * Access persona data using this interface.
 *
 * All data will be kept inside the in-memory cache.
 */
class Notifications internal constructor(
    private val steamClient: ExtendedSteamClient
) {
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

        val rawNotifications = steamClient.grpc.steamNotification.GetSteamNotifications().executeSteam(
            data = CSteamNotification_GetSteamNotifications_Request(include_hidden = includeHidden)
        )

        _confirmationCount.value = rawNotifications.confirmation_count ?: 0

        val parsedNotifications = rawNotifications.notifications
            .partition { SteamNotificationType.fromValue(it.notification_type ?: 0) == SteamNotificationType.Item }
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

                when (SteamNotificationType.fromValue(not.notification_type ?: 0)) {
                    SteamNotificationType.Wishlist -> {
                        val appId = json.decodeFromString<Notification.WishlistSale.Body>(rawJson).appId

                        Notification.WishlistSale(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() ?: false,
                            hidden = not.hidden ?: false,
                            appSummary = if (appId != 0) {
                                steamClient.store.getAppSummaries(listOf(appId)).values.first()
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
                            item = bruhcollective.itaysonlab.ksteam.models.econ.EconItemReference(
                                json.decodeFromString<Notification.Item.Body>(
                                    rawJson
                                ).let {
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
}