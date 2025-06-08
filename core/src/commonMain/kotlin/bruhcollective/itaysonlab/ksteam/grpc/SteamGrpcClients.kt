package bruhcollective.itaysonlab.ksteam.grpc

import bruhcollective.itaysonlab.ksteam.handlers.UnifiedMessages
import steam.webui.accountcart.AccountCartService
import steam.webui.accountcart.GrpcAccountCartService
import steam.webui.accountlinking.AccountLinkingService
import steam.webui.accountlinking.GrpcAccountLinkingService
import steam.webui.accountprivacy.AccountPrivacyService
import steam.webui.accountprivacy.GrpcAccountPrivacyService
import steam.webui.accountprivateapps.AccountPrivateAppsService
import steam.webui.accountprivateapps.GrpcAccountPrivateAppsService
import steam.webui.achievements.AchievementsService
import steam.webui.achievements.GrpcAchievementsService
import steam.webui.assetsetpublishing.AssetSetPublishingService
import steam.webui.assetsetpublishing.GrpcAssetSetPublishingService
import steam.webui.auction.AuctionService
import steam.webui.auction.GrpcAuctionService
import steam.webui.authentication.AuthenticationService
import steam.webui.authentication.GrpcAuthenticationService
import steam.webui.authenticationsupport.AuthenticationSupportService
import steam.webui.authenticationsupport.GrpcAuthenticationSupportService
import steam.webui.broadcast.BroadcastService
import steam.webui.broadcast.GrpcBroadcastService
import steam.webui.chat.ChatService
import steam.webui.chat.GrpcChatService
import steam.webui.chatroom.ChatRoomService
import steam.webui.chatroom.GrpcChatRoomService
import steam.webui.chatusability.ChatUsabilityService
import steam.webui.chatusability.GrpcChatUsabilityService
import steam.webui.checkout.CheckoutService
import steam.webui.checkout.GrpcCheckoutService
import steam.webui.clan.ClanService
import steam.webui.clan.GrpcClanService
import steam.webui.clanfaqs.ClanFAQSService
import steam.webui.clanfaqs.GrpcClanFAQSService
import steam.webui.clientcomm.ClientCommService
import steam.webui.clientcomm.GrpcClientCommService
import steam.webui.clientmetrics.ClientMetricsService
import steam.webui.clientmetrics.GrpcClientMetricsService
import steam.webui.cloud.CloudService
import steam.webui.cloud.GrpcCloudService
import steam.webui.cloudconfigstore.CloudConfigStoreService
import steam.webui.cloudconfigstore.GrpcCloudConfigStoreService
import steam.webui.cloudgaming.CloudGamingService
import steam.webui.cloudgaming.GrpcCloudGamingService
import steam.webui.community.CommunityService
import steam.webui.community.GrpcCommunityService
import steam.webui.dailydeal.DailyDealService
import steam.webui.dailydeal.GrpcDailyDealService
import steam.webui.econ.EconService
import steam.webui.econ.GrpcEconService
import steam.webui.familygroups.FamilyGroupsService
import steam.webui.familygroups.GrpcFamilyGroupsService
import steam.webui.forums.ForumsService
import steam.webui.forums.GrpcForumsService
import steam.webui.friendmessages.FriendMessagesService
import steam.webui.friendmessages.GrpcFriendMessagesService
import steam.webui.friendslist.FriendsListService
import steam.webui.friendslist.GrpcFriendsListService
import steam.webui.gamenotes.GameNotesService
import steam.webui.gamenotes.GrpcGameNotesService
import steam.webui.gamerecordingclip.GameRecordingClipService
import steam.webui.gamerecordingclip.GrpcGameRecordingClipService
import steam.webui.loyaltyrewards.GrpcLoyaltyRewardsService
import steam.webui.loyaltyrewards.LoyaltyRewardsService
import steam.webui.marketingmessages.GrpcMarketingMessagesService
import steam.webui.marketingmessages.MarketingMessagesService
import steam.webui.mobileapp.GrpcMobileAppService
import steam.webui.mobileapp.MobileAppService
import steam.webui.mobileauth.GrpcMobileAuthService
import steam.webui.mobileauth.MobileAuthService
import steam.webui.mobiledevice.GrpcMobileDeviceService
import steam.webui.mobiledevice.MobileDeviceService
import steam.webui.mobileperaccount.GrpcMobilePerAccountService
import steam.webui.mobileperaccount.MobilePerAccountService
import steam.webui.news.GrpcNewsService
import steam.webui.news.NewsService
import steam.webui.parental.GrpcParentalService
import steam.webui.parental.ParentalService
import steam.webui.phone.GrpcPhoneService
import steam.webui.phone.PhoneService
import steam.webui.physicalgoods.GrpcPhysicalGoodsService
import steam.webui.physicalgoods.PhysicalGoodsService
import steam.webui.player.GrpcPlayerService
import steam.webui.player.PlayerService
import steam.webui.playtest.GrpcPlaytestService
import steam.webui.playtest.PlaytestService
import steam.webui.publishedfile.GrpcPublishedFileService
import steam.webui.publishedfile.PublishedFileService
import steam.webui.quest.GrpcQuestService
import steam.webui.quest.QuestService
import steam.webui.salefeature.GrpcSaleFeatureService
import steam.webui.salefeature.SaleFeatureService
import steam.webui.saleitemrewards.GrpcSaleItemRewardsService
import steam.webui.saleitemrewards.SaleItemRewardsService
import steam.webui.shoppingcart.GrpcShoppingCartService
import steam.webui.shoppingcart.ShoppingCartService
import steam.webui.steamawards.GrpcSteamAwardsService
import steam.webui.steamawards.SteamAwardsService
import steam.webui.steamcharts.GrpcSteamChartsService
import steam.webui.steamcharts.SteamChartsService
import steam.webui.steamnotification.GrpcSteamNotificationService
import steam.webui.steamnotification.SteamNotificationService
import steam.webui.steamtv.GrpcSteamTVService
import steam.webui.steamtv.SteamTVService
import steam.webui.store.GrpcStoreService
import steam.webui.store.StoreService
import steam.webui.storeappsimilarity.GrpcStoreAppSimilarityService
import steam.webui.storeappsimilarity.StoreAppSimilarityService
import steam.webui.storebrowse.GrpcStoreBrowseService
import steam.webui.storebrowse.StoreBrowseService
import steam.webui.storecatalog.GrpcStoreCatalogService
import steam.webui.storecatalog.StoreCatalogService
import steam.webui.storemarketing.GrpcStoreMarketingService
import steam.webui.storemarketing.StoreMarketingService
import steam.webui.storequery.GrpcStoreQueryService
import steam.webui.storequery.StoreQueryService
import steam.webui.storesales.GrpcStoreSalesService
import steam.webui.storesales.StoreSalesService
import steam.webui.storetopsellers.GrpcStoreTopSellersService
import steam.webui.storetopsellers.StoreTopSellersService
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
import steam.webui.userreviews.GrpcUserReviewsService
import steam.webui.userreviews.UserReviewsService
import steam.webui.userstorevisit.GrpcUserStoreVisitService
import steam.webui.userstorevisit.UserStoreVisitService
import steam.webui.video.GrpcVideoService
import steam.webui.video.VideoService
import steam.webui.videoclip.GrpcVideoClipService
import steam.webui.videoclip.VideoClipService
import steam.webui.voicechat.GrpcVoiceChatService
import steam.webui.voicechat.VoiceChatService
import steam.webui.wishlist.GrpcWishlistService
import steam.webui.wishlist.WishlistService

/**
 * Provides auto-generated gRPC clients for Steam Network services.
 * 
 * Please note that partner (for admins and develop) services are not provided due to unavailability of them when using a regular account.
 * Also, client-site services (SteamOS-related most time) are also not included because they are not to be called on a remote server.
 *
 * These methods accept and return raw Protobuf objects and are meant to be called with [bruhcollective.itaysonlab.ksteam.util.executeSteam].
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
    val accountCart: AccountCartService
    val accountLinking: AccountLinkingService
    val accountPrivacy: AccountPrivacyService
    val accountPrivateApps: AccountPrivateAppsService
    val achievements: AchievementsService
    val assetSetPublishing: AssetSetPublishingService
    val auction: AuctionService
    val authentication: AuthenticationService
    val authenticationSupport: AuthenticationSupportService
    val broadcast: BroadcastService
    val chat: ChatService
    val chatRoom: ChatRoomService
    val chatUsability: ChatUsabilityService
    val checkout: CheckoutService
    val clan: ClanService
    val clanChatRooms: ChatRoomService
    val clanFaqs: ClanFAQSService
    val clientComm: ClientCommService
    val clientMetrics: ClientMetricsService
    val cloud: CloudService
    val cloudConfigStore: CloudConfigStoreService
    val cloudGaming: CloudGamingService
    val community: CommunityService
    val dailyDeal: DailyDealService
    val econ: EconService
    val familyGroups: FamilyGroupsService
    val forums: ForumsService
    val friendMessages: FriendMessagesService
    val friendsList: FriendsListService
    val gameNotes: GameNotesService
    val gameRecordingClip: GameRecordingClipService
    val loyaltyRewards: LoyaltyRewardsService
    val marketingMessages: MarketingMessagesService
    val mobileApp: MobileAppService
    val mobileAuth: MobileAuthService
    val mobileDevice: MobileDeviceService
    val mobilePerAccount: MobilePerAccountService
    val news: NewsService
    val parental: ParentalService
    val phone: PhoneService
    val physicalGoods: PhysicalGoodsService
    val player: PlayerService
    val playtest: PlaytestService
    val publishedFile: PublishedFileService
    val quest: QuestService
    val saleFeature: SaleFeatureService
    val saleItemRewards: SaleItemRewardsService
    val shoppingCart: ShoppingCartService
    val steamAwards: SteamAwardsService
    val steamCharts: SteamChartsService
    val steamNotification: SteamNotificationService
    val steamTV: SteamTVService
    val store: StoreService
    val storeAppsSimilarity: StoreAppSimilarityService
    val storeBrowse: StoreBrowseService
    val storeCatalog: StoreCatalogService
    val storeMarketing: StoreMarketingService
    val storeQuery: StoreQueryService
    val storeSales: StoreSalesService
    val storeTopSellers: StoreTopSellersService
    val twoFactor: TwoFactorService
    val userAccount: UserAccountService
    val userGameActivity: UserGameActivityService
    val userGameNotes: UserGameNotesService
    val userNews: UserNewsService
    val userReviews: UserReviewsService
    val userStoreVisit: UserStoreVisitService
    val video: VideoService
    val videoClip: VideoClipService
    val voiceChat: VoiceChatService
    val wishlist: WishlistService
}

internal class SteamGrpcClientsImpl(
    unifiedMessages: UnifiedMessages
) : SteamGrpcClients {
    override val accountCart = GrpcAccountCartService(unifiedMessages)
    override val accountLinking: AccountLinkingService = GrpcAccountLinkingService(unifiedMessages)
    override val accountPrivacy: AccountPrivacyService = GrpcAccountPrivacyService(unifiedMessages)
    override val accountPrivateApps: AccountPrivateAppsService = GrpcAccountPrivateAppsService(unifiedMessages)
    override val achievements = GrpcAchievementsService(unifiedMessages)
    override val assetSetPublishing: AssetSetPublishingService = GrpcAssetSetPublishingService(unifiedMessages)
    override val auction: AuctionService = GrpcAuctionService(unifiedMessages)
    override val authentication = GrpcAuthenticationService(unifiedMessages)
    override val authenticationSupport: AuthenticationSupportService = GrpcAuthenticationSupportService(unifiedMessages)
    override val broadcast: BroadcastService = GrpcBroadcastService(unifiedMessages)
    override val chat: ChatService = GrpcChatService(unifiedMessages)
    override val chatRoom: ChatRoomService = GrpcChatRoomService(unifiedMessages)
    override val chatUsability: ChatUsabilityService = GrpcChatUsabilityService(unifiedMessages)
    override val checkout: CheckoutService = GrpcCheckoutService(unifiedMessages)
    override val clan: ClanService = GrpcClanService(unifiedMessages)
    override val clanChatRooms: ChatRoomService = GrpcChatRoomService(unifiedMessages)
    override val clanFaqs: ClanFAQSService = GrpcClanFAQSService(unifiedMessages)
    override val clientComm = GrpcClientCommService(unifiedMessages)
    override val clientMetrics: ClientMetricsService = GrpcClientMetricsService(unifiedMessages)
    override val cloud: CloudService = GrpcCloudService(unifiedMessages)
    override val cloudConfigStore = GrpcCloudConfigStoreService(unifiedMessages)
    override val cloudGaming: CloudGamingService = GrpcCloudGamingService(unifiedMessages)
    override val community = GrpcCommunityService(unifiedMessages)
    override val dailyDeal: DailyDealService = GrpcDailyDealService(unifiedMessages)
    override val econ = GrpcEconService(unifiedMessages)
    override val familyGroups = GrpcFamilyGroupsService(unifiedMessages)
    override val forums: ForumsService = GrpcForumsService(unifiedMessages)
    override val friendMessages: FriendMessagesService = GrpcFriendMessagesService(unifiedMessages)
    override val friendsList = GrpcFriendsListService(unifiedMessages)
    override val gameNotes = GrpcGameNotesService(unifiedMessages)
    override val gameRecordingClip: GameRecordingClipService = GrpcGameRecordingClipService(unifiedMessages)
    override val loyaltyRewards: LoyaltyRewardsService = GrpcLoyaltyRewardsService(unifiedMessages)
    override val marketingMessages: MarketingMessagesService = GrpcMarketingMessagesService(unifiedMessages)
    override val mobileApp: MobileAppService = GrpcMobileAppService(unifiedMessages)
    override val mobileAuth: MobileAuthService = GrpcMobileAuthService(unifiedMessages)
    override val mobileDevice = GrpcMobileDeviceService(unifiedMessages)
    override val mobilePerAccount: MobilePerAccountService = GrpcMobilePerAccountService(unifiedMessages)
    override val news: NewsService = GrpcNewsService(unifiedMessages)
    override val parental: ParentalService = GrpcParentalService(unifiedMessages)
    override val phone: PhoneService = GrpcPhoneService(unifiedMessages)
    override val physicalGoods: PhysicalGoodsService = GrpcPhysicalGoodsService(unifiedMessages)
    override val player = GrpcPlayerService(unifiedMessages)
    override val playtest: PlaytestService = GrpcPlaytestService(unifiedMessages)
    override val publishedFile = GrpcPublishedFileService(unifiedMessages)
    override val quest: QuestService = GrpcQuestService(unifiedMessages)
    override val saleFeature: SaleFeatureService = GrpcSaleFeatureService(unifiedMessages)
    override val saleItemRewards: SaleItemRewardsService = GrpcSaleItemRewardsService(unifiedMessages)
    override val shoppingCart: ShoppingCartService = GrpcShoppingCartService(unifiedMessages)
    override val steamAwards: SteamAwardsService = GrpcSteamAwardsService(unifiedMessages)
    override val steamCharts: SteamChartsService = GrpcSteamChartsService(unifiedMessages)
    override val steamNotification = GrpcSteamNotificationService(unifiedMessages)
    override val steamTV: SteamTVService = GrpcSteamTVService(unifiedMessages)
    override val store = GrpcStoreService(unifiedMessages)
    override val storeAppsSimilarity: StoreAppSimilarityService = GrpcStoreAppSimilarityService(unifiedMessages)
    override val storeBrowse = GrpcStoreBrowseService(unifiedMessages)
    override val storeCatalog: StoreCatalogService = GrpcStoreCatalogService(unifiedMessages)
    override val storeMarketing: StoreMarketingService = GrpcStoreMarketingService(unifiedMessages)
    override val storeQuery: StoreQueryService = GrpcStoreQueryService(unifiedMessages)
    override val storeSales: StoreSalesService = GrpcStoreSalesService(unifiedMessages)
    override val storeTopSellers: StoreTopSellersService = GrpcStoreTopSellersService(unifiedMessages)
    override val twoFactor = GrpcTwoFactorService(unifiedMessages)
    override val userAccount = GrpcUserAccountService(unifiedMessages)
    override val userGameActivity = GrpcUserGameActivityService(unifiedMessages)
    override val userGameNotes = GrpcUserGameNotesService(unifiedMessages)
    override val userNews = GrpcUserNewsService(unifiedMessages)
    override val userReviews = GrpcUserReviewsService(unifiedMessages)
    override val userStoreVisit: UserStoreVisitService = GrpcUserStoreVisitService(unifiedMessages)
    override val video: VideoService = GrpcVideoService(unifiedMessages)
    override val videoClip: VideoClipService = GrpcVideoClipService(unifiedMessages)
    override val voiceChat: VoiceChatService = GrpcVoiceChatService(unifiedMessages)
    override val wishlist: WishlistService = GrpcWishlistService(unifiedMessages)
}