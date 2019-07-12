package com.adamratzman.octagon.sourcing.crawlers.us

import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.sourcing.agglomerator.CrawlerList
import com.adamratzman.octagon.sourcing.agglomerator.crawler

val us1 = CrawlerList(
        crawler(NewsSource.THE_WASHINGTON_POST, "http://feeds.washingtonpost.com/rss",
                "https://www.washingtonpost.com/rss-feeds/2014/08/04/ab6f109a-1bf7-11e4-ae54-0cfe1f974f8a_story.html?noredirect=on&utm_term=.2c5722ec064b#",
                headlineName = "National", globalHeadline = true),
        crawler(NewsSource.ABC_NEWS, "http://feeds.abcnews.com/abcnews/", "http://abcnews.go.com/Site/page/rss--3520115",
                headlineName = "ABC News: Top Stories", globalHeadline = true),
        crawler(NewsSource.CNN, "http://rss.cnn.com/rss/", "http://www.cnn.com/services/rss/",
                headlineName = "CNN.com - RSS Channel - HP Hero", globalHeadline = true),
        crawler(NewsSource.CNN_MONEY, "http://rss.cnn.com/rss/", "http://money.cnn.com/services/rss/",
                headlineName = "Business and financial news - CNNMoney.com", globalHeadline = true),
        crawler(NewsSource.AL_JAZEERA_ENGLISH, "", "", feeds = listOf("https://www.aljazeera.com/xml/rss/all.xml")),
        crawler(NewsSource.THE_WALL_STREET_JOURNAL, "/feed", "http://www.wsj.com/public/page/rss_news_and_feeds_blogs.html"),
        crawler(NewsSource.THE_NEW_YORK_TIMES, "nytimes.com", "https://archive.nytimes.com/www.nytimes.com/services/xml/rss/index.html?8dpc", headlineName = "NYT > Home Page", globalHeadline = true),
        crawler(NewsSource.THE_WALL_STREET_JOURNAL, "/xml/rss/", "http://www.wsj.com/public/page/rss_news_and_feeds.html",
                headlineName = "WSJ.com: World News", globalHeadline = true)
        )