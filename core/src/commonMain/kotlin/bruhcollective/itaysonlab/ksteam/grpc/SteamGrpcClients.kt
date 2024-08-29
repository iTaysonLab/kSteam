package bruhcollective.itaysonlab.ksteam.grpc

import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import steam.webui.authentication.AuthenticationService
import steam.webui.authentication.GrpcAuthenticationService
import steam.webui.clientcomm.ClientCommService
import steam.webui.clientcomm.GrpcClientCommService
import steam.webui.cloudconfigstore.CloudConfigStoreService
import steam.webui.cloudconfigstore.GrpcCloudConfigStoreService
import steam.webui.community.CommunityService
import steam.webui.community.GrpcCommunityService
import steam.webui.econ.EconService
import steam.webui.econ.GrpcEconService
import steam.webui.familygroups.FamilyGroupsService
import steam.webui.familygroups.GrpcFamilyGroupsService
import steam.webui.friendslist.FriendsListService
import steam.webui.friendslist.GrpcFriendsListService
import steam.webui.gamenotes.GameNotesService
import steam.webui.gamenotes.GrpcGameNotesService
import steam.webui.mobiledevice.GrpcMobileDeviceService
import steam.webui.mobiledevice.MobileDeviceService
import steam.webui.player.GrpcPlayerService
import steam.webui.player.PlayerService
import steam.webui.publishedfile.GrpcPublishedFileService
import steam.webui.publishedfile.PublishedFileService
import steam.webui.steamnotification.GrpcSteamNotificationService
import steam.webui.steamnotification.SteamNotificationService
import steam.webui.store.GrpcStoreService
import steam.webui.store.StoreService
import steam.webui.storebrowse.GrpcStoreBrowseService
import steam.webui.storebrowse.StoreBrowseService
import steam.webui.twofactor.GrpcTwoFactorService
import steam.webui.twofactor.TwoFactorService
import steam.webui.useraccount.GrpcUserAccountService
import steam.webui.useraccount.UserAccountService
import steam.webui.usergameactivity.GrpcUserGameActivityService
import steam.webui.usergameactivity.UserGameActivityService
import steam.webui.usergamenotes.GrpcUserGameNotesService
import steam.webui.usergamenotes.UserGameNotesService
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
    val clientComm: ClientCommService
    val cloudConfigStore: CloudConfigStoreService
    val community: CommunityService
    val econ: EconService
    val familyGroups: FamilyGroupsService
    val friendsList: FriendsListService
    val gameNotes: GameNotesService
    val mobileDevice: MobileDeviceService
    val player: PlayerService
    val publishedFile: PublishedFileService
    val steamNotification: SteamNotificationService
    val store: StoreService
    val storeBrowse: StoreBrowseService
    val twoFactor: TwoFactorService
    val userAccount: UserAccountService
    val userGameActivity: UserGameActivityService
    val userGameNotes: UserGameNotesService
    val userNews: UserNewsService
}

internal class SteamGrpcClientsImpl(
    unifiedMessages: UnifiedMessages
) : SteamGrpcClients {
    override val authentication = GrpcAuthenticationService(unifiedMessages)
    override val clientComm = GrpcClientCommService(unifiedMessages)
    override val cloudConfigStore = GrpcCloudConfigStoreService(unifiedMessages)
    override val community = GrpcCommunityService(unifiedMessages)
    override val econ = GrpcEconService(unifiedMessages)
    override val familyGroups = GrpcFamilyGroupsService(unifiedMessages)
    override val friendsList = GrpcFriendsListService(unifiedMessages)
    override val gameNotes = GrpcGameNotesService(unifiedMessages)
    override val mobileDevice = GrpcMobileDeviceService(unifiedMessages)
    override val player = GrpcPlayerService(unifiedMessages)
    override val publishedFile = GrpcPublishedFileService(unifiedMessages)
    override val steamNotification = GrpcSteamNotificationService(unifiedMessages)
    override val store = GrpcStoreService(unifiedMessages)
    override val storeBrowse = GrpcStoreBrowseService(unifiedMessages)
    override val twoFactor = GrpcTwoFactorService(unifiedMessages)
    override val userAccount = GrpcUserAccountService(unifiedMessages)
    override val userGameActivity = GrpcUserGameActivityService(unifiedMessages)
    override val userGameNotes = GrpcUserGameNotesService(unifiedMessages)
    override val userNews = GrpcUserNewsService(unifiedMessages)
}