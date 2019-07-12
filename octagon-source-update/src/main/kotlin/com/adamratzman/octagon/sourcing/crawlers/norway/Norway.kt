package com.adamratzman.octagon.sourcing.crawlers.norway

import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.sourcing.agglomerator.CrawlerList
import com.adamratzman.octagon.sourcing.agglomerator.crawler

val no1 = CrawlerList(
        crawler(NewsSource.AFTENPOSTEN,"", "", feeds = listOf("https://www.aftenposten.no/rss"))
)