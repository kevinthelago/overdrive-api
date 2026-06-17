package com.overdrive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class OverdriveApplication

fun main(args: Array<String>) {
    runApplication<OverdriveApplication>(*args)
}
