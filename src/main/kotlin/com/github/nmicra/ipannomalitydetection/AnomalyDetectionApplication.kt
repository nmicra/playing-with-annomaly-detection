package com.github.nmicra.ipannomalitydetection

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(info = Info(title = "AnomalyDetectionApplication", version = "1.0", description = "Enter /v3/api-docs into the search box"))
class AnomalyDetectionApplication

fun main(args: Array<String>) {
	runApplication<AnomalyDetectionApplication>(*args)
}
