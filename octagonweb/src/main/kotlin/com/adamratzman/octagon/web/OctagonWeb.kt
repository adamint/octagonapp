package com.adamratzman.octagon.web

import com.adamratzman.octagon.Config
import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.SourceData
import com.adamratzman.octagon.queryAsArrayList
import com.adamratzman.octagon.web.api.Api
import com.adamratzman.octagon.web.rest.RestApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rethinkdb.RethinkDB
import com.rethinkdb.RethinkDB.r
import com.rethinkdb.net.Connection
import java.util.concurrent.Executors

val r: RethinkDB = RethinkDB.r

class Octagon(val config: Config) {
    val executor = Executors.newScheduledThreadPool(0)
    val cachedExecutor = Executors.newCachedThreadPool()
    val gson: Gson = GsonBuilder().disableHtmlEscaping().create()
    val conn: Connection = r.connection().hostname(config["dbhost"]).connect()
    val api: Api = Api(this)
    val restApi: RestApi = RestApi(this)
    var sources = listOf<SourceData>()
    var newsSources = listOf<NewsSource>()

    init {
        retrieveNewsSources()
    }

    private fun retrieveNewsSources() {
        if (!r.db("octagon").tableList().run<List<String>>(conn).contains("sources")) {
            r.db("octagon").tableCreate("sources").run<Any>(conn)
        }
        val dbSources = r.db("octagon").table("sources").run<Any>(conn)
                .queryAsArrayList(gson, SourceData::class.java).filterNotNull()
        sources = dbSources
        newsSources = dbSources.map { it.newsSource }
    }

    fun getSource(sourceId: String): NewsSource? {
        return NewsSource.values().find { src ->
            src.name.equals(sourceId, true) || src.readableName.equals(sourceId, true)
                    || src.readableName.replace(" ", "_").equals(sourceId, true)
                    || src.id.equals(sourceId, true)
        }
    }
}

fun String.toNewsSource(octagon: Octagon) = octagon.getSource(this)

fun main(args: Array<String>) {
    Octagon(Config(args.first()))
}