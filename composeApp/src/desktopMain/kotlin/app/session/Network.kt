package app.session

import java.net.NetworkInterface

fun resolveLanIp(): String {
    val interfaces = runCatching {
        NetworkInterface.getNetworkInterfaces()?.asSequence()
    }.getOrNull() ?: emptySequence()
    
    val candidate = interfaces
        .filter { iface ->
            runCatching { !iface.isLoopback && iface.isUp && !iface.isVirtual }.getOrDefault(false)
        }
        .filter { !isVpnOrVirtualName(it.name.lowercase()) }
        .flatMap { it.inetAddresses.asSequence() }
        .firstOrNull { it.hostAddress.contains('.') && !it.isLoopbackAddress }
        
    return candidate?.hostAddress 
        ?: runCatching {
            NetworkInterface.getNetworkInterfaces()
                ?.asSequence()
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.firstOrNull { !it.isLoopbackAddress && it.hostAddress.contains('.') }
                ?.hostAddress
        }.getOrNull() ?: "127.0.0.1"
}

private fun isVpnOrVirtualName(name: String): Boolean {
    val prefixes = listOf("tun", "tap", "ppp", "wg", "utun", "docker", "veth", "br-", "virbr", "vmnet", "vbox", "zt", "tailscale", "ts")
    return prefixes.any { name.startsWith(it) } || name.contains("vpn")
}
