package com.adamratzman.octagon.sourcing.crawlers.saudi

import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.sourcing.agglomerator.CrawlerList
import com.adamratzman.octagon.sourcing.agglomerator.crawler

val saud1 = CrawlerList(
        crawler(NewsSource.ARGAAM, "http://gulf.argaam.com/rss/", "http://gulf.argaam.com/rss")
)