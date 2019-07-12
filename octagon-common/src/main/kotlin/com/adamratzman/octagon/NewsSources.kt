package com.adamratzman.octagon

data class SourceData(val name: String, val id: String, val rssBase: String, val rssList: String,
                      val headlineName: String?, val globalHeadline: Boolean, val headline: Boolean,
                      val country: Country = Country.USA, val newsSource: NewsSource = NewsSource.values().first { id == it.id })

enum class NewsSource(val readableName: String, _id: String, val country: Country = Country.USA, val parent: NewsSource? = null) {
    // Qatar
    AL_JAZEERA("Al Jazeera", "al_jazeera"),

    // USA
    THE_WASHINGTON_POST("The Washington Post", "the_washington_post"),
    THE_NEW_YORK_TIMES("The New York Times", "nyt"),
    THE_WALL_STREET_JOURNAL("The Wall Street Journal", "wsj"),
    CNN("CNN", "cnn"),
    CNN_MONEY("CNN Money", "money", parent = CNN),
    ABC_NEWS("ABC News", "abc_news"),
    AL_JAZEERA_ENGLISH("Al Jazeera English", "english", parent = AL_JAZEERA),

    THE_CONVERSATION("The Conversation", "the_conversation"),
    
    THE_CONVERSATION_US("The Conversation (US)", "us", parent = THE_CONVERSATION),
    ARS_TECHNICA("Ars Technica", "ars_technica"),

    // Australia
    NEWS_COM_AU("News.com.au", "news_com_au", Country.AUSTRALIA),
    THE_SYDNEY_MORNING_HERALD("The Sydney Morning Herald", "sydney_morning_herald", Country.AUSTRALIA),
    THE_AGE("The Age", "the_age", Country.AUSTRALIA),
    HERALD_SUN("Herald Sun", "herald_sun", Country.AUSTRALIA),
    THE_DAILY_TELEGRAPH_AU("The Daily Telegraph (AU)", "the_daily_telegraph_au", Country.AUSTRALIA),
    THE_WEST_AUSTRALIA("The West (AU)", "the_west_au", Country.AUSTRALIA),
    ABC_NEWS_AU("ABC News (AU)", "abc_news_au", Country.AUSTRALIA),
    ABC_NEWS_AU_RURAL("ABC News Rural (AU)", "rural", parent = ABC_NEWS_AU),
    NINE_NEWS("9News", "nine_news"),

    THE_CONVERSATION_AU("The Conversation (AU)", "au", Country.AUSTRALIA, parent = THE_CONVERSATION),

    // Norway
    AFTENPOSTEN("Aftenposten", "aftenposten", Country.NORWAY),

    // Italy
    ANSA("ANSA", "ansa", Country.ITALY),

    // Saudi Arabia
    ARGAAM("Argaam", "argaam", Country.SAUDI_ARABIA)
    ;

    override fun toString() = readableName
    val id: String = parent?.let { "${it.id}_$_id" } ?: _id

    fun getCategory(): SourceCategory = when (this.id) {
        "wsj" -> SourceCategory.BUSINESS
        "cnn_money" -> SourceCategory.BUSINESS

        else -> SourceCategory.GENERAL
    }

    fun getSnippetId() = this.id.toUpperCase()
}

enum class SourceCategory(val id: String) { GENERAL("general"), BUSINESS("business"), ENTERTAINMENT("entertainment") }

enum class Country(val abbrev: String) {
    USA("us"),
    AUSTRALIA("au"),
    NORWAY("no"),
    ITALY("it"),
    SAUDI_ARABIA("sa"),

}