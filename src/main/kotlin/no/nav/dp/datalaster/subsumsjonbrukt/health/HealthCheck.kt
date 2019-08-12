package no.nav.dp.datalaster.subsumsjonbrukt.health

interface HealthCheck {
    fun status(): HealthStatus
}

enum class HealthStatus {
    UP, DOWN
}
