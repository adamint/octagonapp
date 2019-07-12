package com.adamratzman.octagon.agglomerator

import com.adamratzman.octagon.Octagon
import com.rethinkdb.net.Connection
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

/*class Agglomerator(val octagon: Octagon, vararg crawlerArray: Crawler,val conn: Connection = octagon.conn) {
    val crawlerFactory = CrawlerFactory(ConcurrentLinkedQueue(crawlerArray.toList()))
    val executor = Executors.newScheduledThreadPool(2)
    val cachedExecutor = Executors.newCachedThreadPool()
    fun add(crawler: Crawler): Agglomerator {
        crawlerFactory.add(crawler)
        return this
    }
}*/