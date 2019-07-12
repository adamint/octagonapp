package com.adamratzman.octagon.web.api

import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.r
import com.adamratzman.octagon.web.rest.businessQueryLimitPerMonth
import com.adamratzman.octagon.web.rest.individualQueryLimitPerMonth
import java.util.*

data class User(var name: String, var email: String, val password: Password, var key: String, var type: UserType,
                val queries: MutableList<Query> = mutableListOf()) {
    val queriesInRange: Int
        get() {
            val cal = GregorianCalendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val beginning = cal.timeInMillis

            if (cal.get(GregorianCalendar.MONTH) == GregorianCalendar.DECEMBER) cal.roll(GregorianCalendar.YEAR, true)
            cal.roll(GregorianCalendar.MONTH, true)
            val end = cal.timeInMillis

            return queries.filter { it.time in beginning..end }.sumBy { it.multiplier }
        }

    val totalQueries: Int
        get() = queries.sumBy { it.multiplier }

    fun canQuery(): Boolean {
        return if (type == UserType.COMMUNITY) queriesInRange <= 1000 else queriesInRange <= 250000
    }

    fun getQueryLimit(): Int = if (type == UserType.COMMUNITY) individualQueryLimitPerMonth else businessQueryLimitPerMonth

    /**
     * Add a query to the user
     */
    fun addQuery(query: Query, octagon: Octagon): Int {
        queries.add(query)
        r.db("octagon").table("users").get(key).update(r.json(octagon.gson.toJson(this))).run<Any>(octagon.conn)
        return getQueryLimit() - queriesInRange
    }
}

data class Query(val time: Long, val endpoint: String, val latency: Long, val multiplier: Int)
enum class UserType { COMMUNITY, BUSINESS }