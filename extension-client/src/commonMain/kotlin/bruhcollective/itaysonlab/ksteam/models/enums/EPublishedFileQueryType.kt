package bruhcollective.itaysonlab.ksteam.models.enums

/**
 * Ways you can query for UGC items.
 *
 * Source: https://partner.steamgames.com/doc/webapi/IPublishedFileService#EPublishedFileQueryType
 */
enum class EPublishedFileQueryType {
    RankedByVote,
    RankedByPublicationDate,
    AcceptedForGameRankedByAcceptanceDate,
    RankedByTrend,
    FavoritedByFriendsRankedByPublicationDate,
    CreatedByFriendsRankedByPublicationDate,
    RankedByNumTimesReported,
    CreatedByFollowedUsersRankedByPublicationDate,
    NotYetRated,
    RankedByTotalUniqueSubscriptions,
    RankedByTotalVotesAsc,
    RankedByVotesUp,
    RankedByTextSearch,
    RankedByPlaytimeTrend,
    RankedByTotalPlaytime,
    RankedByAveragePlaytimeTrend,
    RankedByLifetimeAveragePlaytime,
    RankedByPlaytimeSessionsTrend,
    RankedByLifetimePlaytimeSessions,
    RankedByInappropriateContentRating,
    RankedByBanContentCheck,
    RankedByLastUpdatedDate,
}