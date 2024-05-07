package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.library.OwnedGame
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import steam.webui.player.CPlayer_GetOwnedGames_Request
import steam.webui.player.CPlayer_GetPlayNext_Request
import kotlin.jvm.JvmInline

/**
 * Access Player information using this handler.
 */
class Player internal constructor(
    private val steamClient: ExtendedSteamClient
) {
    private val libraryCache = mutableMapOf<SteamId, List<OwnedGame>>()

    /**
     * Requests a library of a specific [steamId].
     *
     * It is not recommended to use this method to get user's library - use collections API instead.
     */
    suspend fun getOwnedGames(steamId: SteamId = steamClient.currentSessionSteamId, includeFreeGames: Boolean = false): List<OwnedGame> {
        libraryCache[steamId]?.let {
            return it
        }

        return steamClient.grpc.player.GetOwnedGames().executeSteam(
            data = CPlayer_GetOwnedGames_Request(
                steamid = steamId.longId,
                include_appinfo = true,
                include_extended_appinfo = true,
                include_free_sub = includeFreeGames,
                include_played_free_games = includeFreeGames
            )
        ).games.map(::OwnedGame).also { libraryCache[steamId] = it }
    }

    /**
     * Requests a list of apps to show in "Play Next" shelf
     */
    suspend fun getPlayNextQueue(): List<Int> {
        return steamClient.grpc.player.GetPlayNext().executeSteam(
            data = CPlayer_GetPlayNext_Request()
        ).appids
    }

    @JvmInline
    value class Library(private val packed: Pair<Int, List<OwnedGame>>) {
        val count get() = packed.first
        val list get() = packed.second
    }
}