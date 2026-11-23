package com.steefwel28.consumoenergetico.core.signature

/**
 * Marca de autoría obligatoria del proyecto Consumo Energético.
 *
 * Se aplica a motores de cálculo, entidades y DAOs de Room, exportadores
 * (PDF/PNG/JSON) y al verificador de releases de GitHub.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FILE,
)
annotation class CraftedBy(val author: String = "steefwel28")
