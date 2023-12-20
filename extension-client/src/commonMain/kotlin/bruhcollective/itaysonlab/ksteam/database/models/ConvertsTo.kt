package bruhcollective.itaysonlab.ksteam.database.models

internal interface ConvertsTo <T> {
    fun convert(): T
}