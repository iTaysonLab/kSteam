package bruhcollective.itaysonlab.kxvdf

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class VdfTests {
    private val openVdf = Vdf {
        ignoreUnknownKeys = true
    }

    @Test
    fun `Decodes simple VDF`() {
        assertEquals(Vdf.decodeFromString<SimpleVdf>(
            """
"rootnode"
{
	"value"		"Hello!"
}
            """.trimIndent()
        ).rootnode.value, "Hello!")
    }

    @Test
    fun `Decodes complex VDF`() {
        Vdf.decodeFromString<AppInfo>(
            """
"appinfo"
{
	"appid"		"727"
	"common"
	{
		"name"		"Game Name"
		"type"		"Game"
	}
}
            """.trimIndent()
        ).apply {
            assertEquals(appinfo.appid, 727)
            assertEquals(appinfo.common.name, "Game Name")
            assertEquals(appinfo.common.type, "Game")
        }
    }

    @Test
    fun `Properly skips unknown values`() {
        openVdf.decodeFromString<SimpleVdf>(
            """
"rootnode"
{
    "skip"		"1"
    "skipClass"
    {
    		"data"		"123"
    		"data2"		"456"
    }
	"value"		"Hello!"
}
            """.trimIndent()
        ).apply {
            assertEquals(rootnode.value, "Hello!")
        }
    }

    //

    @Serializable
    data class SimpleVdf (
        val rootnode: SimpleVdfRootNode
    ) {
        @Serializable
        data class SimpleVdfRootNode (
            val value: String
        )
    }

    @Serializable
    data class AppInfo (
        val appinfo: AppInfoRootNode
    ) {
        @Serializable
        data class AppInfoRootNode (
            val appid: Int,
            val common: CommonNode
        ) {
            @Serializable
            data class CommonNode (
                val name: String,
                val type: String
            )
        }
    }
}