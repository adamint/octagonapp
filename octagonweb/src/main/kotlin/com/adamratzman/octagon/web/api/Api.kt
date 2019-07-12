package com.adamratzman.octagon.web.api

import com.adamratzman.octagon.asPojo
import com.adamratzman.octagon.queryAsArrayList
import com.adamratzman.octagon.web.Octagon
import com.adamratzman.octagon.web.rest.ErrorResponse
import com.adamratzman.octagon.web.rest.methods.information
import com.adamratzman.octagon.web.rest.methods.search
import com.adamratzman.octagon.web.rest.methods.statistics
import com.adamratzman.octagon.web.rest.sources
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.rethinkdb.RethinkDB.r
import spark.Request
import java.security.SecureRandom
import java.util.*

class RandomString @JvmOverloads constructor(length: Int = 21, random: Random = SecureRandom(), symbols: String = alphanum) {
    private val random: Random
    private val symbols: CharArray
    private val buf: CharArray

    fun nextString(): String {
        for (idx in buf.indices)
            buf[idx] = symbols[random.nextInt(symbols.size)]
        return String(buf)
    }

    init {
        if (length < 1) throw IllegalArgumentException()
        if (symbols.length < 2) throw IllegalArgumentException()
        this.random = Objects.requireNonNull(random)
        this.symbols = symbols.toCharArray()
        this.buf = CharArray(length)
    }

    companion object {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = upper.toLowerCase(Locale.ROOT)
        val digits = "0123456789"
        val alphanum = upper + lower + digits
    }
}

class Api(val octagon: Octagon, val gson: Gson = GsonBuilder().serializeNulls().disableHtmlEscaping().create()) {
    val keyGenerator = RandomString()

    init {
        if (!r.dbList().run<List<String>>(octagon.conn).contains("octagon")) r.dbCreate("octagon").run<Any>(octagon.conn)
        val tableList = r.db("octagon").tableList().run<List<String>>(octagon.conn)
        if (!tableList.contains("users")) r.db("octagon").tableCreate("users")
                .optArg("primary_key", "key").runNoReply(octagon.conn)
        if (!tableList.contains("queries")) r.db("octagon").tableCreate("queries").runNoReply(octagon.conn)
        if (!tableList.contains("endpoint_log")) r.db("octagon").tableCreate("endpoint_log").runNoReply(octagon.conn)
    }

    fun getUsers(): List<User> = r.db("octagon").table("users").run<Any>(octagon.conn).queryAsArrayList(gson, User::class.java).filterNotNull()

    fun register(name: String, email: String, password: String, userType: UserType): User {
        val salt = nextSalt
        val user = User(name, email, Password(salt, hash(password.toCharArray(), salt)), generateKey(), userType)
        r.db("octagon").table("users").insert(r.json(gson.toJson(user))).runNoReply(octagon.conn)
        return user
    }

    fun checkKey(key: String): Int {
        val user = asPojo(gson, r.db("octagon").table("users").get(key).run(octagon.conn), User::class.java)
        if (user == null) return 1
        else if (!user.canQuery()) return 2
        return -1
    }

    fun generateKey(): String = keyGenerator.nextString()

    fun query(query: Request): Any {
        if (!octagon.restApi.isSetup()) return ErrorResponse(503, "The API is undergoing maintenance and will be up momentarily")
        val key = query.params("key")
        return when (checkKey(key)) {
            1 -> ErrorResponse(401, "You provided an invalid key")
            2 -> ErrorResponse(403, "You have reached the max queries for this month!")
            else -> {
                val user = asPojo(gson, r.db("octagon").table("users").get(key).run(octagon.conn), User::class.java)!!
                val type = query.params("type")
                when (type) {
                    "top-headlines" -> search(octagon, query, user, true)
                    "articles" -> search(octagon, query, user, false)
                    "sources" -> sources(octagon, query, user)
                    "statistics" -> statistics(octagon, user)
                    "information" -> information(octagon, query, user)
                    else -> ErrorResponse(404, "This query type was not found")
                }
            }
        }
    }
}