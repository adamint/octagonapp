package com.adamratzman.octagon.web.rest.methods

import com.adamratzman.octagon.Country
import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.api.Query
import com.adamratzman.octagon.web.api.User
import com.adamratzman.octagon.web.filter
import com.adamratzman.octagon.web.rest.ErrorResponse
import com.adamratzman.octagon.web.rest.QueryStatus
import com.adamratzman.octagon.web.rest.Response
import com.adamratzman.octagon.web.rest.toSanitizedSnippet
import com.adamratzman.octagon.web.toNewsSource
import spark.Request

/**
 * Getting top headlines, by default sorted by insertion date (descending).
 *
 * Sorting is possible by insertion date (ascending or descending) or, when a query is present, by mentions_i.
 *
 * If a query is present, if the *ignore_case* parameter is left null, it is set to true, else as user defines.
 * In the future, let user allow case ignore either globally or per-term with *ignore_case_i*, where i represents the index of the
 * search term.
 *
 * @param source_types potentially empty list of [SourceCategory] ids
 * @param sources sources **to** include
 * @param excluded_sources source to **not** include
 * @param country possibly null [Country] id. If null, results from all countries are displayed
 * @param limit the amount you want to return. -1 if all results are to be returned. Query cost is 20 if all results are to be returned, else 1
 * @param offset the amount to offset. Default is 0.
 * @param categories possibly empty list of categories that snippets should contain
 * @param q possibly empty list of search terms to use. Case sensitivity decided by..
 * @param ignore_case defaults to true. boolean that decides if the search term should ignore case
 * @param sort method of sorting, descending by publish date by default. Allows ascending or, if search terms are present, mentions_i where
 * i is the index of the associated search term
 * @param feeds possibly empty list of **feed ids** separated with __*,__ - format is sourceId*:feedId
 *
 */
fun search(octagon: Octagon, request: Request, user: User, headlines: Boolean): Any {
    try {
        val before = System.currentTimeMillis() - 1

        val limit = request.getLimit(true)
        val offset = request.getOffset()
        val country = request.getCountry()

        if (country is ErrorResponse) return country
        country as Country?

        val categories = request.getCategories()
        val sourceTypes = request.getSourceTypes()
        val newsSources = request.getSources(octagon)
        val excludedSources = request.getExcludedSources(octagon)
        val feeds = request.getFeeds(octagon.restApi)
        var sources = if (headlines) octagon.restApi.getHeadlineArticles(country) else octagon.restApi.getSnippets(country)
        if (feeds.isNotEmpty()) {
            sources = sources.filter { feeds.map { it.id }.contains(it.feed.id) }
        }

        // Either news sources can be requested OR a country can. Having oth would make no sense.
        if (newsSources.isNotEmpty()) sources = sources.filter { newsSources.map { it.getSnippetId() }.contains(it.feed.source) }
        if (excludedSources.isNotEmpty()) sources = sources.filter { !excludedSources.map { it.getSnippetId() }.contains(it.feed.source) }

        if (categories.isNotEmpty()) sources = sources.filter {
            it.snippet.categories != null && it.snippet.categories!!.map { it.toLowerCase() }.containsAll(categories)
        }
        if (sourceTypes.isNotEmpty()) sources = sources.filter { sourceTypes.contains(it.feed.source.toNewsSource(octagon)?.getCategory()) }

        val searchTerms = request.getQuery()
        if (searchTerms.isNotEmpty()) sources = sources.filter { container ->
            var candidate = true
            val text = container.snippet.title + " " + container.snippet.description
            searchTerms.forEach { searchTerm ->
                if (!(if (searchTerm.ignoreCase) text.toLowerCase().contains(searchTerm.term.toLowerCase())
                        else text.contains(searchTerm.term))) candidate = false
            }
            candidate
        }

        sources = sources.distinctBy { it.snippet.title }

        val start = request.queryParams("start")?.toLongOrNull()
        if (start != null) {
            val end = request.queryParams("end")?.toLongOrNull()
            if (end != null && end < start) return ErrorResponse(400, "The end time cannot be before the start time!")
            else sources = sources.filter(start, end)
        }

        if (sources.size < offset) return ErrorResponse(400, "The specified offset is greater than the total cached headlines")

        val total = if (sources.size < offset + limit) sources.size - offset else limit

        // Sort
        val sortBy = request.queryParams("sort")
        when (sortBy) {
            null, "descending" -> sources = sources.sortedByDescending { it.snippet.publishDate }
            "ascending" -> sources = sources.sortedBy { it.snippet.publishDate }
            else -> {
                searchTerms.getOrNull(sortBy.removePrefix("mentions_").toIntOrNull() ?: -1)?.let { term ->
                    sources = sources.sortedBy { source ->
                        var mentions = 0
                        val combined = (source.snippet.title + " " + source.snippet.description)
                        val chars = combined.toCharArray()
                        for (i in 0..(chars.size - term.term.length - 1)) {
                            if (combined.substring(i, i + term.term.length).equals(term.term, term.ignoreCase)) mentions++
                        }
                        mentions
                    }
                }
            }
        }

        val items = (if (limit != -1) sources.subList(offset, offset + total) else sources).map { it.snippet.toSanitizedSnippet(octagon, it.feed) }
        val next = when {
            limit == -1 -> null
            sources.size >= limit * 2 + offset -> "/api/${request.params("key")}/${request.params("type")}" +
                    "?offset=${offset + limit}&limit=$limit" + (if (country != null) "&country=${country.abbrev}" else "")
            else -> null
        }
        val previous = when {
            limit == -1 -> null
            offset - limit >= 0 -> "/api/${request.params("key")}/${request.params("type")}?offset=${offset - limit}" +
                    "&limit=$limit" + (if (country != null) "&country=${country.abbrev}" else "")
            else -> null
        }

        val latency = System.currentTimeMillis() - before

        val queriesLeft = user.addQuery(Query(System.currentTimeMillis(), request.getEndpoint(),
                latency, if (limit == -1) 20 else 1), octagon)

        return Response(if (latency < 50) QueryStatus.OKAY else QueryStatus.SLOW, latency.toInt(), items, limit, next,
                previous, offset, if (limit == -1) items.size else total, country?.abbrev, queriesLeft)
    } catch (e: Exception) {
        e.printStackTrace()
        return Unit
    }
}
