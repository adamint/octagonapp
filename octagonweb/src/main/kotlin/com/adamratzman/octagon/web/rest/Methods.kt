package com.adamratzman.octagon.web.rest

import com.adamratzman.octagon.Country
import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.api.Query
import com.adamratzman.octagon.web.api.User
import spark.Request

fun sources(octagon: Octagon, request: Request, user: User): Any {
    try {
        val start = System.currentTimeMillis() - 1

        val countryStr = request.queryParams("country")
        val country = Country.values().firstOrNull { countryStr == it.abbrev }

        val items = (if (country != null) octagon.restApi.getSources().filter { it.country == country } else octagon.restApi.getSources())

        val total = items.size

        val latency = System.currentTimeMillis() - start

        val queriesLeft = user.addQuery(Query(System.currentTimeMillis(), request.url().removePrefix("https://")
                .removePrefix("http://").removePrefix(request.host()), latency, 1), octagon)

        return Response(if (latency < 50) QueryStatus.OKAY else QueryStatus.SLOW, latency.toInt(), items, -1, null,
                null, 0, total, country?.abbrev, queriesLeft)
    } catch (e: Exception) {
        e.printStackTrace()
        return Unit
    }
}

/*fun categories(octagon: Octagon, request: Request, user: User): Any {
    val source = request.queryParams("source")
            ?.let {
                octagon.restApi.getSource(it) ?: return ErrorResponse(400, "A news source with this ID was not found")
            }

}*/
