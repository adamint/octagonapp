package com.adamratzman.octagon

import java.util.*

data class NewsFeed(val source: String, val name: String, val url: String, val description: String?,
                    val language: String, var published: Long? = null,
                    var id: String = name.toLowerCase().replace(" ", "_")
                            .filter { it.isLetterOrDigit() || it == '_' },
                    val headline: Boolean = false, val globalHeadline: Boolean) {

    override fun equals(other: Any?): Boolean {
        return other is NewsFeed && other.source == source && other.id == id
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + language.hashCode()
        result = 31 * result + (published?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        result = 31 * result + headline.hashCode()
        result = 31 * result + globalHeadline.hashCode()
        return result
    }
}

data class Snippet(val title: String, var author: String?, val publishDate: Long, val description: String?,
                   val url: String, val source: String, val categories: List<String>?, val type: String = "snippet",
                   val insertionDate: Long = System.currentTimeMillis())

data class Article(val snippet: Snippet, val text: String, val lastEditedAt: Date?)

data class FeedInformation(val url: String, val published: Long?, val type: String = "information",
                           val id: String = "information", val headline: Boolean = false,
                           val globalHeadline: Boolean)

enum class Subject(val readable: String) {
    POLITICS("politics"), BUSINESS("business"),
    ENTERTAINMENT("entertainment"), SPORTS("sports"),
    COMIC("comic"), OTHER("other"), REAL_ESTATE("real estate"),
    LIFESTYLE("lifestyle"), OPINIONS("opinions"), LOCAL("local"),
    GENERAL_NEWS("general news"), FAITH("faith"), WEATHER("weather")
    ;

    override fun toString() = readable
}

enum class NewsScope(val readable: String) {
    WORLD("world"), NATIONAL("national"), LOCAL("local"),
    OTHER("other")
    ;

    override fun toString() = readable
}