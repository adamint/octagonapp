package com.adamratzman.octagon.web

import com.adamratzman.octagon.web.rest.SnippetContainer
import com.google.gson.Gson
import com.rethinkdb.net.Cursor
import org.json.simple.JSONObject

fun List<SnippetContainer>.filter(start: Long, end: Long?) = this.filter { it.snippet.publishDate >= start && if (end != null) it.snippet.publishDate <= end else true }