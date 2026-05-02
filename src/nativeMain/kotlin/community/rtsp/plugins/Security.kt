package community.rtsp.plugins

import community.rtsp.auth.SessionService
import community.rtsp.auth.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*

fun Application.configureSecurity(sessionService: SessionService) {
    install(Sessions) {
        cookie<UserSession>("session_id") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.secure = false
            cookie.extensions["SameSite"] = "Lax"
        }
    }

    install(Authentication) {
        session<UserSession>("auth-session") {
            validate { session ->
                if (sessionService.getUserIdByToken(session.token) != null) {
                    session
                } else {
                    null
                }
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, mapOf("message" to "Unauthorized"))
            }
        }
    }
}