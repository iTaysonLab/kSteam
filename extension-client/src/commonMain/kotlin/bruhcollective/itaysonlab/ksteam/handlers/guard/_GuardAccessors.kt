package bruhcollective.itaysonlab.ksteam.handlers.guard

import bruhcollective.itaysonlab.ksteam.SteamClient

inline val SteamClient.guard get() = getHandler<Guard>()
inline val SteamClient.guardManagement get() = getHandler<GuardManagement>()
inline val SteamClient.guardConfirmation get() = getHandler<GuardConfirmation>()