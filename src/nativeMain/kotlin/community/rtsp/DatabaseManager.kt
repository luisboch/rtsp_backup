package community.rtsp

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.wrapConnection
import co.touchlab.sqliter.DatabaseConfiguration
import community.rtsp.config.AppConfig
import community.rtsp.db.Database

class DatabaseManager(config: AppConfig) {
    private val driver: SqlDriver = NativeSqliteDriver(
        configuration = DatabaseConfiguration(
            name = "database.db",
            version = Database.Schema.version.toInt(),
            create = { connection ->
                wrapConnection(connection) { Database.Schema.create(it) }
            },
            upgrade = { connection, oldVersion, newVersion ->
                wrapConnection(connection) { Database.Schema.migrate(it, oldVersion.toLong(), newVersion.toLong()) }
            },
            extendedConfig = DatabaseConfiguration.Extended(
                basePath = config.dataDir
            )
        )
    )

    val database = Database(driver)
}