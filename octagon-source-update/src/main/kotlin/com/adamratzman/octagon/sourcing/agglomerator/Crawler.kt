package com.adamratzman.octagon.sourcing.agglomerator

import com.adamratzman.octagon.FeedInformation
import com.adamratzman.octagon.NewsFeed
import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.asPojo
import com.adamratzman.octagon.sourcing.SourceAgglomerator
import com.adamratzman.octagon.sourcing.r
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rethinkdb.gen.exc.ReqlDriverError
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import org.jsoup.Jsoup
import java.io.StringReader
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

open class Crawler(val agglomerator: SourceAgglomerator, val rssBase: String, val rssList: String, val source: NewsSource,
                   val headlineName: String? = null, val globalHeadline: Boolean = false, val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()) {
    val input: SyndFeedInput = SyndFeedInput()
    val feeds = CopyOnWriteArrayList<NewsFeed>()
    val creatingDbs = mutableListOf<String>()

    init {
        agglomerator.executor.scheduleAtFixedRate({
            try {
                println("Populating feed for ${source.readableName}")
                populateFeeds()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, 15, TimeUnit.MINUTES)
    }

    open fun generateFeeds() = rssList.getDocument().body().getElementsByTag("a")
            .map {
                if (!it.attr("href").startsWith("http")) {
                    getDomain() + it.attr("href")
                } else it.absUrl("href")
            }.distinct()

    fun populateFeeds() {
        val links = generateFeeds().toMutableList()
        // Handle special cases
        links.removeIf { it.contains("douthat") } // NYT duplication
        println("Populating total ${links.distinct().size} feed LINKS in ${source.readableName}")
        links.distinct().forEach { url ->
            agglomerator.cachedExecutor.execute {
                try {
                    if (url != rssList && url.contains(rssBase)) {
                        try {
                            println("FEED: ${source.readableName} $url")
                            val feed = getSyndFeed(url, source)
                            if (feed.title != null) {
                                val newsFeed = NewsFeed(source.id.toUpperCase(), feed.title, url, feed.description, feed.language
                                        ?: "en_US", headline = feed.title == headlineName,
                                        globalHeadline = feed.title == headlineName && globalHeadline)
                                agglomerator.cachedExecutor.execute { updateFeed(source, newsFeed) }
                            }
                        } catch (e: ReqlDriverError) {
                            println("Network issues..")
                            throw e
                        } catch (e: Exception) {
                            println("Exception in $url")
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    fun getSyndFeed(url: String, source: NewsSource, first: Boolean = true): SyndFeed {
        return try {
            val feed = input.build(StringReader(url.getDocument().html()))
            when (source.id) {
                "the_west_au", "usa_today" -> {
                    val title = feed.description
                    feed.description = null
                    feed.title = title
                }
            }
            feed
        } catch (e: Exception) {
            if (!first) throw e
            getSyndFeed(url, source, false)
        }
    }

    fun getDomain(): String = if (rssList.startsWith("https")) "https://" + rssList.removePrefix("https://").split("/")[0]
    else "http://" + rssList.removePrefix("http://").split("/")[0]

    fun updateFeed(source: NewsSource, newsFeed: NewsFeed) {
        println("Running update check for ${source.readableName} : ${newsFeed.name}.. ${Date().toLocaleString()}")
        try {
            while (creatingDbs.contains(source.id)) {
            }

            if (!r.dbList().run<List<String>>(agglomerator.conn).contains(source.id)) {
                creatingDbs.add(source.id)
                r.dbCreate(source.id).run<Any>(agglomerator.conn)
                creatingDbs.remove(source.id)
            }

            if (feeds.find { it.url != newsFeed.url && it.id == newsFeed.id} != null) newsFeed.id = newsFeed.id + "_${newsFeed.url.hashCode().absoluteValue}"
            else feeds.add(newsFeed)

            if (!r.db(source.id).tableList().run<List<String>>(agglomerator.conn).contains(newsFeed.id)) {
                r.db(source.id).tableCreate(newsFeed.id).run<Any>(agglomerator.conn)
                r.db(source.id).table(newsFeed.id).indexCreate("url").run<Any>(agglomerator.conn)
                r.db(source.id).table(newsFeed.id).indexWait("url").run<Any>(agglomerator.conn)
                println("Fixing irregularity of ${newsFeed.name} RSS feed - ${newsFeed.source}")
                println("${source.id} ${newsFeed.id}")
            }
            r.db(source.id).table(newsFeed.id).wait_().run<Any>(agglomerator.conn)
            if (asPojo(gson, r.db(source.id).table(newsFeed.id).get("information").run(agglomerator.conn), FeedInformation::class.java) == null) {
                println("Inserted feed info for ${newsFeed.name} (${source.readableName})")
                r.db(source.id).table(newsFeed.id).insert(r.json(gson.toJson(FeedInformation(newsFeed.url, newsFeed.published, headline = newsFeed.headline, globalHeadline = globalHeadline)))).run<Any>(agglomerator.conn)
            }
            if (asPojo(gson, r.db(source.id).table(newsFeed.id).get(newsFeed.id).run(agglomerator.conn), NewsFeed::class.java) == null) {
                r.db(source.id).table(newsFeed.id).insert(r.json(gson.toJson(newsFeed))).run<Any>(agglomerator.conn)
                println("Inserted news feed for ${newsFeed.name} (${source.readableName})")
            } else r.db(source.id).table(newsFeed.id).get(newsFeed.id).update(r.json(gson.toJson(newsFeed))).runNoReply(agglomerator.conn)

            println("Done with update check for ${source.readableName} : ${newsFeed.name}.. ${Date().toLocaleString()}")
        } catch (e: ReqlDriverError) {
            println("Network issues..")
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun crawler(source: NewsSource, rssBase: String, rssList: String, headlineName: String? = null,
            globalHeadline: Boolean = false, feeds: List<String>? = null): (SourceAgglomerator) -> Crawler = { agglo ->
    object : Crawler(agglo, rssBase, rssList, source, headlineName, globalHeadline) {
        override fun generateFeeds(): List<String> {
            return feeds ?: super.generateFeeds()
        }
    }
}

fun String.getDocument() = Jsoup.connect(this).userAgent("Octagon").get()


class CrawlerList(vararg crawlerLambdas: SourceAgglomerator.() -> Crawler) : ArrayList<SourceAgglomerator.() -> Crawler>(crawlerLambdas.toList())
