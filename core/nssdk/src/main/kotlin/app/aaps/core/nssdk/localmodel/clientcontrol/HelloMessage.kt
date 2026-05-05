package app.aaps.core.nssdk.localmodel.clientcontrol

import kotlinx.serialization.Serializable

/**
 * First signed message a freshly paired client sends to the master.
 *
 * Echoes the capabilities the client believes it was granted so master can
 * detect mismatch (e.g. capability set drift between QR generation and scan).
 * `protocolVersion` lets either side reject a peer it cannot speak to.
 *
 * Carried inside [SignedEnvelope.payload] with `type = "hello"` and counter=1.
 */
@Serializable
data class HelloMessage(
    val protocolVersion: Int = 1,
    val capabilities: List<String>
)
