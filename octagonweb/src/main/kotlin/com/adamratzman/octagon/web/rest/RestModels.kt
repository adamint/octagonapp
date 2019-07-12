package com.adamratzman.octagon.web.rest

import com.adamratzman.octagon.Country
import com.adamratzman.octagon.NewsFeed
import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.Snippet
import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.r

data class Response<out T>(val status: QueryStatus, val latency_ms: Int, val items: List<T>, val limit: Int, val next: String?,
                           val previous: String?, val offset: Int = 0, val total: Int, val country: String?,
                           val queries_left: Int)

enum class QueryStatus {
    OKAY, UNAVAILABLE, SLOW;

    override fun toString() = name.toLowerCase()
}

data class ErrorResponse(val code: Int, val message: String)

data class Statistics(val total_sources: Int, val sources_per_country: Map<Country, Int>, val total_feeds: Int, val total_articles_cached: Int,
                      val total_insertion_rate: Float, val day_insertion_rate: Float, val users: Int,
                      val your_queries_this_month: Long, val your_total_queries: Long, val global_total_queries: Long)

data class SearchTerm(val term: String, val ignoreCase: Boolean)

data class SnippetContainer(val snippet: Snippet, val feed: NewsFeed)

data class NewsSourceInformation(val source: NewsSource, val feed_size: Int, val feeds: List<String>, val article_count: Int)

data class NewsFeedInformation(val feed: NewsFeed, val source_url: String, val article_count: Int)

data class SanitizedSnippet(val title: String, val author: String?, val published_at: Long, val description: String?,
                            val link: String, val source: NewsSource, val feed_id: String)

fun Snippet.toSanitizedSnippet(octagon: Octagon, feed: NewsFeed): SanitizedSnippet? {
    val newsSource = octagon.getSource(source)
    return if (newsSource == null) {
        r.db(feed.source).table(feed.id).optArg("url", url).delete().runNoReply(octagon.conn)
        null
    } else SanitizedSnippet(title,
            if (author?.isEmpty() == true) null else author?.toLowerCase()?.split(" ")?.joinToString(" ") { if (it == "and") it else it.capitalize() },
            publishDate,
            if (description?.isEmpty() == true) null else description,
            url,
            newsSource,
            "${newsSource.id}*:${feed.id}"
    )
}
