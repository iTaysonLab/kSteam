package bruhcollective.itaysonlab.ksteam.guard

import bruhcollective.itaysonlab.ksteam.guard.models.ConfirmationListState
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

object MobileConfirmSerializer: JsonContentPolymorphicSerializer<ConfirmationListState>(ConfirmationListState::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out ConfirmationListState> {
        return when (val type = element.jsonObject["success"]?.jsonPrimitive?.booleanOrNull) {
            false -> serializer<ConfirmationListState.Error>()
            true -> serializer<ConfirmationListState.Success>()
            else -> error("success boolean is null? = $type")
        }
    }
}