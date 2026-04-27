package app.session

import java.net.NetworkInterface

fun resolveLanIp(): String =
    NetworkInterface.getNetworkInterfaces()
        ?.asSequence()
        ?.flatMap { it.inetAddresses.asSequence() }
        ?.firstOrNull { !it.isLoopbackAddress && it.hostAddress.contains('.') }
        ?.hostAddress ?: "localhost"
