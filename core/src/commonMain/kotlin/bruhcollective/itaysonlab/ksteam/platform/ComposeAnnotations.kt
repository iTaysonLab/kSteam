package bruhcollective.itaysonlab.ksteam.platform

@OptIn(ExperimentalMultiplatform::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
expect annotation class Immutable()

@OptIn(ExperimentalMultiplatform::class)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.BINARY)
@OptionalExpectation
expect annotation class Stable()