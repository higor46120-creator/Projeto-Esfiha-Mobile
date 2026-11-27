package br.com.fiec.helptec.data.api

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class UnwrapJsonInterceptor(private val gson: Gson = Gson()) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val rawBody = response.body?.string() ?: return response

        val trimmed = rawBody.trim()
        // Se começar e terminar com aspas, o API Gateway/Lambda devolveu uma String JSON escapada
        val unwrappedBody = if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            try {
                gson.fromJson(trimmed, String::class.java)
            } catch (e: Exception) {
                rawBody // Se falhar, mantém o original para não mascarar erros reais
            }
        } else {
            rawBody
        }

        return response.newBuilder()
            .body(unwrappedBody.toResponseBody(response.body?.contentType()))
            .build()
    }
}