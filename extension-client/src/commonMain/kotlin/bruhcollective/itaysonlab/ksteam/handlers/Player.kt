package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.SteamClient
import bruhcollective.itaysonlab.ksteam.messages.SteamPacket
import bruhcollective.itaysonlab.ksteam.models.SteamId
import bruhcollective.itaysonlab.ksteam.models.library.OwnedGame
import steam.webui.player.CPlayer_GetOwnedGames_Request
import steam.webui.player.CPlayer_GetOwnedGames_Response
import steam.webui.player.CPlayer_GetPlayNext_Request
import steam.webui.player.CPlayer_GetPlayNext_Response
import kotlin.jvm.JvmInline

/**
 * Access Player information using this handler.
 */
class Player internal constructor(
    private val steamClient: SteamClient
) : BaseHandler {
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

        return steamClient.unifiedMessages.execute(
            methodName = "Player.GetOwnedGames",
            requestAdapter = CPlayer_GetOwnedGames_Request.ADAPTER,
            responseAdapter = CPlayer_GetOwnedGames_Response.ADAPTER,
            requestData = CPlayer_GetOwnedGames_Request(
                steamid = steamId.longId,
                include_appinfo = true,
                include_extended_appinfo = true,
                include_free_sub = includeFreeGames,
                include_played_free_games = includeFreeGames
            )
        ).dataNullable?.games?.map(::OwnedGame).orEmpty().also { libraryCache[steamId] = it }
    }

    /**
     * Requests a list of apps to show in "Play Next" shelf
     */
    suspend fun getPlayNextQueue(): List<Int> {
        return steamClient.unifiedMessages.execute(
            methodName = "Player.GetPlayNext",
            requestAdapter = CPlayer_GetPlayNext_Request.ADAPTER,
            responseAdapter = CPlayer_GetPlayNext_Response.ADAPTER,
            requestData = CPlayer_GetPlayNext_Request()
        ).dataNullable?.appids.orEmpty()
    }

    @JvmInline
    value class Library(private val packed: Pair<Int, List<OwnedGame>>) {
        val count get() = packed.first
        val list get() = packed.second
    }

    override suspend fun onEvent(packet: SteamPacket) = Unit
}