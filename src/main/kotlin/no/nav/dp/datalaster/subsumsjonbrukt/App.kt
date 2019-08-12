package no.nav.dp.datalaster.subsumsjonbrukt

import kotlinx.coroutines.runBlocking
import no.nav.dp.datalaster.subsumsjonbrukt.health.HealthCheck
import no.nav.dp.datalaster.subsumsjonbrukt.health.HealthServer
import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiHttpClient
import java.net.URL
import java.util.concurrent.TimeUnit

fun main() {
    val configuration = Configuration()
    val datalasterSubsumsjonbruktStream = DatalasterSubsumsjonbruktStream(
        SubsumsjonApiHttpClient(URL(configuration.regelApiUrl), configuration.auth.regelApiKey),
        configuration
    ).also {
        it.start()
    }

    val healthChecks = listOf(datalasterSubsumsjonbruktStream as HealthCheck)
    val app = runBlocking {
        return@runBlocking HealthServer.startServer(configuration.application.httpPort, healthChecks).start(wait = false)
    }

    Runtime.getRuntime().addShutdownHook(Thread {
        datalasterSubsumsjonbruktStream.stop()
        app.stop(2, 60, TimeUnit.SECONDS)
    })
}