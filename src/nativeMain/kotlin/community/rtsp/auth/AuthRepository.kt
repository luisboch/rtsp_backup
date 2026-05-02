package community.rtsp.auth

import community.rtsp.db.Database
import community.rtsp.db.Stream
import community.rtsp.db.UserQueries
import community.rtsp.db.StreamQueries
import community.rtsp.db.StreamShareQueries
import community.rtsp.db.User

class AuthRepository(
    private val database: Database,
    private val passwordHasher: PasswordHasher
) {
    private val userQueries = database.userQueries
    private val streamQueries = database.streamQueries
    private val streamShareQueries = database.streamShareQueries

    fun getUserByUsername(username: String): User? {
        return userQueries.getUserByUsername(username).executeAsOneOrNull()
    }

    fun createUser(username: String, password: String): Boolean {
        // In a real app, use a proper salt
        val salt = "fixed_salt_for_demo"
        val hash = passwordHasher.hashPassword(password, salt)
        return try {
            userQueries.insertUser(username, hash)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun verifyCredentials(username: String, password: String): Long? {
        val user = getUserByUsername(username) ?: return null
        val salt = "fixed_salt_for_demo"
        return if (passwordHasher.verifyPassword(password, salt, user.password_hash)) {
            user.id
        } else {
            null
        }
    }

    // Stream access methods
    fun getAllStreams() = streamQueries.getAllStreams().executeAsList()

    fun getStreamsForUser(userId: Long) = streamQueries.getStreamsForUser(userId, userId)

    fun addStream(ownerId: Long, alias: String, url: String, dir: String) {
        streamQueries.insertStream(ownerId, alias, url, dir)
    }

    fun shareStream(streamId: Long, targetUserId: Long) {
        streamShareQueries.shareStream(streamId, targetUserId)
    }

    fun unshareStream(streamId: Long, userId: Long) {
        streamShareQueries.unshareStream(streamId, userId)
    }

    fun deleteAllShares(streamId: Long) {
        streamShareQueries.deleteAllShares(streamId)
    }

    fun inactivateStream(streamId: Long) {
        streamQueries.inactivateStream(streamId)
    }

    fun getStreamById(id: Long, userId: Long): Stream? {
        return streamQueries.getStreamById(id, userId).executeAsOneOrNull()
    }

    fun getStreamByAlias(alias: String, userId: Long): Stream? {
        return streamQueries.getStreamByAlias(alias, userId).executeAsOneOrNull()
    }
}
