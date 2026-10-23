package com.thanh0x.coursedeal.exception

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.dto.ApiErrorDTO
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.util.Date

/**
 * GlobalExceptionHandler class handles exceptions globally within the application.
 */
@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = logger()

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val message =
            ex.bindingResult.fieldErrors
                .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
                .ifBlank { "Validation failed" }
        val error =
            ApiErrorDTO(
                timestamp = Date(),
                status = HttpStatus.BAD_REQUEST.value(),
                error = HttpStatus.BAD_REQUEST.reasonPhrase,
                message = message,
                path = (request as? ServletWebRequest)?.request?.requestURI ?: "",
            )
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(
        exception: BadRequestException,
        request: WebRequest,
    ): ResponseEntity<ApiErrorDTO> {
        return createErrorResponse(HttpStatus.BAD_REQUEST, exception.message ?: "Bad Request", request)
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(
        exception: ConflictException,
        request: WebRequest,
    ): ResponseEntity<ApiErrorDTO> {
        return createErrorResponse(HttpStatus.CONFLICT, exception.message ?: "Conflict", request)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(
        exception: UnauthorizedException,
        request: WebRequest,
    ): ResponseEntity<ApiErrorDTO> {
        return createErrorResponse(HttpStatus.UNAUTHORIZED, exception.message ?: "Unauthorized", request)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(
        exception: ResourceNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ApiErrorDTO> {
        return createErrorResponse(HttpStatus.NOT_FOUND, exception.message ?: "Not Found", request)
    }

    @ExceptionHandler(UnsupportedOperationException::class)
    fun handleUnsupportedOperationException(
        exception: UnsupportedOperationException,
        request: WebRequest,
    ): ResponseEntity<ApiErrorDTO> {
        return createErrorResponse(HttpStatus.NOT_IMPLEMENTED, exception.message ?: "Not Implemented", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        exception: Exception,
        request: WebRequest,
    ): ResponseEntity<ApiErrorDTO> {
        log.error("Unexpected error: ", exception)
        return createErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            exception.message ?: "Internal Server Error",
            request,
        )
    }

    private fun createErrorResponse(
        status: HttpStatus,
        message: String,
        request: WebRequest,
    ): ResponseEntity<ApiErrorDTO> {
        val error =
            ApiErrorDTO(
                timestamp = Date(),
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = (request as ServletWebRequest).request.requestURI,
            )
        return ResponseEntity(error, status)
    }
}
