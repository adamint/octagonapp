package com.adamratzman.octagon

import com.google.gson.GsonBuilder
import com.rethinkdb.RethinkDB
import com.rethinkdb.net.Connection
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val r = RethinkDB.r

class Octagon(config: Config) {
    val conn: Connection = r.connection().hostname(config["dbhost"]).connect()
    // val agglomerator: Agglomerator = Agglomerator(this)
    val gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()
    val executor = Executors.newCachedThreadPool()

    init {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({ updateSnippets() }, 0, 5, TimeUnit.MINUTES)
    }
}

fun main(args: Array<String>) {
    Octagon(Config(args.first()))
    // println(octagon.api.register("Adam Ratzman", "adam@adamratzman.com", UserType.COMMUNITY).key)
}