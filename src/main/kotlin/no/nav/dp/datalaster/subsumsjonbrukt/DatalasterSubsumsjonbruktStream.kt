package no.nav.dp.datalaster.subsumsjonbrukt

import mu.KotlinLogging
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import no.nav.dp.datalaster.subsumsjonbrukt.health.HealthCheck
import no.nav.dp.datalaster.subsumsjonbrukt.health.HealthStatus
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiClient
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonId
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import java.util.Properties
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private val LOGGER = KotlinLogging.logger {}

class DatalasterSubsumsjonbruktStream(
    private val subsumsjonApiClient: SubsumsjonApiClient,
    private val configuration: Configuration
) : HealthCheck {

    private val APPLICATION_NAME =
        "dp-datalaster-subsumsjonbrukt" // NB: also used as group.id for the consumer group - do not change!

    private val streams: KafkaStreams by lazy {
        KafkaStreams(this.buildTopology(), this.getConfig()).apply {
            setUncaughtExceptionHandler { _, _ -> exitProcess(0) }
        }
    }

    fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        builder
            .consumeTopic(inTopic)
            .mapValues { _, jsonValue -> SubsumsjonId.fromJson(jsonValue) }
            .mapValues { _, id -> id?.let { subsumsjonApiClient.subsumsjon(it) } }
            .filterNot { _, value -> value == null }
            .toTopic(outTopic)
        return builder.build()
    }

    fun start() = streams.start().also { LOGGER.info { "Starting up $APPLICATION_NAME kafka stream" } }

    fun stop() = with(streams) {
        close(3, TimeUnit.SECONDS)
        cleanUp()
    }.also {
        LOGGER.info { "Shutting down $APPLICATION_NAME kafka stream" }
    }

    override fun status(): HealthStatus {
        return when (streams.state()) {
            KafkaStreams.State.ERROR -> HealthStatus.DOWN
            KafkaStreams.State.PENDING_SHUTDOWN -> HealthStatus.DOWN
            else -> HealthStatus.UP
        }
    }

    private fun getConfig(): Properties {
        return streamConfig(
            APPLICATION_NAME, configuration.kafka.bootstrapServer,
            KafkaCredential(configuration.application.username, configuration.application.password)
        ).also {
            it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        }
    }
}

val outTopic = Topic(
    "privat-dagpenger-subsumsjon-data-brukt",
    keySerde = Serdes.String(),
    valueSerde = Serdes.String()
)

val inTopic = Topic(
    "privat-dagpenger-subsumsjon-brukt",
    keySerde = Serdes.String(),
    valueSerde = Serdes.String()
)
