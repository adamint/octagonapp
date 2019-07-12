package com.adamratzman.octagon.sourcing.crawlers.au

import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.sourcing.agglomerator.CrawlerList
import com.adamratzman.octagon.sourcing.agglomerator.crawler

val aus2 = CrawlerList(
        crawler(NewsSource.THE_CONVERSATION_AU, "https://theconversation.com/au/", "https://theconversation.com/au/feeds")
)
