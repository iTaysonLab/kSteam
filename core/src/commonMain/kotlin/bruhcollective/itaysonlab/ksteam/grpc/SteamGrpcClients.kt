package bruhcollective.itaysonlab.ksteam.grpc

import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import steam.webui.authentication.AuthenticationService
import steam.webui.authentication.GrpcAuthenticationService
import steam.webui.cloudconfigstore.CloudConfigStoreService
import steam.webui.cloudconfigstore.GrpcCloudConfigStoreService
import steam.webui.community.CommunityService
import steam.webui.community.GrpcCommunityService
import steam.webui.econ.EconService
import steam.webui.econ.GrpcEconService
import steam.webui.friendslist.FriendsListService
import steam.webui.friendslist.GrpcFriendsListService
import steam.webui.player.GrpcPlayerService
import steam.webui.player.PlayerService
import steam.webui.publishedfile.GrpcPublishedFileService
import steam.webui.publishedfile.PublishedFileService
import steam.webui.steamnotification.GrpcSteamNotificationService
import steam.webui.steamnotification.SteamNotificationService
import steam.webui.storebrowse.GrpcStoreBrowseService
import steam.webui.storebrowse.StoreBrowseService
import steam.webui.twofactor.GrpcTwoFactorService
import steam.webui.twofactor.TwoFactorService
import steam.webui.useraccount.GrpcUserAccountService
import steam.webui.useraccount.UserAccountService
import steam.webui.usergameactivity.GrpcUserGameActivityService
import steam.webui.usergameactivity.UserGameActivityService
import steam.webui.usernews.GrpcUserNewsService
import steam.webui.usernews.UserNewsService

/**
 * Provides auto-generated gRPC clients for Steam Network API, based on dumped protobufs.
 *
 * These methods accept and return raw Protobuf objects.
 *
 * Example:
 * ```kotlin
 * steamClient.grpc.authentication.BeginAuthSessionViaCredentials().executeSteam(
 *     data = CAuthentication_BeginAuthSessionViaCredentials_Request(...), // raw protobuf message
 *     anonymous = true, // true - don't suspend if unauthorized
 *     web = false, // true - force execution on REST API
 * )
 * ```
 */
interface SteamGrpcClients {
    val authentication: AuthenticationService
    val cloudConfigStore: CloudConfigStoreService
    val community: CommunityService
    val econ: EconService
    val friendsList: FriendsListService
    val player: PlayerService
    val publishedFile: PublishedFileService
    val steamNotification: SteamNotificationService
    val storeBrowse: StoreBrowseService
    val twoFactor: TwoFactorService
    val userAccount: UserAccountService
    val userGameActivity: UserGameActivityService
    val userNews: UserNewsService
}

internal class SteamGrpcClientsImpl (
    unifiedMessages: UnifiedMessages
): SteamGrpcClients {
    override val authentication = GrpcAuthenticationService(unifiedMessages)
    override val cloudConfigStore = GrpcCloudConfigStoreService(unifiedMessages)
    override val community = GrpcCommunityService(unifiedMessages)
    override val econ = GrpcEconService(unifiedMessages)
    override val friendsList = GrpcFriendsListService(unifiedMessages)
    override val player = GrpcPlayerService(unifiedMessages)
    override val publishedFile = GrpcPublishedFileService(unifiedMessages)
    override val steamNotification = GrpcSteamNotificationService(unifiedMessages)
    override val storeBrowse = GrpcStoreBrowseService(unifiedMessages)
    override val twoFactor = GrpcTwoFactorService(unifiedMessages)
    override val userAccount = GrpcUserAccountService(unifiedMessages)
    override val userGameActivity = GrpcUserGameActivityService(unifiedMessages)
    override val userNews = GrpcUserNewsService(unifiedMessages)
}