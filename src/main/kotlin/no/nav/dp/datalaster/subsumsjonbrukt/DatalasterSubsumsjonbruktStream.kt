package no.nav.dp.datalaster.subsumsjonbrukt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.dagpenger.streams.KafkaCredential
import no.nav.dagpenger.streams.Topic
import no.nav.dagpenger.streams.consumeTopic
import no.nav.dagpenger.streams.streamConfig
import no.nav.dagpenger.streams.toTopic
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiClient
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonId
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

class DatalasterSubsumsjonbruktStream(
    private val subsumsjonApiClient: SubsumsjonApiClient
) {

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

    companion object Runner : CoroutineScope {
        private val SERVICE_APP_ID =
            "dp-datalaster-subsumsjonbrukt" // NB: also used as group.id for the consumer group - do not change!
        private val job: Job = Job()
        override val coroutineContext: CoroutineContext
            get() = Dispatchers.IO + job

        fun run(configuration: Configuration, subsumsjonApiClient: SubsumsjonApiClient) {
            launch {
                logger.info("Starter kafka stream")
                val properties = streamConfig(
                    SERVICE_APP_ID, configuration.kafka.bootstrapServer,
                    KafkaCredential(configuration.application.username, configuration.application.password)
                )
                val datalasterSubsumsjonbruktStream = DatalasterSubsumsjonbruktStream(subsumsjonApiClient)
                val kafkaStreams = KafkaStreams(datalasterSubsumsjonbruktStream.buildTopology(), properties)
                kafkaStreams.start()
                Runtime.getRuntime().addShutdownHook(Thread {
                    logger.info { "Shutting down stream" }
                    kafkaStreams.close(2, TimeUnit.SECONDS)
                    kafkaStreams.cleanUp()
                })
            }
        }
    }

    fun isActive(): Boolean {
        return job.isActive
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
