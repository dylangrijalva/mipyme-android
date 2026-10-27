package co.mipyme.contract

sealed interface OptionalField<out T> {
    data object Absent : OptionalField<Nothing>

    data class Present<T>(
        val value: T,
    ) : OptionalField<T>
}
