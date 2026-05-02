package community.rtsp

import community.rtsp.auth.*
import community.rtsp.config.ConfigLoader
import community.rtsp.plugins.configureRouting
import community.rtsp.plugins.configureSecurity
import community.rtsp.plugins.configureSerialization
import community.rtsp.stream.CleanService
import community.rtsp.stream.StreamBackupService
import community.rtsp.stream.StreamRepository
import community.rtsp.system.FfmpegCliService
import community.rtsp.system.SystemStatsService
import community.rtsp.util.GenerateRandomService
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val host = getenv("HOST")?.toKString() ?: "0.0.0.0"
    val port = getenv("PORT")?.toKString()?.toIntOrNull() ?: 8080

    embeddedServer(CIO, host = host, port = port) {
        module()
    }.start(wait = true)
}

@OptIn(ExperimentalForeignApi::class)
fun Application.module() {
    val config = ConfigLoader.load()
    val dbManager = DatabaseManager(config)
    val database = dbManager.database

    val authRepository = AuthRepository(database, PasswordHasher())
    val streamRepository = StreamRepository(database)
    val sessionService = SessionService(database.sessionQueries)
    val sessionCleanupService = SessionCleanupService(sessionService)
    val randomService = GenerateRandomService()

    val systemStatsService = SystemStatsService()
    val ffmpegCliService = FfmpegCliService()
    val backupService = StreamBackupService(config, streamRepository)
    val cleanService = CleanService(config, streamRepository)

    backupService.start()
    cleanService.start()
    sessionCleanupService.start()

    environment.monitor.subscribe(ApplicationStopping) {
        backupService.stop()
        cleanService.stop()
        sessionCleanupService.stop()
    }

    // Call extension methods
    configureSerialization()
    configureSecurity(sessionService)
    configureRouting(
        config,
        authRepository,
        streamRepository,
        sessionService,
        randomService,
        backupService,
        systemStatsService,
        ffmpegCliService
    )
}
