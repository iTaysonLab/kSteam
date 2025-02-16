package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.notifications.Notification
import bruhcollective.itaysonlab.ksteam.models.notifications.NotificationFeed
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import steam.enums.ESteamNotificationType
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
            .partition { ESteamNotificationType.fromValue(it.notification_type ?: 0) == ESteamNotificationType.k_ESteamNotificationType_Item }
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

                when (ESteamNotificationType.fromValue(not.notification_type ?: 0)) {
                    ESteamNotificationType.k_ESteamNotificationType_Wishlist -> {
                        val appId = json.decodeFromString<Notification.WishlistSale.Body>(rawJson).appId

                        Notification.WishlistSale(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() == true,
                            hidden = not.hidden == true,
                            appSummary = if (appId != 0) {
                                steamClient.store.querySteamApplications(listOf(AppId(appId))).first()
                            } else {
                                null
                            }
                        )
                    }

                    ESteamNotificationType.k_ESteamNotificationType_Item -> {
                        Notification.Item(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() == true,
                            hidden = not.hidden == true,
                            item = bruhcollective.itaysonlab.ksteam.models.econ.EconItemReference(
                                json.decodeFromString<Notification.Item.Body>(
                                    rawJson
                                ).let {
                                    Triple(it.appId, it.contextId, it.assetId)
                                })
                        )
                    }

                    ESteamNotificationType.k_ESteamNotificationType_FriendInvite -> {
                        Notification.FriendRequest(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() == true,
                            hidden = not.hidden == true,
                            requestor = steamClient.persona.persona(
                                SteamId.fromAccountId(
                                    id = json.decodeFromString<Notification.FriendRequest.Body>(rawJson).accountId
                                )
                            )
                        )
                    }

                    ESteamNotificationType.k_ESteamNotificationType_Gift -> {
                        Notification.Gift(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() == true,
                            hidden = not.hidden == true,
                            gifter = steamClient.persona.persona(
                                SteamId.fromAccountId(
                                    id = json.decodeFromString<Notification.Gift.Body>(rawJson).accountId
                                )
                            )
                        )
                    }

                    ESteamNotificationType.k_ESteamNotificationType_General -> {
                        val body = json.decodeFromString<Notification.General.Body>(rawJson)

                        Notification.General(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() == true,
                            hidden = not.hidden == true,
                            title = body.title,
                            description = body.body,
                            iconUrl = body.image,
                            link = body.link
                        )
                    }

                    else -> {
                        Notification.Unknown(
                            timestamp = not.timestamp ?: 0,
                            unread = not.read?.not() == true,
                            hidden = not.hidden == true,
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