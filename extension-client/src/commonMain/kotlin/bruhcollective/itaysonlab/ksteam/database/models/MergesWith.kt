package bruhcollective.itaysonlab.ksteam.database.models

internal interface MergesWith <T> {
    fun merge(with: T)
}