package com.adamratzman.octagon.sourcing.crawlers.au

import com.adamratzman.octagon.NewsSource
import com.adamratzman.octagon.sourcing.agglomerator.CrawlerList
import com.adamratzman.octagon.sourcing.agglomerator.crawler

val aus1 = CrawlerList(
        crawler(NewsSource.NEWS_COM_AU, "https://www.news.com.au/content-feeds/", "https://www.news.com.au/more-information/rss-feeds",
                headlineName = "RSS news.com.au — Australia’s #1 news site | World News"),
        crawler(NewsSource.THE_SYDNEY_MORNING_HERALD, "https://www.smh.com.au/rss/", "https://www.smh.com.au/rssheadlines",
                headlineName = "Sydney Morning Herald - National"),
        crawler(NewsSource.THE_AGE, "https://www.theage.com.au/rss/", "https://www.theage.com.au/rssheadlines",
                headlineName = "The Age - National"),
        crawler(NewsSource.HERALD_SUN, "/rss", "http://www.heraldsun.com.au/help-rss", headlineName = "Herald Sun"),
        crawler(NewsSource.THE_DAILY_TELEGRAPH_AU, "/rss", "https://www.dailytelegraph.com.au/help-rss",
                headlineName = "Breaking News &#124; Daily Telegraph"),
        crawler(NewsSource.THE_WEST_AUSTRALIA, "/rss", "https://thewest.com.au/rss-feeds", headlineName = "The West Australian"),
        crawler(NewsSource.ABC_NEWS_AU, "/rss.xml", "https://adamratzman.com/uploads/octagon/rss-urls.pdf",
                headlineName = "Top Stories", globalHeadline = true, feeds = listOf(
                "http://www.abc.net.au/news/feed/46182/rss.xml",
                "http://www.abc.net.au/local/rss/sydney/news.xml",
                "http://www.abc.net.au/news/feed/1534/rss.xml",
                "http://www.abc.net.au/radionational/feed/3727018/rss.xml",
                "http://www.abc.net.au/radionational/feed/2884582/rss.xml",
                "http://www.abc.net.au/news/feed/51892/rss.xml",
                "http://www.abc.net.au/news/feed/45924/rss.xml"
        )),
        crawler(NewsSource.ABC_NEWS_AU_RURAL, "https://www.abc.net.au/news/feed", "https://www.abc.net.au/news/rural/rss/"),
        crawler(NewsSource.NINE_NEWS, "", "", headlineName = "9News", feeds = listOf("https://www.9news.com.au/rss"))
)