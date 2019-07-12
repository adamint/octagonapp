package com.adamratzman.octagon.web.rest.methods

import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.api.Query
import com.adamratzman.octagon.web.api.User
import com.adamratzman.octagon.web.rest.*
import com.adamratzman.octagon.web.toNewsSource
import spark.Request

fun information(octagon: Octagon, request: Request, user: User): Any {
    val before = System.currentTimeMillis()
    val source = request.queryParams("source_id")?.let { id -> octagon.newsSources.firstOrNull { it.id == id } }

    if (source != null) {
        val sourceCache = octagon.restApi.snippetCache.filter { it.key.source.toNewsSource(octagon) == source }
        val latency = System.currentTimeMillis() - before
        return Response(if (latency < 50) QueryStatus.OKAY else QueryStatus.SLOW,
                latency.toInt(), listOf(NewsSourceInformation(source,
                octagon.restApi.newsFeeds.filter { it.source.toNewsSource(octagon) == source }.count(),
                sourceCache.map { "${source.id}*:${it.key.id}" }, sourceCache.map { it.value.size }.sum())),
                -1, null, null, 0, 1, null,
                user.addQuery(Query(System.currentTimeMillis(), request.getEndpoint(), latency, 1), octagon))
    }

    val feeds = request.queryParams("feed_ids")?.split("*,")?.mapNotNull { id ->
        val split = id.split("*:")
        if (split.size != 2) null
        else {
            octagon.newsSources.firstOrNull { it.id == split[0] }?.let { source ->
                octagon.restApi.newsFeeds.firstOrNull { it.source.toNewsSource(octagon) == source && it.id == split[1] }
            }
        }
    }

    if (feeds?.isNotEmpty() == true) {
        val feedsInformation = feeds.map {
            NewsFeedInformation(it, "/api/${user.key}/information?source_id=${it.source.toNewsSource(octagon)}", octagon.restApi.getFeed(it).size)
        }
        val latency = System.currentTimeMillis() - before
        return Response(if (latency < 50) QueryStatus.OKAY else QueryStatus.SLOW,
                latency.toInt(), feedsInformation, -1, null, null, 0, feedsInformation.size, null,
                user.addQuery(Query(System.currentTimeMillis(), request.getEndpoint(), latency, 1), octagon))
    }

    return ErrorResponse(400, "Neither a source nor a news feed was requested")
}