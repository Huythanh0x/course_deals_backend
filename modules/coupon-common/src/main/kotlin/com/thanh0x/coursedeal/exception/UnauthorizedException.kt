package com.thanh0x.coursedeal.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause, false, false)
