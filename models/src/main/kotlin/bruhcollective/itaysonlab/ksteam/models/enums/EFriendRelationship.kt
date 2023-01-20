package bruhcollective.itaysonlab.ksteam.models.enums

import bruhcollective.itaysonlab.ksteam.models.SteamId
import steam.webui.friendslist.CMsgClientFriendsList_Friend

enum class EFriendRelationship {
    None,
    Blocked,
    RequestRecipient,
    Friend,
    RequestInitiator,
    Ignored,
    IgnoredFriend;

    companion object {
        fun byEncoded(num: Int?) = values().getOrElse(num ?: 0) { None }
    }
}

val CMsgClientFriendsList_Friend.relationship get() = EFriendRelationship.byEncoded(efriendrelationship)
val CMsgClientFriendsList_Friend.steamId get() = SteamId(ulfriendid?.toULong() ?: 0u)