package no.nav.dp.datalaster.subsumsjonbrukt

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

class ConfigurationTest {

    private fun withProps(props: Map<String, String>, test: () -> Unit) {
        props.forEach { (k, v) ->
            System.getProperties()[k] = v
        }
        test()
        props.keys.forEach {
            System.getProperties().remove(it)
        }
    }

    @Test
    fun `Loads DEV profile with dev-fss as cluster name`() {
        withProps(mapOf("NAIS_CLUSTER_NAME" to "dev-fss")) {
            with(Configuration()) {
                this.application.profile shouldBe Profile.DEV
            }
        }
    }

    @Test
    fun `Loads PROD profile with prod-fss as cluster name`() {
        withProps(mapOf("NAIS_CLUSTER_NAME" to "prod-fss")) {
            with(Configuration()) {
                this.application.profile shouldBe Profile.PROD
            }
        }
    }

    @Test
    fun `No NAIS_CLUSTER_NAME should give local profile`() {
        withProps(emptyMap()) {
            with(Configuration()) {
                this.application.profile shouldBe Profile.LOCAL
            }
        }
    }
}