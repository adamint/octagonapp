package com.adamratzman.octagon.sourcing

import com.adamratzman.octagon.Config
import com.adamratzman.octagon.sourcing.crawlers.au.aus1
import com.adamratzman.octagon.sourcing.crawlers.au.aus2
import com.adamratzman.octagon.sourcing.crawlers.italy.ita1
import com.adamratzman.octagon.sourcing.crawlers.norway.no1
import com.adamratzman.octagon.sourcing.crawlers.saudi.saud1
import com.adamratzman.octagon.sourcing.crawlers.us.us1
import com.adamratzman.octagon.sourcing.crawlers.us.us2
import com.rethinkdb.RethinkDB
import com.rethinkdb.net.Connection
import java.util.concurrent.Executors

val r: RethinkDB = RethinkDB.r

class SourceAgglomerator(config: Config) {
    val crawlers = getCrawlerLists().flatten()
    val conn: Connection = r.connection().hostname(config["dbhost"]).connect()
    val executor = Executors.newScheduledThreadPool(1)
    val cachedExecutor = Executors.newCachedThreadPool()
    init {
        updateSources()
    }

    fun updateSources() {
        crawlers.forEach { crawler ->
            cachedExecutor.execute { crawler(this) }
        }
    }
}

fun getCrawlerLists() = listOf(
        us1,
        us2,
        aus1,
        aus2,
        no1,
        ita1,
        saud1
)

fun main(args: Array<String>) {
    SourceAgglomerator(Config(args.first()))
}