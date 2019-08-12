package no.nav.dp.datalaster.subsumsjonbrukt

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dp.datalaster.subsumsjonbrukt.health.HealthCheck
import no.nav.dp.datalaster.subsumsjonbrukt.health.HealthServer
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiHttpClient
import java.net.URL

private val LOGGER = KotlinLogging.logger {}

fun main() {
    val configuration = Configuration()
    val datalasterSubsumsjonbruktStream = DatalasterSubsumsjonbruktStream(
        SubsumsjonApiHttpClient(URL(configuration.regelApiUrl), configuration.auth.regelApiKey),
        configuration
    ).also {
        it.start()
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        datalasterSubsumsjonbruktStream.stop()
    })
    val healthChecks = listOf(datalasterSubsumsjonbruktStream as HealthCheck)
    runBlocking {
        LOGGER.info { "STARTING" }
        HealthServer.startServer(configuration.application.httpPort, healthChecks).start(wait = false)
    }
}