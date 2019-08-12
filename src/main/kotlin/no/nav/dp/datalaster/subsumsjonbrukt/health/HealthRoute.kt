package no.nav.dp.datalaster.subsumsjonbrukt.health

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports

object HealthServer {
    suspend fun startServer(port: Int, healthChecks: List<HealthCheck>): NettyApplicationEngine {
        DefaultExports.initialize()
        return embeddedServer(Netty, port = port) {
            health(healthChecks)
        }
    }
}

internal fun Application.health(healthChecks: List<HealthCheck>) {
    install(DefaultHeaders)
    routing {
        healthRoutes(healthChecks)
    }
}

fun Route.healthRoutes(healthChecks: List<HealthCheck>) {
    route("/metrics") {
        get {
            val names = call.request.queryParameters.getAll("name")?.toSet() ?: kotlin.collections.emptySet()
            call.respondTextWriter(
                io.ktor.http.ContentType.parse(TextFormat.CONTENT_TYPE_004),
                io.ktor.http.HttpStatusCode.OK
            ) {
                TextFormat.write004(this, CollectorRegistry.defaultRegistry.filteredMetricFamilySamples(names))
            }
        }
    }

    route("/isAlive") {
        get {
            if (healthChecks.all { it.status() == HealthStatus.UP }) call.respondText("ALIVE", ContentType.Text.Plain) else
                call.response.status(HttpStatusCode.ServiceUnavailable)
        }
    }
    route("/isReady") {
        get {
            call.respondText(text = "READY", contentType = io.ktor.http.ContentType.Text.Plain)
        }
    }
}