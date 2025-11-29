package com.xboard.utils

import com.tencent.mmkv.MMKV

class MMKVUtil private constructor(name: String) {
    /**
     * 默认配置文件
     */
    val mName = name

    init {
        if (mName == DEFAULT_MCONFIG_FILE_NAME) {
            mmkv = MMKV.defaultMMKV()
        } else {
            mmkv = MMKV.mmkvWithID(mName)
        }
    }

    companion object {
        val DEFAULT_MCONFIG_FILE_NAME = "default_mconfig"
        @Volatile
        private var instance: MMKVUtil? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: MMKVUtil(DEFAULT_MCONFIG_FILE_NAME).apply {
                instance = this
            }
        }

        fun getInstance(name: String) = instance ?: synchronized(this) {
            instance ?: MMKVUtil(name).apply {
                instance = this
            }
        }

        private lateinit var mmkv: MMKV
    }

    fun <T> setValue(key: String, value: T) {
        when (value) {
            is Int -> mmkv.encode(key, value)
            is String -> mmkv.encode(key, value)
            is Long -> mmkv.encode(key, value)
            is Float -> mmkv.encode(key, value)
            is Double -> mmkv.encode(key, value)
            is Boolean -> mmkv.encode(key, value)
            is ByteArray -> mmkv.encode(key, value)
            is Set<*> -> mmkv.encode(key, value as Set<String>)
            else -> throw IllegalArgumentException("Unsupported type: ${value?.javaClass?.name}")
        }
    }

    fun getIntValue(key: String, defValue: Int): Int {
        return mmkv.getInt(key, 0)
    }

    fun getStringValue(key: String, defValue: String): String? {
        return mmkv.getString(key, defValue)
    }

    fun getBooleanValue(key: String, defValue: Boolean): Boolean {
        return mmkv.getBoolean(key, defValue)
    }

    fun getFloatValue(key: String, defValue: Float): Float {
        return mmkv.getFloat(key, defValue)
    }

    fun getDoubleValue(key: String, defValue: Double): Double {
        return mmkv.decodeDouble(key, defValue)
    }

    fun getLongValue(key: String, defValue: Long): Long {
        return mmkv.getLong(key, defValue)
    }

    fun clear(keys: Array<String>) {
        mmkv.removeValuesForKeys(keys)
    }


}