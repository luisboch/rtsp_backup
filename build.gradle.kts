plugins {
    kotlin("multiplatform") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("app.cash.sqldelight") version "2.3.2"
}

group = "community.rtsp"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"

kotlin {
    val nativeTarget = linuxX64("native")

    nativeTarget.apply {
        binaries{
            executable {
                entryPoint = "community.rtsp.main"
            }
        }
        binaries.all {
            linkerOpts(
                "-L/usr/lib/x86_64-linux-gnu",
                "-lsqlite3",
                "-lpthread",
                "-ldl",
                "--allow-shlib-undefined"
            )
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-cio:$ktorVersion")
                implementation("io.ktor:ktor-server-cors:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-server-auth:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("app.cash.sqldelight:native-driver:2.3.2")
            }
        }
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("community.rtsp.db")
            srcDirs.setFrom("src/nativeMain/sqldelight")
        }
    }
}

