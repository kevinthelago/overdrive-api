package com.overdrive.common.web

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ProblemDetail> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            mapOf("field" to it.field, "message" to (it.defaultMessage ?: "invalid"))
        }
        return problem(
            status = HttpStatus.BAD_REQUEST,
            type = "validation",
            title = "Validation Error",
            detail = "One or more fields failed validation",
        ) { setProperty("errors", fieldErrors) }
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLock(ex: ObjectOptimisticLockingFailureException): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.CONFLICT,
            type = "optimistic-lock",
            title = "Conflict",
            detail = "Resource was modified concurrently; fetch and retry",
        ) { setProperty("reason", "OPTIMISTIC_LOCK_FAILURE") }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.CONFLICT,
            type = "referential-integrity",
            title = "Conflict",
            detail = "Data integrity constraint violated",
        ) { setProperty("reason", "REFERENTIAL_INTEGRITY_VIOLATION") }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ResponseEntity<ProblemDetail> =
        problem(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            type = "internal",
            title = "Internal Server Error",
            detail = "An unexpected error occurred",
        )

    private fun problem(
        status: HttpStatus,
        type: String,
        title: String,
        detail: String,
        configure: ProblemDetail.() -> Unit = {},
    ): ResponseEntity<ProblemDetail> {
        val pd = ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.type = URI.create("https://api.overdrive.com/errors/$type")
            this.title = title
            configure()
        }
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd)
    }
}
