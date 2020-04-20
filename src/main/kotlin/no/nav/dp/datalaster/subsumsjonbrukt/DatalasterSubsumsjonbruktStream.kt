package no.nav.dp.datalaster.subsumsjonbrukt

import java.util.Properties
import mu.KotlinLogging
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Service
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiClient
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonClientException
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonId
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

private val logger = KotlinLogging.logger {}

class DatalasterSubsumsjonbruktStream(
    private val subsumsjonApiClient: SubsumsjonApiClient,
    private val configuration: Configuration
) : Service() {
    override val SERVICE_APP_ID: String = "dp-datalaster-subsumsjonbrukt"
    override fun buildTopology(): Topology {
        val builder = StreamsBuilder()
        builder
            .consumeTopic(inTopic)
            .mapValues { _, jsonValue -> SubsumsjonId.fromJson(jsonValue) }
            .peek { _, id -> id?.let { logger.info { "Add data to subsumsjon id brukt $id" } } }
            .mapValues { _, id ->
                id?.let {
                    return@let try {
                        subsumsjonApiClient.subsumsjon(it)
                    } catch (exc: SubsumsjonClientException) {
                        when {
                            configuration.application.profile != Profile.PROD -> {
                                null
                            }
                            exc.status == 404 -> {
                                logger.warn { "Subsumsjons id '$it' not found" }
                                null
                            }
                            else -> {
                                throw exc
                            }
                        }
                    }
                }
            }
            .filterNot { _, value -> value == null }
            .toTopic(outTopic)
        return builder.build()
    }

    override fun getConfig(): Properties {
        return streamConfig(
            SERVICE_APP_ID, configuration.kafka.bootstrapServer,
            KafkaCredential(configuration.application.username, configuration.application.password)
        ).also {
            it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        }
    }
}

val inTopic = Topic(
    "privat-dagpenger-subsumsjon-brukt",
    keySerde = Serdes.String(),
    valueSerde = Serdes.String()
)

val outTopic = Topic(
    "privat-dagpenger-subsumsjon-brukt-data",
    keySerde = Serdes.String(),
    valueSerde = Serdes.String()
)
