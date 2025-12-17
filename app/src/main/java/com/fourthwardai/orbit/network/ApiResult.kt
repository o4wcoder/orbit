package com.fourthwardai.orbit.network

import kotlin.compareTo

sealed interface ApiError {
    val message: String

    data class Network(
        override val message: String = "Network error",
    ) : ApiError

    data class Http(
        val code: Int,
        override val message: String,
    ) : ApiError

    data class Parsing(
        override val message: String = "Failed to parse response",
    ) : ApiError

    data class Unknown(
        override val message: String,
        val cause: Throwable? = null,
    ) : ApiError
}

fun ApiError.isTransient(): Boolean = when (this) {
    is ApiError.Network -> true
    is ApiError.Http -> code >= 500 || code == 429 // treat 5xx and 429 as transient
    is ApiError.Parsing -> false
    is ApiError.Unknown -> true
}

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> =
    when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.Failure -> this
    }

inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> =
    also { if (this is ApiResult.Success) block(data) }

inline fun <T> ApiResult<T>.onFailure(block: (ApiError) -> Unit): ApiResult<T> =
    also { if (this is ApiResult.Failure) block(error) }

fun Throwable.toApiError(): ApiError = when (this) {
    is io.ktor.client.plugins.ClientRequestException ->
        ApiError.Http(response.status.value, message)
    is io.ktor.network.sockets.SocketTimeoutException ->
        ApiError.Network("Timeout")
    else ->
        ApiError.Unknown(message ?: "Unknown error", this)
}
