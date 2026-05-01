package community.rtsp.auth

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

class SessionService(
    private val sessionQueries: community.rtsp.db.SessionQueries,
    private val sessionDuration: Duration = 24.hours
) {
    fun createSession(userId: Long, token: String): Long {
        val expiresAt = Clock.System.now().plus(sessionDuration).toEpochMilliseconds()
        sessionQueries.insertSession(token, userId, expiresAt)
        return expiresAt
    }

    fun getUserIdByToken(token: String): Long? {
        val session = sessionQueries.getSessionByToken(token).executeAsOneOrNull()
        return if (session != null && session.expires_at > Clock.System.now().toEpochMilliseconds()) {
            session.user_id
        } else {
            null
        }
    }

    fun invalidateToken(token: String) {
        sessionQueries.deleteSessionByToken(token)
    }

    fun cleanupExpiredSessions() {
        val now = Clock.System.now().toEpochMilliseconds()
        sessionQueries.deleteExpiredSessions(now)
    }
}
