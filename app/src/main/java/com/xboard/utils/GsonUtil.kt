package com.sunmi.background.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import java.io.Reader
import java.lang.reflect.Type

/**
 * Created by jogger on 2020/4/7
 * 描述：
 */
object GsonUtil {
    private val GSON = createGson(true)

    private val GSON_NO_NULLS = createGson(false)
    fun getGson(): Gson {
        return getGson(true)
    }

    fun getGson(serializeNulls: Boolean): Gson {
        return if (serializeNulls) GSON_NO_NULLS else GSON
    }

    fun toJson(`object`: Any): String {
        return toJson(`object`, true)
    }

    fun toJson(`object`: Any, includeNulls: Boolean): String {
        return if (includeNulls) GSON.toJson(`object`) else GSON_NO_NULLS.toJson(`object`)
    }

    fun <T> fromJson(json: String?, type: Class<T>): T? {
        if (json == null) return null
        try {
            return GSON.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun <T> fromJson(json: String?, type: Type): T? {
        return GSON.fromJson(json, type)
    }

    fun <T> fromJson(reader: Reader, type: Class<T>): T {
        return GSON.fromJson(reader, type)
    }

    fun <T> fromJson(reader: Reader, type: Type): T {
        return GSON.fromJson(reader, type)
    }

    @Throws(JsonSyntaxException::class)
    fun <T> fromJson(json: JsonElement, classOfT: Class<T>): T {
        return GSON.fromJson(json, classOfT)
    }

    private fun createGson(serializeNulls: Boolean): Gson {
        val builder = GsonBuilder()
        if (serializeNulls) builder.serializeNulls()
        return builder.create()
    }
}