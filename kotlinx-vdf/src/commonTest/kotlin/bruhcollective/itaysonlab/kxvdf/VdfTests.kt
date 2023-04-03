package bruhcollective.itaysonlab.kxvdf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okio.Buffer
import okio.ByteString.Companion.decodeHex
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class VdfTests {
    private val openVdf = Vdf {
        ignoreUnknownKeys = true
    }

    @Test
    fun `Decodes simple VDF`() {
        assertEquals("Hello!", Vdf.decodeFromString<SimpleVdf>(
            """
"rootnode"
{
	"value"		"Hello!"
}
            """.trimIndent()
        ).rootnode.value)
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
            assertEquals(727, appinfo.appid)
            assertEquals("Game Name", appinfo.common.name)
            assertEquals("Game", appinfo.common.type)
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
            assertEquals("Hello!", rootnode.value)
        }
    }

    //

    @Test
    fun `Encodes complex VDF`() {
        assertEquals(actual = Vdf.encodeToString<AppInfo>(AppInfo(
            appinfo = AppInfo.AppInfoRootNode(
                appid = 727,
                common = AppInfo.AppInfoRootNode.CommonNode(
                    name = "Game Name",
                    type = "Game"
                )
            )
        )), expected = """
"appinfo"
{
	"appid"		"727"
	"common"
	{
		"name"		"Game Name"
		"type"		"Game"
	}
}

            """.trimIndent())
    }

    @Test
    fun `Decodes binary VDF`() {
        Vdf {
            binaryFormat = true
            ignoreUnknownKeys = true
            readFirstInt = true
        }.decodeFromBufferedSource<PackageInfo>(deserializer = RootNodeSkipperDeserializationStrategy(), "010000000032383038303000027061636b616765696400e04804000262696c6c696e6774797065000a000000026c6963656e736574797065000100000002737461747573000000000000657874656e6465640001616c6c6f7763726f7373726567696f6e74726164696e67616e6467696674696e670066616c736500080061707069647300023000fe7b0d0008006465706f7469647300023000ff7b0d00023100017c0d00023200037c0d00023300057c0d00023400077c0d0008006170706974656d7300080808".decodeHex().let {
            Buffer().also { b -> b.write(it) }
        }).apply {
            assertEquals(280800, packageid)
            assertEquals(10, billingtype)
            assertEquals(1, licensetype)
            assertEquals(0, status)
            assertEquals(883710, appids.first())
        }
    }

    //

    @Serializable
    data class PackageInfo(
        val packageid: Int,
        val billingtype: Int,
        val licensetype: Int,
        val status: Int,
        val extended: PackageInfoExtended,
        val appids: List<Int>,
        val depotids: List<Int>
    ) {
        @Serializable
        data class PackageInfoExtended(
            val allowcrossregiontradingandgifting: Boolean
        )
    }

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