package bruhcollective.itaysonlab.ksteam.grpc

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import steam.webui.cloudconfigstore.CloudConfigStore
import steam.webui.cloudconfigstore.GrpcCloudConfigStore
import steam.webui.community.GrpcCommunity
import steam.webui.econ.Econ
import steam.webui.econ.GrpcEcon
import steam.webui.friendslist.FriendsList
import steam.webui.friendslist.GrpcFriendsList
import steam.webui.player.GrpcPlayer
import steam.webui.player.Player
import steam.webui.publishedfile.GrpcPublishedFile
import steam.webui.publishedfile.PublishedFile
import steam.webui.steamnotification.GrpcSteamNotification
import steam.webui.steamnotification.SteamNotification
import steam.webui.storebrowse.GrpcStoreBrowse
import steam.webui.storebrowse.StoreBrowse
import steam.webui.twofactor.GrpcTwoFactor
import steam.webui.twofactor.TwoFactor
import steam.webui.useraccount.GrpcUserAccount
import steam.webui.useraccount.UserAccount
import steam.webui.usergameactivity.GrpcUserGameActivity
import steam.webui.usergameactivity.UserGameActivity
import steam.webui.usernews.GrpcUserNews

/**
 * Holder for gRPC clients for accessing advanced Steam subsystems.
 */
interface ExtendedSteamGrpcClients: SteamGrpcClients {
    val cloudConfigStore: CloudConfigStore
    val community: GrpcCommunity
    val econ: Econ
    val friendsList: FriendsList
    val player: Player
    val publishedFile: PublishedFile
    val steamNotification: SteamNotification
    val storeBrowse: StoreBrowse
    val twoFactor: TwoFactor
    val userAccount: UserAccount
    val userGameActivity: UserGameActivity
    val userNews: GrpcUserNews
}

internal class ExtendedSteamGrpcClientsImpl (
    internalGrpcClients: SteamGrpcClients,
    unifiedMessages: UnifiedMessages
): ExtendedSteamGrpcClients, SteamGrpcClients by internalGrpcClients {
    constructor(client: SteamClient): this(client.grpc, client.unifiedMessages)

    override val cloudConfigStore = GrpcCloudConfigStore(unifiedMessages)
    override val community = GrpcCommunity(unifiedMessages)
    override val econ = GrpcEcon(unifiedMessages)
    override val friendsList = GrpcFriendsList(unifiedMessages)
    override val player = GrpcPlayer(unifiedMessages)
    override val publishedFile = GrpcPublishedFile(unifiedMessages)
    override val steamNotification = GrpcSteamNotification(unifiedMessages)
    override val storeBrowse = GrpcStoreBrowse(unifiedMessages)
    override val twoFactor = GrpcTwoFactor(unifiedMessages)
    override val userAccount = GrpcUserAccount(unifiedMessages)
    override val userGameActivity = GrpcUserGameActivity(unifiedMessages)
    override val userNews = GrpcUserNews(unifiedMessages)
}