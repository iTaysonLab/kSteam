package bruhcollective.itaysonlab.ksteam.platform

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
actual annotation class Immutable actual constructor()

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.SOURCE)
actual annotation class Stable actual constructor()