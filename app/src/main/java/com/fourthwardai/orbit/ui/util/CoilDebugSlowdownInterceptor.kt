package com.fourthwardai.orbit.ui.util

import coil.intercept.Interceptor
import coil.request.ImageResult
import kotlinx.coroutines.delay

class DebugSlowdownInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        delay(800) // 👈 force 800ms load time
        return chain.proceed(chain.request)
    }
}

class SlowdownInterceptor : coil.intercept.Interceptor {
    override suspend fun intercept(chain: coil.intercept.Interceptor.Chain): coil.request.SuccessResult {
        kotlinx.coroutines.delay(2000) // 👈 delay load for shimmer visibility
        return chain.proceed(chain.request) as coil.request.SuccessResult
    }
}
