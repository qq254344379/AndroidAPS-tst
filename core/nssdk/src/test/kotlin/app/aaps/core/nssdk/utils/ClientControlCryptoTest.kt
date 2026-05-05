package app.aaps.core.nssdk.utils

import app.aaps.core.nssdk.localmodel.clientcontrol.SignedEnvelope
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ClientControlCryptoTest {

    private fun draft(
        clientId: String = "client-1",
        counter: Long = 1L,
        timestamp: Long = 1_700_000_000_000L,
        type: String = "hello",
        payload: String = """{"protocolVersion":1,"capabilities":[]}"""
    ) = SignedEnvelope(clientId, counter, timestamp, type, payload, signature = "")

    @Test
    fun signVerifyRoundtrip() {
        val secret = ClientControlCrypto.newSecretBytes()
        val signed = ClientControlCrypto.signEnvelope(secret, draft())
        assertThat(signed.signature).isNotEmpty()
        assertThat(ClientControlCrypto.verifyEnvelope(secret, signed)).isTrue()
    }

    @Test
    fun verifyRejectsWrongSecret() {
        val signed = ClientControlCrypto.signEnvelope(ClientControlCrypto.newSecretBytes(), draft())
        assertThat(ClientControlCrypto.verifyEnvelope(ClientControlCrypto.newSecretBytes(), signed)).isFalse()
    }

    @Test
    fun verifyRejectsTamperedPayload() {
        val secret = ClientControlCrypto.newSecretBytes()
        val signed = ClientControlCrypto.signEnvelope(secret, draft(payload = "original"))
        val tampered = signed.copy(payload = "tampered")
        assertThat(ClientControlCrypto.verifyEnvelope(secret, tampered)).isFalse()
    }

    @Test
    fun verifyRejectsTamperedCounter() {
        val secret = ClientControlCrypto.newSecretBytes()
        val signed = ClientControlCrypto.signEnvelope(secret, draft(counter = 5L))
        val tampered = signed.copy(counter = 6L)
        assertThat(ClientControlCrypto.verifyEnvelope(secret, tampered)).isFalse()
    }

    @Test
    fun verifyRejectsTamperedType() {
        val secret = ClientControlCrypto.newSecretBytes()
        val signed = ClientControlCrypto.signEnvelope(secret, draft(type = "hello"))
        val tampered = signed.copy(type = "scene.stop")
        assertThat(ClientControlCrypto.verifyEnvelope(secret, tampered)).isFalse()
    }

    @Test
    fun signatureIsDeterministicForSameInput() {
        val secret = ClientControlCrypto.newSecretBytes()
        val a = ClientControlCrypto.signEnvelope(secret, draft())
        val b = ClientControlCrypto.signEnvelope(secret, draft())
        assertThat(a.signature).isEqualTo(b.signature)
    }

    @Test
    fun newSecretIs32Bytes() {
        assertThat(ClientControlCrypto.newSecretBytes()).hasLength(32)
    }

    @Test
    fun newSecretsAreUnique() {
        val a = ClientControlCrypto.bytesToHex(ClientControlCrypto.newSecretBytes())
        val b = ClientControlCrypto.bytesToHex(ClientControlCrypto.newSecretBytes())
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun hexRoundtripsCleanly() {
        val original = ClientControlCrypto.newSecretBytes()
        val roundtrip = ClientControlCrypto.hexToBytes(ClientControlCrypto.bytesToHex(original))
        assertThat(roundtrip).isEqualTo(original)
    }

    @Test
    fun hexRejectsOddLength() {
        try {
            ClientControlCrypto.hexToBytes("abc")
            assertThat(false).isTrue() // unreached
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Odd-length")
        }
    }

    @Test
    fun hexRejectsNonHexCharacters() {
        try {
            ClientControlCrypto.hexToBytes("zz")
            assertThat(false).isTrue() // unreached
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Non-hex")
        }
    }

    @Test
    fun timestampSkewAcceptsCloseValues() {
        val now = 1_700_000_000_000L
        assertThat(ClientControlCrypto.timestampWithinSkew(now, now)).isTrue()
        assertThat(ClientControlCrypto.timestampWithinSkew(now - 4 * 60_000L, now)).isTrue()
        assertThat(ClientControlCrypto.timestampWithinSkew(now + 4 * 60_000L, now)).isTrue()
    }

    @Test
    fun timestampSkewRejectsStaleAndFuture() {
        val now = 1_700_000_000_000L
        assertThat(ClientControlCrypto.timestampWithinSkew(now - 6 * 60_000L, now)).isFalse()
        assertThat(ClientControlCrypto.timestampWithinSkew(now + 6 * 60_000L, now)).isFalse()
    }

    @Test
    fun timestampSkewBoundaryIsInclusive() {
        val now = 1_700_000_000_000L
        val window = 5 * 60_000L
        // Exactly at the boundary — accepted (window is `<=`).
        assertThat(ClientControlCrypto.timestampWithinSkew(now - window, now)).isTrue()
        assertThat(ClientControlCrypto.timestampWithinSkew(now + window, now)).isTrue()
        // One millisecond past the boundary — rejected.
        assertThat(ClientControlCrypto.timestampWithinSkew(now - window - 1, now)).isFalse()
        assertThat(ClientControlCrypto.timestampWithinSkew(now + window + 1, now)).isFalse()
    }

    @Test
    fun newClientIdIsUuidShape() {
        val id = ClientControlCrypto.newClientId()
        assertThat(id).matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    }

    @Test
    fun canonicalStringMatchesPipeFormat() {
        val env = draft(clientId = "c", counter = 7L, timestamp = 12345L, type = "hello", payload = "p")
        assertThat(env.canonicalString()).isEqualTo("c|7|12345|hello|p")
    }
}
