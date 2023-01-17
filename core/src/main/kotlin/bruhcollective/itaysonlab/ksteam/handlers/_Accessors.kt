package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient

val SteamClient.account get() = getHandler<Account>()
val SteamClient.notifications get() = getHandler<Notifications>()
val SteamClient.persona get() = getHandler<Persona>()
val SteamClient.store get() = getHandler<Store>()
val SteamClient.webApi get() = getHandler<WebApi>()
val SteamClient.library get() = getHandler<Library>()

//

internal val SteamClient.storage get() = getHandler<Storage>()