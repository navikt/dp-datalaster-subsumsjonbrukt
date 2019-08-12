package no.nav.dp.datalaster.subsumsjonbrukt.health

import io.kotlintest.shouldBe
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class HealthApplicationTest {

    val goodHealthChecks: List<HealthCheck>
        get() = listOf(mockk<HealthCheck>().also {
            every { it.status() } returns HealthStatus.UP
        })

    @Test
    fun `Should have alive, ready and metrics endpoints`() = withTestApplication(api(goodHealthChecks)) {
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

    val badHealthChecks: List<HealthCheck>
        get() = listOf(mockk<HealthCheck>().also {
            every { it.status() } returns HealthStatus.DOWN
        })

    @Test
    fun `alive check should fail if a healtcheck is down `() = withTestApplication(api(badHealthChecks)) {
        with(handleRequest(HttpMethod.Get, "/isAlive")) {
            response.status() shouldBe HttpStatusCode.ServiceUnavailable
        }
    }
}

private fun api(
    healthChecks: List<HealthCheck>
): Application.() -> Unit {
    return fun Application.() {
        health(
            healthChecks
        )
    }
}