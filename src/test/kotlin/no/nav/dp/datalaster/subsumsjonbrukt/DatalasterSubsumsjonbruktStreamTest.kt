package no.nav.dp.datalaster.subsumsjonbrukt

import de.huxhorn.sulky.ulid.ULID
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiClient
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonClientException
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonId
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TestInputTopic
import org.apache.kafka.streams.TestOutputTopic
import org.apache.kafka.streams.TopologyTestDriver
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class DatalasterSubsumsjonbruktStreamTest {

    companion object {
        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    @Test
    fun `Should not be able to process record with malformed json`() {
        val regelApiClient = mockk<SubsumsjonApiClient>()
        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, mockk())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            topologyTestDriver.subsumsjonInputTopic().also { it.pipeInput("bad json") }
            assertTrue { topologyTestDriver.subsumsjonOutputTopic().isEmpty }
        }

        verify(exactly = 0) { regelApiClient.subsumsjon(any()) }
    }

    @Test
    fun `Should not be able to process record with json missing id`() {
        val regelApiClient = mockk<SubsumsjonApiClient>()
        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, mockk())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            val json =
                """
                { "aname" : "aname" }
                """.trimIndent()
            topologyTestDriver.subsumsjonInputTopic().also { it.pipeInput(json) }
            assertTrue { topologyTestDriver.subsumsjonOutputTopic().isEmpty }
        }

        verify(exactly = 0) { regelApiClient.subsumsjon(any()) }
    }

    @Test
    fun `Should not be able to process record with json with an unrecognized id`() {
        val regelApiClient = mockk<SubsumsjonApiClient>()
        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, mockk())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            val json =
                """
                 { "id" : "blabla" }
                """.trimIndent()
            topologyTestDriver.subsumsjonInputTopic().also { it.pipeInput(json) }
            assertTrue { topologyTestDriver.subsumsjonOutputTopic().isEmpty }
        }

        verify(exactly = 0) { regelApiClient.subsumsjon(any()) }
    }

    @Test
    fun `Should be able to process record with json with an recognized id`() {
        val subsumsjonId = SubsumsjonId(ULID().nextULID())
        val regelApiClient = mockk<SubsumsjonApiClient>().apply {
            every { this@apply.subsumsjon(subsumsjonId) } returns """{"any":"any"}"""
        }

        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, mockk())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            val json =
                """
                { "id" : "${subsumsjonId.id}" }
                """.trimIndent()
            topologyTestDriver.subsumsjonInputTopic().also {
                it.pipeInput(json)
                assertFalse { topologyTestDriver.subsumsjonOutputTopic().isEmpty }
            }
        }

        verify(exactly = 1) { regelApiClient.subsumsjon(subsumsjonId) }
    }

    @Test
    fun `Skip subsumsjon id not found`() {
        val subsumsjonId = SubsumsjonId(ULID().nextULID())
        val regelApiClient = mockk<SubsumsjonApiClient>().apply {
            every { this@apply.subsumsjon(subsumsjonId) } throws SubsumsjonClientException(status = 404, message = "not found")
        }

        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, Configuration())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            val json =
                """
                { "id" : "${subsumsjonId.id}" }
                """.trimIndent()
            topologyTestDriver.subsumsjonInputTopic().also { it.pipeInput(json) }
        }

        verify(exactly = 1) { regelApiClient.subsumsjon(subsumsjonId) }
    }
}

private fun TopologyTestDriver.subsumsjonInputTopic(): TestInputTopic<String, String> =
    this.createInputTopic(
        inTopic.name,
        inTopic.keySerde.serializer(),
        inTopic.valueSerde.serializer()
    )

private fun TopologyTestDriver.subsumsjonOutputTopic(): TestOutputTopic<String, String> =
    this.createOutputTopic(
        outTopic.name,
        outTopic.keySerde.deserializer(),
        outTopic.valueSerde.deserializer()
    )
