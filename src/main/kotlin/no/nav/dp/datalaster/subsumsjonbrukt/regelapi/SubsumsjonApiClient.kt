package no.nav.dp.datalaster.subsumsjonbrukt.regelapi

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import de.huxhorn.sulky.ulid.ULID
import mu.KotlinLogging
import no.nav.dp.datalaster.subsumsjonbrukt.moshiInstance
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.net.URL

interface SubsumsjonApiClient {
    fun subsumsjon(subsumsjonId: SubsumsjonId): String
}

data class SubsumsjonId(val id: String) {
    init {
        try {
            ULID.parseULID(id)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("ID $id is not a valid ULID")
        }
    }

    companion object Mapper {
        private val LOGGER = KotlinLogging.logger { }
        private val jsonAdapter = moshiInstance.adapter<Map<String, String>>(Map::class.java)
        fun fromJson(json: String): SubsumsjonId? {
            return runCatching {
                jsonAdapter.fromJson(json)?.get("id")?.let { SubsumsjonId(it) }
                    ?: throw IllegalArgumentException("Failed to get id from payload: $json")
            }.onFailure { e -> LOGGER.warn("Failed to convert json string to object", e) }.getOrNull()
        }
    }
}

class SubsumsjonApiHttpClient(private val regelApiUrl: URL, private val apiKey: String) : SubsumsjonApiClient {

    override fun subsumsjon(subsumsjonId: SubsumsjonId): String {

        val jsonData = "${regelApiUrl.toURI().toASCIIString()}/behov"
        val (_, response, result) = with(
            jsonData.httpGet()
                .apiKey(apiKey)
        ) {
            responseString()
        }
        return when (result) {
            is Result.Failure -> throw RuntimeException(
                "Failed to fetch subsumsjon. Response message ${response.responseMessage}. Error message: ${result.error.message}"
            )
            is Result.Success -> response.body().asString("application/json")
        }
    }
}

internal fun Request.apiKey(apiKey: String) = this.header("X-API-KEY", apiKey)