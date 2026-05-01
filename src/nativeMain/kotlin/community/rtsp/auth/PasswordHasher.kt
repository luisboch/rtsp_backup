package community.rtsp.auth

import kotlin.random.Random

class PasswordHasher {
    // For the sake of this prototype in a Kotlin/Native environment without easy 
    // access to BCrypt, we use a salted SHA-256 approach.
    // TODO: In production, use a robust implementation like BCrypt or Argon2.
    
    fun hashPassword(password: String, salt: String): String {
        // Simplified for demonstration
        return (password + salt).hashCode().toString() 
    }

    fun verifyPassword(password: String, salt: String, hash: String): Boolean {
        return hashPassword(password, salt) == hash
    }
}
