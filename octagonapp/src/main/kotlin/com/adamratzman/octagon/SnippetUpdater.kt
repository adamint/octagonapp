package com.adamratzman.octagon

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import org.jsoup.Jsoup
import java.io.StringReader
import java.util.*

val input: SyndFeedInput = SyndFeedInput()

fun Octagon.updateSnippets() {
    p("Source Retrieval", "Retrieving source list from database")
    val dbList = r.dbList().run<List<String>>(conn)
    NewsSource.values().filter { dbList.contains(it.id) }.forEach { newsSource ->
        executor.execute {
            p("Source Retrieval", "Generating feeds for ${newsSource.readableName}")
            r.db(newsSource.id).tableList().run<List<String>>(conn).forEach { table ->
                executor.execute {
                    asPojo(gson, r.db(newsSource.id).table(table).get("information").run(conn), FeedInformation::class.java)?.let { feedInfo ->
                        updateFeedInfo(newsSource, Pair(table, feedInfo))
                    }
                }
            }
        }
    }
}

fun Octagon.updateFeedInfo(newsSource: NewsSource, pair: Pair<String, FeedInformation>) {
    pair.let { (table, feedInfo) ->
        p("Feed Update IP", "Starting update for $table (${newsSource.readableName})")
        val before = System.currentTimeMillis()
        val syndFeed = getSyndFeed(feedInfo.url, newsSource)
        p("Feed Update IP", "Retrieved feed for $table (${newsSource.readableName}) in ${System.currentTimeMillis() - before} ms")
        val feed = NewsFeed(newsSource.id.toUpperCase(), syndFeed.title, feedInfo.url, syndFeed.description, syndFeed.language
                ?: "en_US", syndFeed.publishedDate?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis(),
                id = "feed",
                headline = feedInfo.headline,
                globalHeadline = feedInfo.globalHeadline)

        var count = 0
        getSnippets(syndFeed, newsSource).forEach { snippet ->
            val snippetDoesntExist = r.db(newsSource.id).table(table).getAll(snippet.url).optArg("index", "url").isEmpty.run<Boolean>(conn)
            if (snippetDoesntExist) {
                r.db(newsSource.id).table(table).insert(r.json(gson.toJson(snippet))).runNoReply(conn)
                count++
            }
        }

        r.db(newsSource.id).table(table).get("feed").replace(r.json(gson.toJson(feed))).runNoReply(conn)

        p("Feed Update Done", if (count == 0) "Done | No change in ${feed.name}" else "Added $count snippets in ${feed.name} - ${feed.source} - ${Date().toLocaleString()}")
    }
}

fun getSnippets(feed: SyndFeed, source: NewsSource): List<Snippet> {
    val newFeedSnippet = feed.entries.mapNotNull { entry ->
        if (entry.link != null || entry.uri != null) {
            Snippet(entry.title.clean(), entry.author.clean(), entry.publishedDate?.time
                    ?: System.currentTimeMillis(), entry.description?.value?.let { Jsoup.parse(it).text().clean() },
                    entry.link?.clean()
                            ?: entry.uri.clean(), source.id.toUpperCase(), entry.categories?.map { it.name.clean() },
                    insertionDate = entry.publishedDate?.time ?: System.currentTimeMillis())
        } else null
    }.distinct().toMutableList()
    return newFeedSnippet
}


fun Octagon.getSyndFeed(url: String, source: NewsSource, first: Boolean = true): SyndFeed {
    return try {
        val feed = input.build(StringReader(Jsoup.connect(url).userAgent("Octagon | Open Source API").get().html()))
        when (source.id) {
            "the_west_au", "usa_today" -> {
                val title = feed.description
                feed.description = null
                feed.title = title
            }
        }
        feed
    } catch (e: Exception) {
        if (!first) throw Exception("Unable to parse $url - (${source.readableName})", e)
        getSyndFeed(url, source, false)
    }
}

fun p(category: String, obj: Any) = println("$category | $obj")
//fun get(url: String)