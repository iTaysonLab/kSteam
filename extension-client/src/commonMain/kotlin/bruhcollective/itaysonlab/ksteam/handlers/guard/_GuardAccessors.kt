package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.handlers.guard.Guard
import bruhcollective.itaysonlab.ksteam.handlers.guard.GuardConfirmation

inline val SteamClient.guard get() = getHandler<Guard>()
inline val SteamClient.guardManagement get() = getHandler<GuardManagement>()
inline val SteamClient.guardConfirmation get() = getHandler<GuardConfirmation>()