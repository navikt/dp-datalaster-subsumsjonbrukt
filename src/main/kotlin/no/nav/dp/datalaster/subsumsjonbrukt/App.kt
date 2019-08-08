package no.nav.dp.datalaster.subsumsjonbrukt

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dp.datalaster.subsumsjonbrukt.health.HealthServer
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiHttpClient
import java.net.URL

private val LOGGER = KotlinLogging.logger {}

fun main() {
    val configuration = Configuration()
    runBlocking {
        LOGGER.info { "STARTING" }
        HealthServer.startServer(configuration.application.httpPort).start(wait = false)
        DatalasterSubsumsjonbruktStream.run(
            configuration,
            SubsumsjonApiHttpClient(URL(configuration.regelApiUrl), configuration.auth.regelApiKey)
        )
    }
}