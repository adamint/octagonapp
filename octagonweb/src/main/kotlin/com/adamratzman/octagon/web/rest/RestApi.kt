package com.adamratzman.octagon.web.rest

import com.adamratzman.octagon.*
import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.r
import com.adamratzman.octagon.web.toNewsSource
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import org.jsoup.Jsoup
import spark.Spark.get
import spark.Spark.port
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

const val individualQueryLimitPerMonth = 100000
const val businessQueryLimitPerMonth = 500000

class RestApi(val octagon: Octagon) {
    val input = SyndFeedInput()
    var snippetCache = ConcurrentHashMap<NewsFeed, MutableList<SnippetContainer>>()
    var newsFeeds = CopyOnWriteArrayList<NewsFeed>()

    init {
        port(80)

        updateNewsFeeds()

        get("/api/:key/:type") { request, response ->
            val obj = octagon.api.query(request)
            if (obj is ErrorResponse) response.status(obj.code)
            octagon.api.gson.toJson(obj)
        }
    }


    fun getNewsFeed(url: String, source: NewsSource, headline: Boolean, globalHeadline: Boolean): NewsFeed {
        println(url)
        val feed = getSyndFeed(url)
        return NewsFeed(source.getSnippetId(), feed.title, url, feed.description
                ?: "No description available for this feed", feed.language
                ?: "en_US", feed.publishedDate?.time, headline = headline, globalHeadline = globalHeadline)
    }

    fun updateNewsFeeds() {
        octagon.executor.scheduleWithFixedDelay({
            try {
                r.dbList().run<List<String>>(octagon.conn).forEach { db ->
                    octagon.cachedExecutor.execute {
                        r.db(db).tableList().run<List<String>>(octagon.conn).forEach { feedId ->
                            octagon.cachedExecutor.execute {
                                try {
                                    asPojo(octagon.gson, r.db(db).table(feedId).get(feedId).run(octagon.conn), NewsFeed::class.java)?.let { feed ->
                                        val feedSnippets = r.db(db).table(feedId).filter { it.g("type").eq("snippet") }.run<Any>(octagon.conn)
                                                .queryAsArrayList(octagon.gson, Snippet::class.java).filterNotNull().distinctBy { it.url }
                                                .map { SnippetContainer(it, feed) }
                                        // Sanitize author names
                                        feedSnippets.forEach { (snippet, _) ->
                                            if (snippet.author?.isEmpty() == true) snippet.author = null
                                            else snippet.author = snippet.author?.toLowerCase()?.split(" ")
                                                    ?.joinToString(" ") { if (it == "and") it else it.capitalize() }
                                        }
                                        snippetCache[feed] = feedSnippets.sortedByDescending { it.snippet.insertionDate }.toMutableList()
                                        if (!newsFeeds.map { it.id }.contains(feed.id)) newsFeeds.add(feed)
                                        println("Updated ${feed.name} | ${feed.source}")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, 1, TimeUnit.MINUTES)
    }

    fun getHeadlineFeeds(country: Country?): List<NewsFeed> =
            if (country == null) newsFeeds.filter { it.globalHeadline }
            else newsFeeds.filter { it.headline }.filter { it.source.toNewsSource(octagon)?.country == country }

    fun getHeadlineArticles(country: Country?): List<SnippetContainer> {
        val sourceSnippets = getHeadlineFeeds(country).map { snippetCache[it]!! }
        val intermixedHeadlines = mutableListOf<SnippetContainer>()
        for (i in 0..(sourceSnippets.map { it.size }.max() ?: -1)) {
            sourceSnippets.forEach { if (it.size > i) intermixedHeadlines.add(it[i]) }
        }
        return intermixedHeadlines
    }

    fun getSyndFeed(url: String): SyndFeed {
        return input.build(StringReader(Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36")
                .ignoreContentType(true).get().html()))
    }

    fun getSources(category: SourceCategory? = null): List<NewsSource> = NewsSource.values().filter { if (category != null) it.getCategory() == category else true }

    fun getSource(id: String): NewsSource? = getSources().firstOrNull { it.id == id }

    fun getFeed(source: NewsFeed): List<SnippetContainer> = snippetCache[source]!!

    fun getSnippets(country: Country?): List<SnippetContainer> {
        return (if (country != null) snippetCache.filter { it.key.source.toNewsSource(octagon)?.country == country }
        else snippetCache).map { entry -> entry.value }.flatten()
    }

    /**
     * Insertion rate is represented by insertions/hour
     */
    fun getInsertionRate(start: Long = 0, end: Long = System.currentTimeMillis()): Float {
        return snippetCache.values.flatten().filter { it.snippet.insertionDate in start..end }.count() / ((end - start) / 1000 / 60 / 60f)
    }

    fun isSetup() = newsFeeds.size == r.tableList().count().run<Long>(octagon.conn).toInt()
}