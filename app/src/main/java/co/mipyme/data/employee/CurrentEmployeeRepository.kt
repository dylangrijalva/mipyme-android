package co.mipyme.data.employee

import co.mipyme.auth.AuthService
import co.mipyme.auth.AuthState
import co.mipyme.contract.Employee
import co.mipyme.data.location.locationUserId
import co.mipyme.database.MipymeCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentEmployeeRepository(
    private val authService: AuthService,
    private val cache: MipymeCache,
    scope: CoroutineScope,
    private val employeeOverride: MutableStateFlow<EmployeeOverride?> = MutableStateFlow(null),
) {
    val currentEmployeeFlow: Flow<Employee?> = combine(
        employeeOverride,
        authService.authStateFlow,
    ) { override, authState ->
        val accountId = (authState as? AuthState.Authenticated)?.account?.id
        override?.takeIf { it.accountId == accountId }?.employeeId ?: authState.locationUserId
    }
        .distinctUntilChanged()
        .flatMapLatest { employeeId ->
            if (employeeId == null) flowOf(null) else cache.observeEmployee(employeeId)
        }
        .distinctUntilChanged()

    init {
        scope.launch {
            authService.authStateFlow
                .filter { it != AuthState.Initializing }
                .map { (it as? AuthState.Authenticated)?.account?.id }
                .distinctUntilChanged()
                .collect { employeeOverride.value = null }
        }
    }
}
