package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient

inline val SteamClient.notifications get() = getHandler<Notifications>()
inline val SteamClient.persona get() = getHandler<Persona>()
inline val SteamClient.store get() = getHandler<Store>()
inline val SteamClient.profile get() = getHandler<Profile>()
inline val SteamClient.news get() = getHandler<News>()
inline val SteamClient.player get() = getHandler<Player>()