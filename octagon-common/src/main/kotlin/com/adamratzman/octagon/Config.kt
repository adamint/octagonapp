package com.adamratzman.octagon

import java.io.File

data class Config(var url: String) {
    private val file = File(url)
    private val values: HashMap<String, String> = hashMapOf()

    private fun load() {
        file.readLines().forEach {
            val split = it.split(" :: ")
            values[split[0]] = split[1]
        }
    }

    init {
        load()
    }

    fun reload() {
        values.clear()
        load()
    }

    operator fun get(key: String): String = values[key]
            ?: throw IllegalArgumentException("Unable to find key $key in configuration")
}
