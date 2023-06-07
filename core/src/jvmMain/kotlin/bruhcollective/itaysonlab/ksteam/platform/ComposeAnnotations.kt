package bruhcollective.itaysonlab.ksteam.platform

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
actual annotation class Immutable

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.SOURCE)
actual annotation class Stable