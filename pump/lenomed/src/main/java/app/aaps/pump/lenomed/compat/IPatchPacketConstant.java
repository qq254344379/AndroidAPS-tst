package app.aaps.pump.lenomed.compat;

/**
 * Compatibility shim for the ported Lenomed (Embecta) driver.
 *
 * Mirrors {@code app.aaps.pump.eopatch.core.ble.IPatchPacketConstant}, which the
 * driver referenced for a single constant. Only the referenced constant is kept,
 * with the exact value copied from the upstream source ({@code LEGACY: Byte = 0x50}).
 */
public interface IPatchPacketConstant {

    byte LEGACY = (byte) 0x50; // 0x50
}
