package bruhcollective.itaysonlab.ksteam.guard

import bruhcollective.itaysonlab.ksteam.models.guard.ConfirmationListState
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

internal object MobileConfirmSerializer: JsonContentPolymorphicSerializer<ConfirmationListState>(ConfirmationListState::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ConfirmationListState> {
        return when (val type = element.jsonObject["success"]?.jsonPrimitive?.booleanOrNull) {
            false -> serializer<ConfirmationListState.Error>()
            true -> serializer<ConfirmationListState.Success>()
            else -> error("success boolean is null? = $type")
        }
    }
}