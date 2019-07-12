package com.adamratzman.octagon.sourcing.crawlers.us

import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.sourcing.agglomerator.CrawlerList
import com.adamratzman.octagon.sourcing.agglomerator.crawler

val us2 = CrawlerList(
        crawler(NewsSource.THE_CONVERSATION_US, "https://theconversation.com/us/", "https://theconversation.com/us/feeds"),
        crawler(NewsSource.ARS_TECHNICA, "feeds.arstechnica.com", "https://arstechnica.com/rss-feeds/")
)