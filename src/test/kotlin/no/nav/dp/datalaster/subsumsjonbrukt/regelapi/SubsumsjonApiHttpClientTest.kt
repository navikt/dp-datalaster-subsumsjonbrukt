package no.nav.dp.datalaster.subsumsjonbrukt.regelapi

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import de.huxhorn.sulky.ulid.ULID
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.net.URL

internal class SubsumsjonApiHttpClientTest {

    companion object {
        val server: WireMockServer = WireMockServer(WireMockConfiguration.options().dynamicPort())

        @BeforeAll
        @JvmStatic
        fun start() {
            server.start()
        }

        @AfterAll
        @JvmStatic
        fun stop() {
            server.stop()
        }
    }

    @BeforeEach
    fun configure() {
        WireMock.configureFor(server.port())
    }

    private val apiKey = "regelApiKey"

    @Test
    fun `Should get subsumsjon `() {
        val client = SubsumsjonApiHttpClient(URL(server.url("")), apiKey)
        val equalToPattern = EqualToPattern(apiKey)
        val id = ULID().nextULID()
        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("//subsumsjon/result/$id"))
                .withHeader("X-API-KEY", equalToPattern)
                .willReturn(
                    WireMock.aResponse()
                        .withBody("any")
                )
        )
        val subsumsjon = client.subsumsjon(SubsumsjonId(id))
        subsumsjon shouldBe "any"
    }

    @Test
    fun `Should not get subsumsjon on server error `() {
        val client = SubsumsjonApiHttpClient(URL(server.url("")), apiKey)
        val id = ULID().nextULID()
        val equalToPattern = EqualToPattern(apiKey)
        WireMock.stubFor(
            WireMock.get(WireMock.urlEqualTo("//subsumsjon/result/$id"))
                .withHeader("X-API-KEY", equalToPattern)
                .willReturn(
                    serverError()
                )
        )
        shouldThrow<SubsumsjonClientException> { client.subsumsjon(SubsumsjonId(id)) }
    }
}