package bruhcollective.itaysonlab.ksteam.database.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomFullPersona
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersona
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersonaRelationship
import bruhcollective.itaysonlab.ksteam.database.room.entity.persona.RoomPersonaRpKvo
import kotlinx.coroutines.flow.Flow

@Dao
internal interface RoomPersonaDao {
    @Transaction
    @Query("SELECT * FROM persona WHERE uid = :id")
    suspend fun getFullById(id: Long): RoomFullPersona?

    @Transaction
    @Query("SELECT * FROM persona WHERE uid = :id")
    fun getFullFlowById(id: Long): Flow<RoomFullPersona?>

    @Upsert
    suspend fun upsertPersonas(persona: List<RoomPersona>)

    @Upsert
    suspend fun upsertPersonaRelationships(relations: List<RoomPersonaRelationship>)

    @Upsert
    suspend fun upsertPersonaRpKvos(kvos: List<RoomPersonaRpKvo>)

    @Query("UPDATE persona_relationship SET relationship = 0")
    suspend fun resetRelationships()

    @Transaction
    suspend fun updateRelationships(list: List<RoomPersonaRelationship>) {
        resetRelationships()
        upsertPersonaRelationships(list)
    }

    @Query("DELETE FROM persona_rp_kvo WHERE uid = :uid")
    suspend fun resetRpKvos(uid: Long): Int
}