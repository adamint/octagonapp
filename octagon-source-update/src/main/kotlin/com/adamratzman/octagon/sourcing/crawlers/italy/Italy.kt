package com.adamratzman.octagon.sourcing.crawlers.italy

import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.sourcing.agglomerator.CrawlerList
import com.adamratzman.octagon.sourcing.agglomerator.crawler

val ita1 = CrawlerList(
        crawler(NewsSource.ANSA, "rss.xml", "https://www.ansa.it/sito/static/ansa_rss.html",
                headlineName = "RSS di - ANSA.it")
)