package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.guard.Guard
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardConfirmation
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardManagement
import bruhcollective.itaysonlab.ksteam.handlers.library.CloudConfiguration
import bruhcollective.itaysonlab.ksteam.handlers.library.Library
import bruhcollective.itaysonlab.ksteam.handlers.library.Pics

// Core
inline val SteamClient.notifications get() = getHandler<Notifications>()
inline val SteamClient.persona get() = getHandler<Persona>()
inline val SteamClient.store get() = getHandler<Store>()
inline val SteamClient.profile get() = getHandler<Profile>()
inline val SteamClient.news get() = getHandler<News>()
inline val SteamClient.userNews get() = getHandler<UserNews>()
inline val SteamClient.player get() = getHandler<Player>()
inline val SteamClient.publishedFiles get() = getHandler<PublishedFiles>()

// Pics
inline val SteamClient.library get() = getHandler<Library>()
inline val SteamClient.pics get() = getHandler<Pics>()
internal inline val SteamClient.cloudConfiguration get() = getHandler<CloudConfiguration>()

// Guard
inline val SteamClient.guard get() = getHandler<Guard>()
inline val SteamClient.guardManagement get() = getHandler<GuardManagement>()
inline val SteamClient.guardConfirmation get() = getHandler<GuardConfirmation>()