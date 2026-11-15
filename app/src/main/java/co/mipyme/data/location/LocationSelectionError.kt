package co.mipyme.data.location

sealed interface LocationSelectionError {
    data object NotAccessible : LocationSelectionError

    data object PersistFailed : LocationSelectionError
}
