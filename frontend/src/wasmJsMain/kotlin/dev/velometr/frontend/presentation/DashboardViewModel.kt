package dev.velometr.frontend.presentation

import dev.velometr.frontend.data.ActivityDto
import dev.velometr.frontend.data.ApiClient
import dev.velometr.frontend.data.UnauthorizedException
import dev.velometr.frontend.data.YearSummaryDto
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

data class DashboardData(
    val summary: YearSummaryDto,
    val weeks: List<Double>,
    val activities: List<ActivityDto>,
)

/** Holds DashboardScreen's state and talks to the API; the screen only renders what this exposes. */
class DashboardViewModel(
    private val api: ApiClient,
    private val scope: CoroutineScope,
    private val onLoggedOut: () -> Unit,
) {
    var year by mutableStateOf(currentYear())
        private set
    var data by mutableStateOf<DashboardData?>(null)
        private set
    var loadError by mutableStateOf(false)
        private set
    var showImportModal by mutableStateOf(false)
        private set

    private var loadJob: Job? = null

    init {
        load()
    }

    fun setYear(newYear: Int) {
        if (newYear == year) return
        year = newYear
        data = null
        load()
    }

    fun retry() = load()

    fun openImportModal() {
        showImportModal = true
    }

    fun closeImportModal() {
        showImportModal = false
    }

    /** Called from ImportModal's own coroutine scope, which catches [dev.velometr.frontend.data.ImportException] to show an inline error. */
    suspend fun importZip(bytes: ByteArray, filename: String) {
        try {
            api.importZip(bytes, filename)
            showImportModal = false
            load()
        } catch (e: UnauthorizedException) {
            api.logout()
            showImportModal = false
            onLoggedOut()
        }
    }

    private fun load() {
        loadJob?.cancel()
        val requestedYear = year
        loadError = false
        loadJob = scope.launch {
            try {
                val loaded = DashboardData(
                    api.yearSummary(requestedYear),
                    api.weeklyDistances(requestedYear).weeks,
                    api.activities(requestedYear),
                )
                currentCoroutineContext().ensureActive()
                data = loaded
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnauthorizedException) {
                api.logout()
                onLoggedOut()
            } catch (e: Exception) {
                loadError = true
            }
        }
    }
}
