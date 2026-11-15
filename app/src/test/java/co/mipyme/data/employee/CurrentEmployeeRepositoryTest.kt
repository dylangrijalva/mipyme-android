package co.mipyme.data.employee

import co.mipyme.auth.AuthState
import co.mipyme.contract.Employee
import co.mipyme.contract.EmployeeStatus
import co.mipyme.contract.IdentityAccount
import co.mipyme.contract.IdentityPublicMetadata
import co.mipyme.contract.PosPermission
import co.mipyme.testing.FakeAuthService
import co.mipyme.testing.FakeMipymeCache
import co.mipyme.testing.syncEmployee
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrentEmployeeRepositoryTest {

    private val cashier = syncEmployee("emp_1", emptyList(), permissions = listOf(PosPermission.CHECKOUT))
    private val manager = syncEmployee("emp_2", emptyList(), permissions = listOf(PosPermission.MANAGE_CATALOG))

    @Test
    fun `the signed in employee is the current employee`() = runTest {
        val repository = repository(FakeAuthService(authenticated()), FakeMipymeCache(employees = listOf(cashier, manager)))

        val employee = repository.currentEmployeeFlow.first()

        assertEquals("emp_1", employee?.id)
        assertEquals(listOf(PosPermission.CHECKOUT), employee?.permissions)
    }

    @Test
    fun `there is no current employee while signed out or without an employee id`() = runTest {
        val cache = FakeMipymeCache(employees = listOf(cashier))

        assertNull(repository(FakeAuthService(AuthState.Unauthenticated), cache).currentEmployeeFlow.first())
        assertNull(repository(FakeAuthService(AuthState.Initializing), cache).currentEmployeeFlow.first())
        assertNull(repository(FakeAuthService(authenticated(employeeId = null)), cache).currentEmployeeFlow.first())
    }

    @Test
    fun `there is no current employee when the employee is not cached`() = runTest {
        val repository = repository(FakeAuthService(authenticated()), FakeMipymeCache(employees = listOf(manager)))

        assertNull(repository.currentEmployeeFlow.first())
    }

    @Test
    fun `an override takes precedence over the signed in employee`() = runTest {
        val override = MutableStateFlow<EmployeeOverride?>(null)
        val repository = repository(
            FakeAuthService(authenticated()),
            FakeMipymeCache(employees = listOf(cashier, manager)),
            override,
        )
        runCurrent()

        override.value = EmployeeOverride("user_1", "emp_2")

        assertEquals("emp_2", repository.currentEmployeeFlow.first()?.id)
    }

    @Test
    fun `the current employee updates when the cached employee changes`() = runTest {
        val cache = FakeMipymeCache(employees = listOf(cashier))
        val repository = repository(FakeAuthService(authenticated()), cache)
        val emissions = mutableListOf<Employee?>()
        backgroundScope.launch { repository.currentEmployeeFlow.collect { emissions.add(it) } }
        runCurrent()

        cache.employees = listOf(syncEmployee("emp_1", emptyList(), EmployeeStatus.DISABLED))
        runCurrent()

        assertEquals(listOf(EmployeeStatus.ENABLED, EmployeeStatus.DISABLED), emissions.map { it?.status })
    }

    @Test
    fun `the override is cleared when the signed in account changes`() = runTest {
        val authService = FakeAuthService(authenticated(accountId = "user_1", employeeId = "emp_1"))
        val override = MutableStateFlow<EmployeeOverride?>(null)
        val repository = repository(authService, FakeMipymeCache(employees = listOf(cashier, manager)), override)
        runCurrent()
        override.value = EmployeeOverride("user_1", "emp_2")
        runCurrent()

        authService.authState.value = authenticated(accountId = "user_2", employeeId = "emp_1")
        runCurrent()

        assertNull(override.value)
        assertEquals("emp_1", repository.currentEmployeeFlow.first()?.id)
    }

    @Test
    fun `the override is cleared when the account signs out`() = runTest {
        val authService = FakeAuthService(authenticated())
        val override = MutableStateFlow<EmployeeOverride?>(null)
        repository(authService, FakeMipymeCache(employees = listOf(cashier, manager)), override)
        runCurrent()
        override.value = EmployeeOverride("user_1", "emp_2")
        runCurrent()

        authService.authState.value = AuthState.Unauthenticated
        runCurrent()

        assertNull(override.value)
    }

    @Test
    fun `the override is kept when the same account is refreshed`() = runTest {
        val authService = FakeAuthService(authenticated(employeeId = "emp_1"))
        val override = MutableStateFlow<EmployeeOverride?>(null)
        repository(authService, FakeMipymeCache(employees = listOf(cashier, manager)), override)
        runCurrent()
        override.value = EmployeeOverride("user_1", "emp_2")
        runCurrent()

        authService.authState.value = authenticated(employeeId = "emp_2")
        runCurrent()

        assertEquals(EmployeeOverride("user_1", "emp_2"), override.value)
    }

    @Test
    fun `a stale override is never emitted for another account even before it is cleared`() = runTest {
        val authService = FakeAuthService(authenticated(accountId = "user_1", employeeId = "emp_1"))
        val neverClearing = CoroutineScope(Job().apply { cancel() })
        val override = MutableStateFlow<EmployeeOverride?>(EmployeeOverride("user_1", "emp_2"))
        val repository = repository(
            authService,
            FakeMipymeCache(employees = listOf(cashier, manager, syncEmployee("emp_3", emptyList()))),
            override,
            neverClearing,
        )
        val emissions = mutableListOf<String?>()
        backgroundScope.launch { repository.currentEmployeeFlow.collect { emissions.add(it?.id) } }
        runCurrent()

        authService.authState.value = authenticated(accountId = "user_2", employeeId = "emp_3")
        runCurrent()

        assertEquals(listOf<String?>("emp_2", "emp_3"), emissions)
    }

    private fun TestScope.repository(
        authService: FakeAuthService,
        cache: FakeMipymeCache,
        override: MutableStateFlow<EmployeeOverride?> = MutableStateFlow(null),
        scope: CoroutineScope = backgroundScope,
    ): CurrentEmployeeRepository {
        return CurrentEmployeeRepository(
            authService = authService,
            cache = cache,
            scope = scope,
            employeeOverride = override,
        )
    }

    private fun authenticated(
        accountId: String = "user_1",
        employeeId: String? = "emp_1",
    ): AuthState.Authenticated {
        return AuthState.Authenticated(
            IdentityAccount(
                id = accountId,
                name = "Monkey D. Luffy",
                email = "luffy@example.com",
                pictureUrl = "",
                publicMetadata = IdentityPublicMetadata(merchantId = "merch_1", employeeId = employeeId),
            ),
        )
    }
}
