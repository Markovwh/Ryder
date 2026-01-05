package com.example.ryder

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
