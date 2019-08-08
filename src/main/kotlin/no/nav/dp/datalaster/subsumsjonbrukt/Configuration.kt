package no.nav.dp.datalaster.subsumsjonbrukt

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType


private val localProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "LOCAL",
        "application.httpPort" to "8080",
        "kafka.bootstrapServer" to "localhost:9092",
        "srvdp.datalaster.subsumsjonbrukt.username" to "srvdp-vedtakresultat",
        "srvdp.datalaster.subsumsjonbrukt.password" to "srvdp-passord"
    )
)

private val devProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "DEV",
        "application.httpPort" to "8080",
        "kafka.bootstrapServer" to "b27apvl00045.preprod.local:8443,b27apvl00046.preprod.local:8443,b27apvl00047.preprod.local:8443",
        "srvdp.datalaster.subsumsjonbrukt.username" to "srvdp-vedtakresultat",
        "srvdp.datalaster.subsumsjonbrukt.password" to "srvdp-passord"
    )
)

private val prodProperties = ConfigurationMap(
    mapOf(
        "application.profile" to "PROD",
        "application.httpPort" to "8080",
        "kafka.bootstrapServer" to "a01apvl00145.adeo.no:8443,a01apvl00146.adeo.no:8443,a01apvl00147.adeo.no:8443,a01apvl00149.adeo.no:8443",
        "srvdp.datalaster.subsumsjonbrukt.username" to "srvdp-vedtakresultat",
        "srvdp.datalaster.subsumsjonbrukt.password" to "srvdp-passord"
    )
)

data class Configuration(
    val application: Application = Application()
)

data class Application(
    val httpPort: Int = config()[Key("application.httpPort", intType)],
    val profile: Profile = config()[Key("application.profile", stringType)].let { Profile.valueOf(it) },
    val username: String = config()[Key("srvdp.datalaster.subsumsjonbrukt.username", stringType)],
    val password: String = config()[Key("srvdp.datalaster.subsumsjonbrukt.password", stringType)]
)


enum class Profile {
    LOCAL, DEV, PROD
}


private fun config() = when (getEnvOrProp("NAIS_CLUSTER_NAME")) {
    "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding devProperties
    "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding prodProperties
    else -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables overriding localProperties
}

fun getEnvOrProp(propName: String): String? {
    return System.getenv(propName) ?: System.getProperty(propName)
}
