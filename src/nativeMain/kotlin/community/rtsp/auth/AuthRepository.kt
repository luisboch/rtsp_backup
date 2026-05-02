package community.rtsp.auth

import community.rtsp.db.Database
import community.rtsp.db.User

class AuthRepository(
    private val database: Database,
    private val passwordHasher: PasswordHasher
) {
    private val userQueries = database.userQueries

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
}
