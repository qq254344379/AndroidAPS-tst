package app.aaps.pump.lenomed.compat;

/**
 * Compatibility shim for the ported Lenomed (Embecta) driver.
 *
 * The Embecta driver reuses a handful of Dana-RS / Dana-i BLE packet-type
 * constants that originally lived in {@code app.aaps.pump.danars.encryption.BleEncryption}.
 * Rather than depend on the whole dana module (which drags a heavy transitive
 * graph), we expose only the constants the driver actually references, with the
 * exact values copied from the upstream source.
 *
 * The upstream constants are {@code UByte} (0x00..0xFF); they are exposed to Java
 * as signed {@code byte}, matching the driver's {@code setMsgType(byte)} signature.
 */
public final class BleEncryption {

    private BleEncryption() {
    }

    // Values copied from app.aaps.pump.danars.encryption.BleEncryption (companion object).
    public static final byte DANAR_PACKET__START_PACKET = (byte) 0xA5;        // 0xA5u
    public static final byte DANAR_PACKET__END_PACKET = (byte) 0x5A;          // 0x5Au
    public static final byte DANAR_PACKET__ENCRYPTION_START = (byte) 0x7A;    // 0x7Au
    public static final byte DANAR_PACKET__ENCRYPTION_END = (byte) 0x2E;      // 0x2Eu
    public static final byte DANAI_PACKET__ENCRYPTION_START = (byte) 0xAA;    // 0xAAu
    public static final byte DANAI_PACKET__ENCRYPTION_END = (byte) 0xEE;      // 0xEEu
    public static final byte DANAR_PACKET__TYPE_COMMAND = (byte) 0xA1;        // 0xA1u
}
