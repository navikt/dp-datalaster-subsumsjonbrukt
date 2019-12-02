package no.nav.dp.datalaster.subsumsjonbrukt

import de.huxhorn.sulky.ulid.ULID
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiClient
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonClientException
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonId
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.junit.jupiter.api.Test

import java.util.Properties

internal class DatalasterSubsumsjonbruktStreamTest {

    companion object {
        val factory = ConsumerRecordFactory<String, String>(
            inTopic.name,
            inTopic.keySerde.serializer(),
            inTopic.valueSerde.serializer()
        )

        val config = Properties().apply {
            this[StreamsConfig.APPLICATION_ID_CONFIG] = "test"
            this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "dummy:1234"
        }
    }

    @Test
    fun `Should not be able to process record with malformed json`() {
        val regelApiClient = mockk<SubsumsjonApiClient>()
        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, mockk<Configuration>())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create("bad json")
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                outTopic.name,
                outTopic.keySerde.deserializer(),
                outTopic.valueSerde.deserializer()
            )

            ut shouldBe null
        }

        verify(exactly = 0) { regelApiClient.subsumsjon(any()) }
    }

    @Test
    fun `Should not be able to process record with json missing id`() {
        val regelApiClient = mockk<SubsumsjonApiClient>()
        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, mockk<Configuration>())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create("""
                { "aname" : "aname" }
            """.trimIndent())
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                outTopic.name,
                outTopic.keySerde.deserializer(),
                outTopic.valueSerde.deserializer()
            )

            ut shouldBe null
        }

        verify(exactly = 0) { regelApiClient.subsumsjon(any()) }
    }

    @Test
    fun `Should not be able to process record with json with an unrecognized id`() {
        val regelApiClient = mockk<SubsumsjonApiClient>()
        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, mockk<Configuration>())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create("""
                { "id" : "blabla" }
            """.trimIndent())
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                outTopic.name,
                outTopic.keySerde.deserializer(),
                outTopic.valueSerde.deserializer()
            )

            ut shouldBe null
        }

        verify(exactly = 0) { regelApiClient.subsumsjon(any()) }
    }

    @Test
    fun `Should be able to process record with json with an recognized id`() {
        val subsumsjonId = SubsumsjonId(ULID().nextULID())
        val regelApiClient = mockk<SubsumsjonApiClient>().apply {
            every { this@apply.subsumsjon(subsumsjonId) } returns "any"
        }

        val stream = DatalasterSubsumsjonbruktStream(regelApiClient, mockk<Configuration>())

        TopologyTestDriver(stream.buildTopology(), config).use { topologyTestDriver ->
            val inputRecord = factory.create("""
                { "id" : "${subsumsjonId.id}" }
            """.trimIndent())
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                outTopic.name,
                outTopic.keySerde.deserializer(),
                outTopic.valueSerde.deserializer()
            )

            ut shouldNotBe null
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
            val inputRecord = factory.create("""
                { "id" : "${subsumsjonId.id}" }
            """.trimIndent())
            topologyTestDriver.pipeInput(inputRecord)

            val ut = topologyTestDriver.readOutput(
                outTopic.name,
                outTopic.keySerde.deserializer(),
                outTopic.valueSerde.deserializer()
            )

            ut shouldBe null
        }

        verify(exactly = 1) { regelApiClient.subsumsjon(subsumsjonId) }
    }
}