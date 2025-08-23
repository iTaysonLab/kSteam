package bruhcollective.itaysonlab.ksteam.handlers

import bruhcollective.itaysonlab.ksteam.ExtendedSteamClient
import bruhcollective.itaysonlab.ksteam.models.AppId
import bruhcollective.itaysonlab.ksteam.models.game_notes.GameNote
import bruhcollective.itaysonlab.ksteam.models.game_notes.GameWithNotes
import bruhcollective.itaysonlab.ksteam.util.executeSteam
import steam.webui.usergamenotes.CUserGameNote
import steam.webui.usergamenotes.CUserGameNotes_DeleteNote_Request
import steam.webui.usergamenotes.CUserGameNotes_GetGamesWithNotes_Request
import steam.webui.usergamenotes.CUserGameNotes_GetNotesForGame_Request
import kotlin.String
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Access Steam friend activity using this handler.
 */
@OptIn(ExperimentalTime::class)
class GameNotes internal constructor(
    private val steamClient: ExtendedSteamClient
) {
    suspend fun getNotes(
        app: AppId,
        shortcutName: String? = null,
        shortcutId: Int? = null,
        includeContent: Boolean = true,
    ): List<GameNote> {
        return steamClient.grpc.userGameNotes.GetNotesForGame().executeSteam(
            data = CUserGameNotes_GetNotesForGame_Request(
                appid = app.value,
                shortcut_name = shortcutName,
                shortcutid = shortcutId,
                include_content = includeContent,
            )
        ).notes.mapNotNull { obj ->
            return@mapNotNull GameNote(
                id = obj.id ?: return@mapNotNull null,
                appid = obj.appid?.let(::AppId) ?: return@mapNotNull null,
                shortcutName = obj.shortcut_name,
                shortcutId = obj.shortcutid,
                ordinal = obj.ordinal ?: 0,
                timeCreated = Instant.fromEpochSeconds(obj.time_created?.toLong() ?: 0),
                timeModified = Instant.fromEpochSeconds(obj.time_modified?.toLong() ?: 0),
                title = obj.title.orEmpty(),
                content = obj.content.orEmpty()
            )
        }
    }

    suspend fun getGamesWithNotes(): List<GameWithNotes> {
        return steamClient.grpc.userGameNotes.GetGamesWithNotes().executeSteam(
            data = CUserGameNotes_GetGamesWithNotes_Request()
        ).games_with_notes.mapNotNull { obj ->
            return@mapNotNull GameWithNotes(
                appId = obj.appid?.let(::AppId) ?: return@mapNotNull null,
                shortcutName = obj.shortcut_name,
                shortcutId = obj.shortcutid,
                timeModified = Instant.fromEpochSeconds(obj.last_modified?.toLong() ?: 0),
                noteCount = obj.note_count ?: 0
            )
        }
    }

    suspend fun deleteNote(
        app: AppId,
        shortcutName: String? = null,
        shortcutId: Int? = null,
        noteId: String
    ) {
        steamClient.grpc.userGameNotes.DeleteNote().executeSteam(
            data = CUserGameNotes_DeleteNote_Request(
                appid = app.value,
                shortcut_name = shortcutName,
                shortcutid = shortcutId,
                note_id = noteId
            )
        )
    }

    suspend fun saveNote() {
        TODO()
    }
}