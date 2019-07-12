package com.adamratzman.octagon.web.rest.methods

import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.api.User
import com.adamratzman.octagon.web.rest.Statistics

fun statistics(octagon: Octagon, user: User): Statistics {
    return Statistics(octagon.restApi.getSources().size, octagon.restApi.getSources().groupBy { it.country }.mapValues { it.value.size },
            octagon.restApi.newsFeeds.size, octagon.restApi.snippetCache.values.map { it.size }.sum(),
            octagon.restApi.getInsertionRate(), octagon.restApi.getInsertionRate(start = System.currentTimeMillis() - (1000 * 60 * 60 * 24)),
            octagon.api.getUsers().size, user.queriesInRange.toLong(), user.totalQueries.toLong(),
            octagon.api.getUsers().map { it.totalQueries.toLong() }.sum())
}