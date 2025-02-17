package ireader.koreanonline

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.ireader.core.*
import org.ireader.core_api.source.Dependencies
import org.ireader.core_api.source.model.*
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import tachiyomix.annotations.Extension

@Extension
abstract class KoreanOnline(deps: Dependencies) : ParsedHttpSource(deps) {

    override val name = "KoreanMtl.Online"


    override val id: Long
        get() = 14204738993432853
    override val baseUrl = "https://www.koreanmtl.online"

    override val lang = "en"

    override fun getListings(): List<Listing> {
        return listOf(
               LatestListing()
        )
    }

    override fun getFilters(): FilterList {
        return listOf(
                Filter.Title(),
                Filter.Sort(
                        "Sort By:",arrayOf(
                        "Latest",
                )),
        )
    }

    override suspend fun getMangaList(sort: Listing?, page: Int): MangasPageInfo {
        return getLatest(page)
    }

    override suspend fun getMangaList(filters: FilterList, page: Int): MangasPageInfo {
        val sorts = filters.findInstance<Filter.Sort>()?.value?.index
        val query = filters.findInstance<Filter.Title>()?.value
        if (!query.isNullOrBlank()) {
            return getSearch(page,query)
        }
        return when(sorts) {
            0 -> getLatest(page)
            else -> getLatest(page)
        }
    }

    suspend fun getLatest(page: Int) : MangasPageInfo {
        val res = requestBuilder("$baseUrl/p/novels-listing.html")
        return bookListParse(client.get(res).asJsoup(),"ul.a li.b",null) { latestFromElement(it) }
    }
    suspend fun getSearch(page: Int,query: String) : MangasPageInfo {
        val res = requestBuilder("$baseUrl/p/novels-listing.html")
        return bookListParse(client.get(res).asJsoup(),"ul.a li.b",null) { latestFromElement(it) }
    }



    override fun HttpRequestBuilder.headersBuilder() {
        headers {
            append(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36")
            append(HttpHeaders.CacheControl, "max-age=0")
            append(HttpHeaders.Referrer, baseUrl)
        }
    }


    fun latestFromElement(element: Element): MangaInfo {
        val title = element.select("a").text()
        val url = element.select("a").attr("href")

        return MangaInfo(key = url, title = title)
    }


    // manga details
    override fun detailParse(document: Document): MangaInfo {
        val description = document.select("div.post-body p").eachText().joinToString("\n")

        return MangaInfo(
            title = "",
            key = "",
            description = description,
        )
    }


    // chapters
    override fun chaptersRequest(book: MangaInfo): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(book.key)
            headers { headers }
        }
    }

    override fun chaptersSelector() = "div.post-body ul.a li.a"

    override fun chapterFromElement(element: Element): ChapterInfo {
        val link = baseUrl + element.select("a").attr("href").substringAfter(baseUrl)
        val name = element.select("a").text()

        return ChapterInfo(name = name, key = link)
    }


    override suspend fun getChapterList(manga: MangaInfo): List<ChapterInfo> {
        val request = client.get(chaptersRequest(manga)).asJsoup()
        return chaptersParse(request)
    }


    override fun pageContentParse(document: Document): List<String> {
        return document.select("h1,p").eachText()
    }

    override suspend fun getContents(chapter: ChapterInfo): List<String> {
        return pageContentParse(client.get(contentRequest(chapter)).asJsoup())
    }


    override fun contentRequest(chapter: ChapterInfo): HttpRequestBuilder {
        return HttpRequestBuilder().apply {
            url(chapter.key)
            headers { headers }
        }
    }


}