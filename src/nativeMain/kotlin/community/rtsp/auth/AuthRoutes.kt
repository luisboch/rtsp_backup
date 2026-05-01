package community.rtsp.auth

import community.rtsp.util.GenerateRandomService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.sessions.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.*
import io.ktor.server.routing.*
import community.rtsp.auth.SessionService
import community.rtsp.auth.AuthRepository
import community.rtsp.db.Session
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class UserSession(val userId: Long, val token: String) : Principal

fun Route.authRoutes(
    authRepository: AuthRepository,
    sessionService: SessionService,
    randomService: GenerateRandomService
) {
    route("/api/auth") {
        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            val userId = authRepository.verifyCredentials(username, password)
            if (userId != null) {
                val token = randomService.generate(32)
                sessionService.createSession(userId, token)
                
                val session = UserSession(userId, token)
                call.sessions.set(session)
                
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("message" to "Logged in")
                )
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Invalid credentials"))
            }
        }

        post("/register") {
            val params = call.receiveParameters()
            val username = params["username"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.BadRequest)

            if (authRepository.getUserByUsername(username) != null) {
                return@post call.respond(HttpStatusCode.Conflict, mapOf("message" to "Username already exists"))
            }

            if (authRepository.createUser(username, password)) {
                val user = authRepository.getUserByUsername(username)
                if (user != null) {
                    val token = randomService.generate(32)
                    sessionService.createSession(user.id, token)
                    call.sessions.set(UserSession(user.id, token))
                    call.respond(HttpStatusCode.Created, mapOf("message" to "User created and logged in"))
                } else {
                    call.respond(HttpStatusCode.Created, mapOf("message" to "User created successfully"))
                }
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("message" to "Failed to create user"))
            }
        }

        post("/logout") {
            val session = call.sessions.get<UserSession>()
            if (session != null) {
                sessionService.invalidateToken(session.token)
                call.sessions.clear<UserSession>()
            }
            call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out"))
        }
    }
}
