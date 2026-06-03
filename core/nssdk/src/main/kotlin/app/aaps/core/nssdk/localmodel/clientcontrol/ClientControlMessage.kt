package app.aaps.core.nssdk.localmodel.clientcontrol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The closed family of payloads a paired client can send to a master inside a
 * [SignedEnvelope]. New message types must extend this sealed hierarchy — that
 * gives the receiver compile-time exhaustiveness in `when` dispatch and avoids
 * the silent-typo failure mode of string-keyed message types.
 *
 * Wire format: each variant is serialized polymorphically via kotlinx
 * serialization. The discriminator field on the wire is named `type` (kotlinx's
 * default). The signed bytes are the JSON of the variant including the
 * discriminator — see [SignedEnvelope.payload].
 *
 * **The `@SerialName` strings are the wire contract — once shipped they cannot
 * be renamed without a flag day.** The Kotlin class names can be refactored
 * freely; the SerialNames cannot. Add new variants, do not rename existing.
 */
@Serializable
sealed class ClientControlMessage {

    /**
     * First signed message a freshly paired client sends to the master.
     * Promotes the master's pairing entry from Pending → Active. Sent with
     * counter = 1 (the first envelope after pairing).
     *
     * `protocolVersion` lets either side reject a peer it cannot speak to.
     */
    @Serializable
    @SerialName("hello")
    data class Hello(
        val protocolVersion: Int = 1
    ) : ClientControlMessage()

    /**
     * Tells the master to activate the named scene. `durationMinutes = null` means use
     * the scene's stored default; an explicit value (including 0 for indefinite) overrides.
     */
    @Serializable
    @SerialName("scene_start")
    data class SceneStart(
        val sceneId: String,
        val durationMinutes: Int? = null
    ) : ClientControlMessage()

    /**
     * Tells the master to deactivate whatever scene is currently active.
     *
     * `triggerChain = true` mirrors master's "Skip to <ChainTarget>" choice — after deactivating,
     * the master re-checks canChain (target enabled, loop running, pump initialized, profile set)
     * and activates the active scene's configured chain target if all are met. `false` is a plain
     * stop (chain dies). Wire intentionally carries no target id: the master uses its own current
     * chain config so client-side staleness can't trigger an unintended scene.
     */
    @Serializable
    @SerialName("scene_stop")
    data class SceneStop(
        val triggerChain: Boolean = false
    ) : ClientControlMessage()

    /**
     * Client pushes its full scene-definitions JSON to the master so a scene edit made on the
     * client side propagates. The wire carries the same JSON shape the local pref holds (a JSON
     * array of scene objects), unmodified — receiver does per-scene last-writer-wins by the
     * `lastModified` field on each scene, then triggers a master republish that fans the merged
     * result back out to every paired client via the running-config doc.
     *
     * `scenesJson` is opaque to the nssdk module (a String, not parsed) so the wire schema
     * stays decoupled from the UI module's scene model. Master parses it.
     */
    @Serializable
    @SerialName("scene_definitions_update")
    data class SceneDefinitionsUpdate(
        val scenesJson: String
    ) : ClientControlMessage()

    /**
     * Client pushes its full insulin-configuration JSON to the master after a local edit. The master
     * applies whole-list last-writer-wins by [version] (this device's last-edit wall clock) —
     * strictly-newer replaces, stale is dropped — then republishes via the running-config doc.
     *
     * `insulinJson` is opaque to the nssdk module (a String, the same object shape the local pref
     * holds) so the wire schema stays decoupled from the insulin model. Master parses it.
     */
    @Serializable
    @SerialName("insulin_configuration_update")
    data class InsulinConfigurationUpdate(
        val insulinJson: String,
        val version: Long
    ) : ClientControlMessage()

    /**
     * Client asks the master to activate an insulin — i.e. create a profile switch that re-applies
     * the master's CURRENT profile (name/%/timeshift/duration) with this insulin's [iCfgJson] config.
     * The master uses its own live profile (the client's view may be stale), so only the insulin
     * config travels; the resulting profile switch syncs back normally. `iCfgJson` is the same JSON
     * shape `ICfg` serializes to, opaque to the nssdk module.
     */
    @Serializable
    @SerialName("insulin_activate")
    data class InsulinActivate(
        val iCfgJson: String
    ) : ClientControlMessage()

    /**
     * Generic client→master push of plain, bidirectionally-synced preference values (keys carrying a
     * `SyncSpec(direction = Bidirectional)`). One message batches every locally-changed synced key, so
     * new synced settings need no new wire type — they just appear as another entry in [prefs].
     *
     * Each entry's value is the preference serialized as a string (the master parses it back using the
     * key's declared type), plus a per-key `lastModified` the master applies last-writer-wins by.
     * Master adopts strictly-newer values and republishes via the running-config doc.
     */
    @Serializable
    @SerialName("preferences_update")
    data class PreferencesUpdate(
        val prefs: Map<String, PrefEntry>
    ) : ClientControlMessage()
}

/** One synced preference on the wire: its value (serialized as a string) and edit timestamp. */
@Serializable
data class PrefEntry(
    val value: String,
    val lastModified: Long
)
