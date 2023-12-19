package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.internal.CloudConfiguration
import bruhcollective.itaysonlab.ksteam.handlers.library.Pics

inline val SteamClient.library get() = getHandler<Library>()
inline val SteamClient.pics get() = getHandler<Pics>()

internal inline val SteamClient.cloudConfiguration get() = getHandler<CloudConfiguration>()