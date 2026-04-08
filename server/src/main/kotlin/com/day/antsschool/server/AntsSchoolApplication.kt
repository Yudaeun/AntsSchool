package com.day.antsschool.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AntsSchoolApplication

fun main(args: Array<String>) {
    runApplication<AntsSchoolApplication>(*args)
}
