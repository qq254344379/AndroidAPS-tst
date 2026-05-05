package app.aaps.core.nssdk.localmodel.clientcontrol

import kotlinx.serialization.Serializable

/**
 * First signed message a freshly paired client sends to the master.
 *
 * `protocolVersion` lets either side reject a peer it cannot speak to.
 * Carried inside [SignedEnvelope.payload] with `type = "hello"` and counter=1.
 */
@Serializable
data class HelloMessage(
    val protocolVersion: Int = 1
)
