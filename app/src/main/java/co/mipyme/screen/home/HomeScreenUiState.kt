package co.mipyme.screen.home

data class HomeScreenUiState(
    val email: String? = null,
    val locationName: String? = null,
    val visibleDestinations: List<HomeDestination> = emptyList(),
    val backStack: List<HomeDestination> = emptyList(),
    val pendingLostAccessDestination: HomeDestination? = null,
)
