package com.fourthwardai.orbit.ui.util

import coil.intercept.Interceptor
import coil.request.ImageResult
import kotlinx.coroutines.delay

class SlowdownInterceptor(private val delayMillis: Long) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        delay(delayMillis)
        return chain.proceed(chain.request)
    }
}
