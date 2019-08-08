package no.nav.dp.datalaster.subsumsjonbrukt.health

import io.kotlintest.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test

class HealthApplicationTest {

    @Test
    fun `Should have alive, ready and metrics endpoints`() = withTestApplication(Application::health) {
        with(handleRequest(HttpMethod.Get, "/isAlive")) {
            response.status() shouldBe HttpStatusCode.OK
        }
        with(handleRequest(HttpMethod.Get, "/isReady")) {
            response.status() shouldBe HttpStatusCode.OK
        }
        with(handleRequest(HttpMethod.Get, "/metrics")) {
            response.status() shouldBe HttpStatusCode.OK
        }
    }
}