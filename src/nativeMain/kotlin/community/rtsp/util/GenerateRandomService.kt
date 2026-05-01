package community.rtsp.util

import kotlin.random.Random

class GenerateRandomService {
    fun generate(size: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..size).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}
