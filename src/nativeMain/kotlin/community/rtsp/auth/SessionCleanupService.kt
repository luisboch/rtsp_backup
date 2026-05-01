package community.rtsp.auth

import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class SessionCleanupService(
    private val sessionService: SessionService,
    private val cleanupInterval: Duration = 1.hours
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start() {
        println("Starting SessionCleanupService with interval $cleanupInterval")
        scope.launch {
            while (isActive) {
                try {
                    println("Cleaning up expired sessions...")
                    sessionService.cleanupExpiredSessions()
                } catch (e: Exception) {
                    println("Error during session cleanup: ${e.message}")
                }
                delay(cleanupInterval)
            }
        }
    }

    fun stop() {
        println("Stopping SessionCleanupService.")
        scope.cancel()
    }
}
