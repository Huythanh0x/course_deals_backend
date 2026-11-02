package com.thanh0x.coursedeal.crawlerrunner.fetcher

import com.thanh0x.coursedeal.config.logger
import com.thanh0x.coursedeal.utils.UrlValidator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * A utility class for fetching and parsing web content.
 */
class WebContentFetcher {
    private val log = logger()

    /**
     * Retrieves a JSONArray object from a specified URL.
     *
     * @param urlString the URL from which to retrieve the JSON array
     * @return a JSONArray object, or null if the request failed
     */
    fun getJsonArrayFrom(urlString: String): JSONArray? {
        val rawJson = fetchSafeContent(urlString) ?: return null
        return try {
            JSONArray(rawJson)
        } catch (e: JSONException) {
            log.warn("Error parsing JSON array from {}: {}", urlString, e.message)
            null
        }
    }

    /**
     * Retrieves the raw HTML content from a specified URL and parses it into an HTML Document using Jsoup.
     *
     * @param urlString the URL of the webpage to retrieve HTML content from
     * @return an HTML Document, or null if the request failed
     */
    fun getHtmlDocumentFrom(urlString: String): Document? {
        val rawHtml = fetchSafeContent(urlString) ?: return null
        return Jsoup.parse(rawHtml)
    }

    companion object {
        private val log = logger()
        private const val REQUEST_TIMEOUT_SECONDS = 10L

        /**
         * Retrieves a JSONObject from a given URL.
         *
         * @param urlString the URL from which to retrieve the JSON object
         * @return the JSONObject, or null if the request failed or timed out
         */
        @JvmStatic
        fun getJsonObjectFrom(urlString: String): JSONObject? {
            val content = fetchSafeContent(urlString) ?: return null
            return try {
                JSONObject(content)
            } catch (e: JSONException) {
                log.warn("Error parsing JSON from {}: {}", urlString, e.message)
                null
            }
        }

        /**
         * Validates the URL and fetches its raw content, logging and returning null if either
         * the URL is unsafe or the fetch produced no content.
         */
        private fun fetchSafeContent(urlString: String): String? {
            if (!UrlValidator.isSafeUrl(urlString)) return null

            val content = getRawHTMLContentFrom(urlString)
            return if (content.isNullOrBlank()) {
                log.warn("Failed to fetch content from {}, returning null", urlString)
                null
            } else {
                content
            }
        }

        /**
         * Fetches the raw HTML content from a given URL.
         *
         * @param urlString the URL from which to fetch the raw HTML content
         * @return the raw HTML content as a String
         */
        @JvmStatic
        fun getRawHTMLContentFrom(urlString: String): String? {
            val client =
                HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build()
            val request =
                HttpRequest.newBuilder()
                    .uri(URI.create(urlString))
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                    )
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .build()

            return try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                response.body()
            } catch (e: IOException) {
                log.warn("Error fetching content from {}", urlString, e)
                null
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Interrupted while fetching content from {}", urlString, e)
                null
            }
        }
    }
}
