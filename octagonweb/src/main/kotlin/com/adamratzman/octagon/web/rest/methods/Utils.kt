package com.adamratzman.octagon.web.rest.methods

import com.adamratzman.octagon.Country
import com.adamratzman.octagon.NewsFeed
import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.SourceCategory
import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.rest.ErrorResponse
import com.adamratzman.octagon.web.rest.RestApi
import com.adamratzman.octagon.web.rest.SearchTerm
import spark.Request

fun Request.getLimit(allowInfinite: Boolean): Int {
    val limit = queryParams("limit")?.toIntOrNull()
    return when {
        limit == null -> 20
        limit in 1..50 -> limit
        limit <= 0 && allowInfinite -> -1
        else -> 20
    }
}

fun Request.getOffset(): Int {
    val offset = queryParams("offset")?.toIntOrNull()
    return if (offset == null) 0 else if (offset < 0) 0 else offset
}

fun Request.getCountry(): Any? {
    val country = queryParams("country")
    return if (country == null) null else Country.values().firstOrNull { it.abbrev == country }
            ?: ErrorResponse(400, "A country with this id was not found")
}

fun Request.getSourceTypes() = queryParams("source_types")?.split(",")
        ?.mapNotNull { id -> SourceCategory.values().firstOrNull { it.id == id } } ?: listOf()

fun Request.getCategories() = queryParams("categories")?.split(",")?.map { it.toLowerCase() } ?: listOf()

fun Request.getSources(octagon: Octagon): List<NewsSource> = queryParams("sources")?.split(",")
        ?.mapNotNull { id -> octagon.newsSources.firstOrNull { it.id == id } } ?: listOf()

fun Request.getExcludedSources(octagon: Octagon): List<NewsSource> = queryParams("excluded_sources")?.split(",")
        ?.mapNotNull { id -> octagon.newsSources.firstOrNull { it.id == id } } ?: listOf()

fun Request.getQuery(): List<SearchTerm> = queryParams("q")?.split(",")
        ?.distinct()?.map { SearchTerm(it, queryParams("ignore_case")?.toBoolean() ?: true) } ?: listOf()

fun Request.getAuthor(): List<String> = queryParams("authors")?.split(",")?.map { it.toLowerCase() }
        ?: listOf()

fun Request.getFeeds(restApi: RestApi): List<NewsFeed> = queryParams("feeds")?.split("*,")
        ?.mapNotNull {
            val feedIdSplit = it.split("*:")
            println(restApi.octagon.newsSources.firstOrNull { it.id == feedIdSplit.getOrNull(0) })
            restApi.octagon.newsSources.firstOrNull { it.id == feedIdSplit.getOrNull(0) }?.let { source ->
                restApi.newsFeeds.firstOrNull { it.id == feedIdSplit.getOrNull(1) && it.source.toLowerCase() == source.id }
            }
        } ?: listOf()

fun Request.getEndpoint() = url().removePrefix("https://")
        .removePrefix("http://").removePrefix(host())