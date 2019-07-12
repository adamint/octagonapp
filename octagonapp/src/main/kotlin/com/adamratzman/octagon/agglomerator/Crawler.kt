package com.adamratzman.octagon.agglomerator

import com.adamratzman.octagon.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rethinkdb.gen.exc.ReqlDriverError
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import javafx.application.Application.launch
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.StringReader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
/*
open class Crawler(val agglomerator: Agglomerator, val rssBase: String, val rssList: String, val source: NewsSource,
                   val headlineName: String? = null, val globalHeadline: Boolean = false, val gson: Gson = GsonBuilder().disableHtmlEscaping().serializeNulls().create()) {
    val input: SyndFeedInput = SyndFeedInput()
    val feeds: List<NewsFeed> = populateFeeds()
    val cache: ConcurrentHashMap<NewsFeed, MutableList<Snippet>> = ConcurrentHashMap()
    var firstRun = true

    init {
            runBlocking {
                println("Source Instantiation | Retrieving cache for ${source.readableName}")
                launch { retrieveCache() }
                println("Source Instantiation | Initiated ${source.readableName} with ${feeds.size} feeds")
                println("Distinct feeds: ${feeds.distinctBy { it.id }}")
                feeds.distinctBy { it.id }.forEach { newsFeed ->
                    launch {
                        println("Running update check for ${source.readableName} : ${newsFeed.name}.. ${Date().toLocaleString()}")
                        try {
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

                            updateSnippets(newsFeed.url)

                            var count = 0
                            cache.putIfAbsent(newsFeed, mutableListOf())
                            cache[newsFeed]!!.forEach { snippet ->
                                if (!r.db(source.id).table(newsFeed.id).indexList().run<List<String>>(agglomerator.conn).contains("url")) {
                                    r.db(source.id).table(newsFeed.id).indexCreate("url").run<Any>(agglomerator.conn)
                                    r.db(source.id).table(newsFeed.id).indexWait("url").run<Any>(agglomerator.conn)
                                }
                                if (r.db(source.id).table(newsFeed.id).getAll(snippet.url).optArg("index", "url")
                                                .run<Any>(agglomerator.conn).queryAsArrayList(gson, Snippet::class.java)
                                                .filterNotNull().isEmpty()) {
                                    r.db(source.id).table(newsFeed.id).insert(r.json(gson.toJson(snippet)))
                                            .runNoReply(agglomerator.conn)
                                    count++
                                }
                            }
                            if (count != 0) println("Added $count snippets in ${newsFeed.name} - ${newsFeed.source} - ${Date().toLocaleString()}")
                            if (firstRun) {
                                firstRun = false
                                println("Set ${source.readableName} to normal insertion")
                            }
                        } catch (e: ReqlDriverError) {
                            println("Restarting aggregator due to network issues..")
                            agglomerator.octagon.main.restart()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
        }
    }

    fun updateSnippets(feedUrl: String): List<Snippet> {
        val newsFeed = getNewsFeed(feedUrl)
        val feed = getSyndFeed(feedUrl, source)
        val snippets = cache[newsFeed]
        if (snippets != null) {
            feed.publishedDate?.time?.let { newsFeed.published = it }
            feed.entries.forEach { entry ->
                if (snippets.none { it.url == entry.link } && (entry.link != null || entry.uri != null)) {
                    snippets.add(Snippet(entry.title.clean(), entry.author.clean(), entry.publishedDate?.time
                            ?: System.currentTimeMillis(), entry.description?.value?.let { Jsoup.parse(it).text().clean() },
                            entry.link?.clean()
                                    ?: entry.uri.clean(), source.id.toUpperCase(), entry.categories?.map { it.name.clean() },
                            insertionDate = (if (firstRun) entry.publishedDate?.time else null)
                                    ?: System.currentTimeMillis()))
                }
            }
        } else {
            val newFeedSnippet = feed.entries.map { entry ->
                if (entry.link != null || entry.uri != null) {
                    Snippet(entry.title.clean(), entry.author.clean(), entry.publishedDate?.time
                            ?: System.currentTimeMillis(), entry.description?.value?.let { Jsoup.parse(it).text().clean() },
                            entry.link?.clean()
                                    ?: entry.uri.clean(), source.id.toUpperCase(), entry.categories?.map { it.name.clean() },
                            insertionDate = (if (firstRun) entry.publishedDate?.time else null)
                                    ?: System.currentTimeMillis())
                } else null
            }.filterNotNull().toMutableList()
            cache[newsFeed] = newFeedSnippet
            return newFeedSnippet
        }
        return snippets
    }

    fun retrieveCache() {
        try {
            if (!r.dbList().run<List<String>>(agglomerator.conn).contains(source.id)) r.dbCreate(source.id).run<Any>(agglomerator.conn)
            r.db(source.id).tableList().run<List<String>>(agglomerator.conn).forEach { tableSource ->
                val feedInfo = asPojo(gson, r.db(source.id).table(tableSource).get("information").run(agglomerator.conn), FeedInformation::class.java)
                if (feeds.map { it.id }.contains(tableSource)) {
                    if (feedInfo != null) {
                        val feed = getSyndFeed(feedInfo.url, source)
                        val newsFeed = NewsFeed(source.id.toUpperCase(), feed.title, feedInfo.url, feed.description, feed.language
                                ?: "en_US", headline = feed.title == headlineName, globalHeadline = feed.title == headlineName && globalHeadline)
                        if (feeds.none { newsFeed.url == it.url }) (feeds as MutableList).add(newsFeed)
                        cache[newsFeed] = r.db(source.id).table(tableSource).filter { it.g("type").eq("snippet") }
                                .run<Any>(agglomerator.conn).queryAsArrayList(gson, Snippet::class.java).filterNotNull().toMutableList()
                    } else {
                        println("deleted $tableSource")
                        r.db(source.id).table(tableSource).delete().run<Any>(agglomerator.conn)
                    }
                }
            }
            println("Retrieved cache for ${source.readableName}")
        } catch (e: ReqlDriverError) {
            println("Restarting aggregator due to network issues..")
            agglomerator.octagon.main.restart()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun generateFeeds() = Jsoup.parse(r.http(rssList).optArg("redirects", 20).run(agglomerator.conn)).body().getElementsByTag("a")
            .map {
                if (!it.attr("href").startsWith("http")) {
                    getDomain() + it.attr("href")
                } else it.absUrl("href")
            }.distinct()

    fun populateFeeds(): List<NewsFeed> {
        val feeds = mutableListOf<NewsFeed>()
        runBlocking {
            val links = generateFeeds().toMutableList()

            // Handle special cases
            links.removeIf { it.contains("douthat") } // NYT duplication

            links.forEach { url ->
                launch(Dispatchers.IO) {
                    if (url != rssList && url.contains(rssBase)) {
                        try {
                            val feed = getSyndFeed(url, source)
                            if (feed.title != null) {
                                val newsFeed = NewsFeed(source.id.toUpperCase(), feed.title, url, feed.description, feed.language
                                        ?: "en_US", headline = feed.title == headlineName,
                                        globalHeadline = feed.title == headlineName && globalHeadline)
                                if (!feeds.map { it.url }.contains(url)) feeds.add(newsFeed)
                            }
                        } catch (e: ReqlDriverError) {
                            println("Restarting aggregator due to network issues..")
                            agglomerator.octagon.main.restart()
                        } catch (e: Exception) {
                            println("Exception in $url")
                        }
                    }
                }
            }
        }
        println("Populated ${feeds.size} feeds for ${source.readableName}")
        return feeds.distinctBy { it.url }.toMutableList()
    }

    fun getSyndFeed(url: String, source: NewsSource, first: Boolean = true): SyndFeed {
        return try {
            val feed = input.build(StringReader(r.http(url).optArg("redirects", 20).run(agglomerator.conn)))
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

    fun getNewsFeed(feedUrl: String): NewsFeed = feeds.first { feedUrl == it.url || feedUrl.removeSuffix("/") == it.url.removeSuffix("/") }
}

class CrawlerFactory(val crawlers: ConcurrentLinkedQueue<Crawler> = ConcurrentLinkedQueue()) {
    fun add(crawler: Crawler): CrawlerFactory {
        crawlers.add(crawler)
        return this
    }
}*//**/