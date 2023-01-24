package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.guard.Guard
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardConfirmation
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardManagement
import bruhcollective.itaysonlab.ksteam.handlers.internal.CloudConfiguration
import bruhcollective.itaysonlab.ksteam.handlers.internal.Sentry

inline val SteamClient.account get() = getHandler<Account>()
inline val SteamClient.notifications get() = getHandler<Notifications>()
inline val SteamClient.persona get() = getHandler<Persona>()
inline val SteamClient.store get() = getHandler<Store>()
inline val SteamClient.webApi get() = getHandler<WebApi>()
inline val SteamClient.library get() = getHandler<Library>()

//

inline val SteamClient.guard get() = getHandler<Guard>()
inline val SteamClient.guardManagement get() = getHandler<GuardManagement>()
inline val SteamClient.guardConfiguration get() = getHandler<GuardConfirmation>()

//

internal inline val SteamClient.storage get() = getHandler<Storage>()
internal inline val SteamClient.sentry get() = getHandler<Sentry>()
internal inline val SteamClient.cloudConfiguration get() = getHandler<CloudConfiguration>()