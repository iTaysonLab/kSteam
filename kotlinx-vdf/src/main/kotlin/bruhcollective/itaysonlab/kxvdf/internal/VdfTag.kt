package bruhcollective.itaysonlab.kxvdf.internal

sealed class VdfTag {
    class NodeStart (val name: String): VdfTag()
    class NodeElement (val name: String, val value: String): VdfTag()
    class NodeEnd (val name: String): VdfTag()
    object EndOfFile: VdfTag()
}