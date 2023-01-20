package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient

inline val SteamClient.account get() = getHandler<Account>()
inline val SteamClient.notifications get() = getHandler<Notifications>()
inline val SteamClient.persona get() = getHandler<Persona>()
inline val SteamClient.store get() = getHandler<Store>()
inline val SteamClient.webApi get() = getHandler<WebApi>()
inline val SteamClient.library get() = getHandler<Library>()

//

internal inline val SteamClient.storage get() = getHandler<Storage>()
internal inline val SteamClient.sentry get() = getHandler<Sentry>()