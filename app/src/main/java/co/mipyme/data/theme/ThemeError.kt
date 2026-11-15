package co.mipyme.data.theme

sealed interface ThemeError {
    data object PersistFailed : ThemeError
}
