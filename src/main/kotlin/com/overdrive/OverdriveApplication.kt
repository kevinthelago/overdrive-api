package com.overdrive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OverdriveApplication

fun main(args: Array<String>) {
    runApplication<OverdriveApplication>(*args)
}
