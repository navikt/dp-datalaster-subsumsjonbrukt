package no.nav.dp.datalaster.subsumsjonbrukt

import no.nav.dp.datalaster.subsumsjonbrukt.regelapi.SubsumsjonApiHttpClient
import java.net.URL

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
}