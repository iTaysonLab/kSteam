package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.internal.Sentry
import bruhcollective.itaysonlab.ksteam.handlers.internal.Storage

inline val SteamClient.account get() = getHandler<Account>()
inline val SteamClient.unifiedMessages get() = getHandler<UnifiedMessages>()

inline val SteamClient.storage get() = getHandler<Storage>()
inline val SteamClient.configuration get() = getHandler<Configuration>()

internal inline val SteamClient.sentry get() = getHandler<Sentry>()