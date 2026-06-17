package com.overdrive.catalog.web

import com.overdrive.catalog.app.CatalogReferentialIntegrityException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

/** Catalog-domain exception mappings supplementing the platform GlobalExceptionHandler. */
@RestControllerAdvice(basePackages = ["com.overdrive.catalog.web"])
class CatalogExceptionHandler {

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.NOT_FOUND, "not-found", "Not Found", ex.message ?: "Resource not found")

    @ExceptionHandler(CatalogReferentialIntegrityException::class)
    fun handleReferentialIntegrity(ex: CatalogReferentialIntegrityException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.CONFLICT, "referential-integrity", "Conflict", ex.message ?: "Entity is referenced")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> =
        problem(HttpStatus.BAD_REQUEST, "validation", "Bad Request", ex.message ?: "Invalid input")

    private fun problem(
        status: HttpStatus,
        type: String,
        title: String,
        detail: String
    ): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.type  = URI.create("https://api.overdrive.com/errors/$type")
            this.title = title
        }
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }
}
