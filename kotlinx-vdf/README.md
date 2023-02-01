## kotlinx-vdf
A KotlinX Serialization module which (de)serializes Valve Data Format (VDF) files.

Supported formats:
- Text VDFs (read/write)
- Binary VDFs (read)

Supported features:
- Lists ("0" - A, "1" - B)
- Typed values (nested classes, (u)int32/64 [binary])
- Okio read/write from/into BufferedSink/BufferedSource

TODO:
- Binary VDF encoding

### Usage

```kotlin
// Define our classes
@Serializable
data class MyVdf(
    private val rootNode: MyVdfContents
)

@Serializable
data class MyVdfContents(
    private val hello: String
)

// Create VDF instance
val vdf = Vdf {
    // Use defaults when encoding
    encodeDefaults = false
    // Ignore unknown keys
    ignoreUnknownKeys = false
    // Read/write in encoded format
    binaryFormat = false
    // [Binary/Read] Consume first int for packageinfo parsing
    readFirstInt = false
}

// MyVdf(rootNode=MyVdfContents(hello="World"))
vdf.decodeFromString<MyVdf>("""
"rootNode"
{
    "hello"     "World"
}
""")

// As simpler way, you can skip the root node
// MyVdfContents(hello="skipped")
vdf.decodeFromString<MyVdfContents>(RootNodeSkipperDeserializationStrategy(), """
"rootNode"
{
    "hello"     "skipped"
}
""")
```