package app.aaps.pump.lenomed.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.SystemClock;
import android.util.SparseArray;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.core.app.ActivityCompat;
import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.core.interfaces.rx.events.EventPumpStatusChanged;
import app.aaps.core.interfaces.ui.UiInteraction;
import app.aaps.core.ui.R;
import app.aaps.core.ui.toast.ToastUtils;
import app.aaps.core.utils.ConcurrencyKt;
import app.aaps.pump.lenomed.LenomedPump;
import app.aaps.pump.lenomed.packet.LenomedPacket;
import app.aaps.pump.lenomed.packet.LenomedResponseMessageHashTable;
import app.aaps.pump.lenomed.packet.LenomedSettingResponseMessageHashTable;
import ch.qos.logback.core.CoreConstants;
import io.socket.client.Socket;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.Triple;
import kotlin.Unit;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;
import kotlin.text.CharsKt;
import kotlin.text.StringsKt;
import org.apache.commons.lang3.StringUtils;

/* JADX INFO: compiled from: BLECommonService.kt */
/* JADX INFO: loaded from: classes4.dex */
@Singleton
@Metadata(d1 = {"\u0000Ã\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0012\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\t\n\u0002\b\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004*\u0001^\b\u0007\u0018\u0000 e2\u00020\u0001:\u0001eBQ\b\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u0012\u0006\u0010\f\u001a\u00020\r\u0012\u0006\u0010\u000e\u001a\u00020\u000f\u0012\u0006\u0010\u0010\u001a\u00020\u0011\u0012\u0006\u0010\u0012\u001a\u00020\u0013¢\u0006\u0004\b\u0014\u0010\u0015J\b\u00105\u001a\u00020\u001eH\u0002J\u0018\u00106\u001a\u00020)2\u0006\u00107\u001a\u00020%2\b\u00108\u001a\u0004\u0018\u00010%J\u0006\u00109\u001a\u00020:J\u000e\u0010;\u001a\u00020:2\u0006\u00107\u001a\u00020%J\b\u0010<\u001a\u00020:H\u0007J\u0014\u0010A\u001a\u00020\u001e*\u00020\u00192\u0006\u0010B\u001a\u00020\u0019H\u0002J\u0018\u0010C\u001a\u00020:2\u0006\u0010D\u001a\u0002002\u0006\u0010E\u001a\u00020\u0019H\u0003J\u0010\u0010I\u001a\n\u0012\u0004\u0012\u00020K\u0018\u00010JH\u0002J\b\u0010L\u001a\u00020:H\u0003J\u0018\u0010M\u001a\u00020:2\u0006\u0010N\u001a\u00020'2\u0006\u0010O\u001a\u00020\u001eH\u0003J\u0010\u0010P\u001a\u00020:2\u0006\u0010Q\u001a\u00020\u001eH\u0007J\u0016\u0010R\u001a\u00020:2\u0006\u0010S\u001a\u00020\u001d2\u0006\u0010T\u001a\u00020UJ\u0010\u0010V\u001a\u00020:2\u0006\u0010E\u001a\u00020\u0019H\u0002J\u0010\u0010W\u001a\u00020:2\u0006\u0010E\u001a\u00020\u0019H\u0002J\u001a\u0010X\u001a\u00020:2\u0006\u0010E\u001a\u00020\u00192\b\b\u0002\u0010Y\u001a\u00020)H\u0002J\u0018\u0010Z\u001a\u00020:2\u0006\u0010[\u001a\u00020\u001e2\u0006\u0010\\\u001a\u00020\u001dH\u0002J\u0018\u0010`\u001a\u00020:2\u0006\u0010a\u001a\u00020b2\u0006\u0010E\u001a\u00020\u0019H\u0003J\b\u0010c\u001a\u00020:H\u0007J\b\u0010d\u001a\u00020:H\u0007R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\u0016\u001a\b\u0012\u0002\b\u0003\u0018\u00010\u0017X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u0018\u001a\u0004\u0018\u00010\u0019X\u0082\u000e¢\u0006\u0002\n\u0000RB\u0010\u001a\u001a6\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u001d\u0012\u0004\u0012\u00020\u001e\u0012\u0004\u0012\u00020\u00190\u001c0\u001bj\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\u001d\u0012\u0004\u0012\u00020\u001e\u0012\u0004\u0012\u00020\u00190\u001c`\u001fX\u0082\u0004¢\u0006\u0002\n\u0000R\u0016\u0010 \u001a\u0004\u0018\u00010!8BX\u0082\u0004¢\u0006\u0006\u001a\u0004\b\"\u0010#R\u0010\u0010$\u001a\u0004\u0018\u00010%X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010&\u001a\u0004\u0018\u00010'X\u0082\u000e¢\u0006\u0002\n\u0000R\u001a\u0010(\u001a\u00020)X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b(\u0010*\"\u0004\b+\u0010,R\u001a\u0010-\u001a\u00020)X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b-\u0010*\"\u0004\b.\u0010,R\u0010\u0010/\u001a\u0004\u0018\u000100X\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u00101\u001a\u0004\u0018\u000100X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u00102\u001a\u00020\u001eX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u00103\u001a\u000204X\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010=\u001a\u00020>X\u0082\u0004¢\u0006\b\n\u0000\u0012\u0004\b?\u0010@R\u0014\u0010F\u001a\u0002008BX\u0082\u0004¢\u0006\u0006\u001a\u0004\bG\u0010HR\u0010\u0010]\u001a\u00020^X\u0082\u0004¢\u0006\u0004\n\u0002\u0010_¨\u0006f"}, d2 = {"Lapp/aaps/pump/lenomed/service/BLECommonService;", "", "injector", "Ldagger/android/HasAndroidInjector;", "aapsLogger", "Lapp/aaps/core/interfaces/logging/AAPSLogger;", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", CoreConstants.CONTEXT_SCOPE_VALUE, "Landroid/content/Context;", "rxBus", "Lapp/aaps/core/interfaces/rx/bus/RxBus;", "lenomedResponseMessageHashTable", "Lapp/aaps/pump/lenomed/packet/LenomedResponseMessageHashTable;", "lenomedSettingResponseMessageHashTable", "Lapp/aaps/pump/lenomed/packet/LenomedSettingResponseMessageHashTable;", "lenomedPump", "Lapp/aaps/pump/lenomed/LenomedPump;", "uiInteraction", "Lapp/aaps/core/interfaces/ui/UiInteraction;", "<init>", "(Ldagger/android/HasAndroidInjector;Lapp/aaps/core/interfaces/logging/AAPSLogger;Lapp/aaps/core/interfaces/resources/ResourceHelper;Landroid/content/Context;Lapp/aaps/core/interfaces/rx/bus/RxBus;Lapp/aaps/pump/lenomed/packet/LenomedResponseMessageHashTable;Lapp/aaps/pump/lenomed/packet/LenomedSettingResponseMessageHashTable;Lapp/aaps/pump/lenomed/LenomedPump;Lapp/aaps/core/interfaces/ui/UiInteraction;)V", "scheduledDisconnection", "Ljava/util/concurrent/ScheduledFuture;", "processedMessageByte", "", "mSendQueue", "Ljava/util/ArrayList;", "Lkotlin/Triple;", "Lapp/aaps/pump/lenomed/packet/LenomedPacket;", "", "Lkotlin/collections/ArrayList;", "bluetoothAdapter", "Landroid/bluetooth/BluetoothAdapter;", "getBluetoothAdapter", "()Landroid/bluetooth/BluetoothAdapter;", "connectDeviceName", "", "bluetoothGatt", "Landroid/bluetooth/BluetoothGatt;", "isConnected", "", "()Z", "setConnected", "(Z)V", "isConnecting", "setConnecting", "uartIndicate", "Landroid/bluetooth/BluetoothGattCharacteristic;", "uartWrite", "mSequence", "responseBuffer", "Ljava/io/ByteArrayOutputStream;", "getMsgSequence", Socket.EVENT_CONNECT, TypedValues.TransitionType.S_FROM, "address", "stopConnecting", "", Socket.EVENT_DISCONNECT, "close", "mGattCallback", "Landroid/bluetooth/BluetoothGattCallback;", "getMGattCallback$annotations", "()V", "indexOfSequence", "sequence", "writeCharacteristicNoResponse", "characteristic", "data", "uartWriteBTGattChar", "getUartWriteBTGattChar", "()Landroid/bluetooth/BluetoothGattCharacteristic;", "getSupportedGattServices", "", "Landroid/bluetooth/BluetoothGattService;", "findCharacteristic", "onConnectionStateChangeSynchronized", "gatt", "newState", "setRequestMtu", "mtu", "sendMessage", "message", "waitMillis", "", "processResponseMessage", "processSettingResponseMessage", "processMessage", "isSetting", "matchAndNotifyQueue", "receivedCommand", "handler", "mScanCallback", "app/aaps/pump/lenomed/service/BLECommonService$mScanCallback$1", "Lapp/aaps/pump/lenomed/service/BLECommonService$mScanCallback$1;", "parseBroadcastPacket", "device", "Landroid/bluetooth/BluetoothDevice;", "startBroadcastScan", "stopBroadcastScan", "Companion", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class BLECommonService {
    private static final String CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private static final byte[] EBT_END_MARKER = {69, 66, 84};
    private static final String INDICATION_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private static final int MTU = 240;
    private static final long WRITE_DELAY_MILLIS = 50;
    private static final String WRITE_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final AAPSLogger aapsLogger;
    private BluetoothGatt bluetoothGatt;
    private String connectDeviceName;
    private final Context context;
        private boolean isConnected;
    private boolean isConnecting;
    private final LenomedPump lenomedPump;
    private final LenomedResponseMessageHashTable lenomedResponseMessageHashTable;
    private final LenomedSettingResponseMessageHashTable lenomedSettingResponseMessageHashTable;
    private final BluetoothGattCallback mGattCallback;
    private final BLECommonService$mScanCallback$1 mScanCallback;
    private final ArrayList<Triple<LenomedPacket, Integer, byte[]>> mSendQueue;
    private int mSequence;
    private byte[] processedMessageByte;
    private final ByteArrayOutputStream responseBuffer;
    private final ResourceHelper rh;
    private final RxBus rxBus;
    private ScheduledFuture<?> scheduledDisconnection;
    private BluetoothGattCharacteristic uartIndicate;
    private BluetoothGattCharacteristic uartWrite;
    private final UiInteraction uiInteraction;

    private static /* synthetic */ void getMGattCallback$annotations() {
    }

    /* JADX WARN: Type inference failed for: r2v5, types: [app.aaps.pump.lenomed.service.BLECommonService$mScanCallback$1] */
    @Inject
    public BLECommonService( AAPSLogger aapsLogger, ResourceHelper rh, Context context, RxBus rxBus, LenomedResponseMessageHashTable lenomedResponseMessageHashTable, LenomedSettingResponseMessageHashTable lenomedSettingResponseMessageHashTable, LenomedPump lenomedPump, UiInteraction uiInteraction) {
        Intrinsics.checkNotNullParameter(aapsLogger, "aapsLogger");
        Intrinsics.checkNotNullParameter(rh, "rh");
        Intrinsics.checkNotNullParameter(context, "context");
        Intrinsics.checkNotNullParameter(rxBus, "rxBus");
        Intrinsics.checkNotNullParameter(lenomedResponseMessageHashTable, "lenomedResponseMessageHashTable");
        Intrinsics.checkNotNullParameter(lenomedSettingResponseMessageHashTable, "lenomedSettingResponseMessageHashTable");
        Intrinsics.checkNotNullParameter(lenomedPump, "lenomedPump");
        Intrinsics.checkNotNullParameter(uiInteraction, "uiInteraction");
        this.aapsLogger = aapsLogger;
        this.rh = rh;
        this.context = context;
        this.rxBus = rxBus;
        this.lenomedResponseMessageHashTable = lenomedResponseMessageHashTable;
        this.lenomedSettingResponseMessageHashTable = lenomedSettingResponseMessageHashTable;
        this.lenomedPump = lenomedPump;
        this.uiInteraction = uiInteraction;
        this.mSendQueue = new ArrayList<>();
        this.responseBuffer = new ByteArrayOutputStream();
        this.mGattCallback = new BluetoothGattCallback() { // from class: app.aaps.pump.lenomed.service.BLECommonService$mGattCallback$1
            @Override // android.bluetooth.BluetoothGattCallback
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                Intrinsics.checkNotNullParameter(gatt, "gatt");
                this.this$0.onConnectionStateChangeSynchronized(gatt, newState);
            }

            @Override // android.bluetooth.BluetoothGattCallback
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                Intrinsics.checkNotNullParameter(gatt, "gatt");
                this.this$0.aapsLogger.debug(LTag.PUMPBTCOMM, "Services discovered");
                if (status == 0) {
                    this.this$0.findCharacteristic();
                    SystemClock.sleep(1600L);
                    this.this$0.setConnected(true);
                    this.this$0.setConnecting(false);
                    ArrayList arrayList = this.this$0.mSendQueue;
                    BLECommonService bLECommonService = this.this$0;
                    synchronized (arrayList) {
                        bLECommonService.aapsLogger.debug(LTag.PUMPBTCOMM, "Services discovered mSendQueue clear start size:" + bLECommonService.mSendQueue.size());
                        bLECommonService.mSendQueue.clear();
                        bLECommonService.aapsLogger.debug(LTag.PUMPBTCOMM, "Services discovered mSendQueue clear end size:" + bLECommonService.mSendQueue.size());
                        Unit unit = Unit.INSTANCE;
                    }
                }
            }

            @Override // android.bluetooth.BluetoothGattCallback
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                byte[] bArrCopyOf;
                byte b;
                Intrinsics.checkNotNullParameter(gatt, "gatt");
                Intrinsics.checkNotNullParameter(characteristic, "characteristic");
                byte[] value = characteristic.getValue();
                AAPSLogger aAPSLogger = this.this$0.aapsLogger;
                LTag lTag = LTag.PUMPBTCOMM;
                LenomedPacket.Companion companion = LenomedPacket.INSTANCE;
                Intrinsics.checkNotNull(value);
                aAPSLogger.debug(lTag, "(Response) Characteristic changed: " + companion.toHex(value));
                this.this$0.responseBuffer.write(value);
                byte[] byteArray = this.this$0.responseBuffer.toByteArray();
                BLECommonService bLECommonService = this.this$0;
                Intrinsics.checkNotNull(byteArray);
                int iIndexOfSequence = bLECommonService.indexOfSequence(byteArray, BLECommonService.EBT_END_MARKER);
                if (iIndexOfSequence != -1) {
                    if (iIndexOfSequence == 0) {
                        bArrCopyOf = new byte[0];
                    } else {
                        bArrCopyOf = Arrays.copyOf(byteArray, iIndexOfSequence);
                        Intrinsics.checkNotNullExpressionValue(bArrCopyOf, "copyOf(...)");
                    }
                    if (bArrCopyOf.length == 0) {
                        this.this$0.aapsLogger.debug(LTag.PUMPBTCOMM, "Empty packet before EBT, ignored");
                    } else {
                        this.this$0.aapsLogger.debug(LTag.PUMPBTCOMM, "Complete response packet received (" + bArrCopyOf.length + " bytes): " + LenomedPacket.INSTANCE.toHex(bArrCopyOf));
                        if ((bArrCopyOf.length == 0) || ((b = bArrCopyOf[0]) != -73 && b != -78)) {
                            AAPSLogger aAPSLogger2 = this.this$0.aapsLogger;
                            LTag lTag2 = LTag.PUMPBTCOMM;
                            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
                            String str = String.format("%02X", Arrays.copyOf(new Object[]{Byte.valueOf(bArrCopyOf[0])}, 1));
                            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
                            aAPSLogger2.debug(lTag2, "Command 0x" + str + " → normal response");
                            this.this$0.processResponseMessage(bArrCopyOf);
                        } else {
                            AAPSLogger aAPSLogger3 = this.this$0.aapsLogger;
                            LTag lTag3 = LTag.PUMPBTCOMM;
                            StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
                            String str2 = String.format("%02X", Arrays.copyOf(new Object[]{Byte.valueOf(bArrCopyOf[0])}, 1));
                            Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
                            aAPSLogger3.debug(lTag3, "Command 0x" + str2 + " → treat as Setting Response");
                            this.this$0.processSettingResponseMessage(bArrCopyOf);
                        }
                    }
                    this.this$0.responseBuffer.reset();
                    int length = iIndexOfSequence + BLECommonService.EBT_END_MARKER.length;
                    if (length < byteArray.length) {
                        this.this$0.responseBuffer.write(byteArray, length, byteArray.length - length);
                    }
                }
            }

            @Override // android.bluetooth.BluetoothGattCallback
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                Intrinsics.checkNotNullParameter(gatt, "gatt");
                Intrinsics.checkNotNullParameter(characteristic, "characteristic");
                AAPSLogger aAPSLogger = this.this$0.aapsLogger;
                LTag lTag = LTag.PUMPBTCOMM;
                LenomedPacket.Companion companion = LenomedPacket.INSTANCE;
                byte[] value = characteristic.getValue();
                Intrinsics.checkNotNullExpressionValue(value, "getValue(...)");
                aAPSLogger.debug(lTag, "(Request) Characteristic write: " + companion.toHex(value));
            }

            @Override // android.bluetooth.BluetoothGattCallback
            public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                super.onMtuChanged(gatt, mtu, status);
                this.this$0.aapsLogger.debug(LTag.PUMPBTCOMM, "MTU changed: " + mtu);
            }
        };
        this.mScanCallback = new ScanCallback() { // from class: app.aaps.pump.lenomed.service.BLECommonService$mScanCallback$1
            @Override // android.bluetooth.le.ScanCallback
            public void onScanResult(int callbackType, ScanResult result) {
                String name;
                Intrinsics.checkNotNullParameter(result, "result");
                BluetoothDevice device = result.getDevice();
                ScanRecord scanRecord = result.getScanRecord();
                if (scanRecord == null || (name = device.getName()) == null) {
                    return;
                }
                if (StringsKt.startsWith$default(name, "Ai", false, 2, (Object) null) && name.length() == 11) {
                    String strSubstring = name.substring(2);
                    Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
                    String str = strSubstring;
                    for (int i = 0; i < str.length(); i++) {
                        if (!Character.isDigit(str.charAt(i))) {
                            return;
                        }
                    }
                    SparseArray<byte[]> manufacturerSpecificData = scanRecord.getManufacturerSpecificData();
                    if (manufacturerSpecificData == null) {
                        return;
                    }
                    int size = manufacturerSpecificData.size();
                    for (int i2 = 0; i2 < size; i2++) {
                        byte[] bArrValueAt = manufacturerSpecificData.valueAt(i2);
                        if (bArrValueAt != null && bArrValueAt.length >= 4) {
                            BLECommonService bLECommonService = this.this$0;
                            Intrinsics.checkNotNull(device);
                            bLECommonService.parseBroadcastPacket(device, bArrValueAt);
                            return;
                        }
                    }
                }
            }

            @Override // android.bluetooth.le.ScanCallback
            public void onScanFailed(int errorCode) {
                this.this$0.aapsLogger.error(LTag.PUMPBTCOMM, "Broadcast scan failed with error: " + errorCode);
            }
        };
    }

    private final BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) this.context.getSystemService("bluetooth");
        if (bluetoothManager != null) {
            return bluetoothManager.getAdapter();
        }
        return null;
    }

    /* JADX INFO: renamed from: isConnected, reason: from getter */
    public final boolean getIsConnected() {
        return this.isConnected;
    }

    public final void setConnected(boolean z) {
        this.isConnected = z;
    }

    /* JADX INFO: renamed from: isConnecting, reason: from getter */
    public final boolean getIsConnecting() {
        return this.isConnecting;
    }

    public final void setConnecting(boolean z) {
        this.isConnecting = z;
    }

    private final int getMsgSequence() {
        int i = this.mSequence;
        int i2 = i % 255;
        int i3 = i + 1;
        this.mSequence = i3;
        if (i3 == 255) {
            this.mSequence = 0;
        }
        return i2;
    }

    public final synchronized boolean connect(String from, String address) {
        Intrinsics.checkNotNullParameter(from, "from");
        if (ActivityCompat.checkSelfPermission(this.context, "android.permission.BLUETOOTH_CONNECT") != 0) {
            ToastUtils toastUtils = ToastUtils.INSTANCE;
            Context context = this.context;
            toastUtils.errorToast(context, context.getString(R.string.need_connect_permission));
            this.aapsLogger.error(LTag.PUMPBTCOMM, "Missing permission: " + from);
            return false;
        }
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "Initializing Bluetooth");
        if (getBluetoothAdapter() == null) {
            this.aapsLogger.error(LTag.PUMPBTCOMM, "Unable to obtain Bluetooth adapter");
            return false;
        }
        if (address == null) {
            this.aapsLogger.error(LTag.PUMPBTCOMM, "Unspecified address");
            return false;
        }
        BluetoothGatt bluetoothGatt = this.bluetoothGatt;
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            SystemClock.sleep(200L);
            bluetoothGatt.close();
            SystemClock.sleep(200L);
            this.bluetoothGatt = null;
        }
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        BluetoothDevice remoteDevice = bluetoothAdapter != null ? bluetoothAdapter.getRemoteDevice(address) : null;
        if (remoteDevice == null) {
            this.aapsLogger.error(LTag.PUMPBTCOMM, "Device not found, unable to connect from: " + from);
            return false;
        }
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "Attempting to create new connection from: " + from);
        this.connectDeviceName = remoteDevice.getName();
        this.bluetoothGatt = remoteDevice.connectGatt(this.context, false, this.mGattCallback);
        this.isConnected = false;
        this.isConnecting = true;
        return true;
    }

    public final synchronized void stopConnecting() {
        this.isConnecting = false;
    }

    public final synchronized void disconnect(String from) {
        BluetoothGatt bluetoothGatt;
        BluetoothGattCharacteristic bluetoothGattCharacteristic;
        Intrinsics.checkNotNullParameter(from, "from");
        if (ActivityCompat.checkSelfPermission(this.context, "android.permission.BLUETOOTH_CONNECT") != 0) {
            this.aapsLogger.error(LTag.PUMPBTCOMM, "Missing permission: " + from);
            return;
        }
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "Disconnecting from: " + from);
        ScheduledFuture<?> scheduledFuture = this.scheduledDisconnection;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        this.scheduledDisconnection = null;
        if (getBluetoothAdapter() != null && (bluetoothGatt = this.bluetoothGatt) != null && (bluetoothGattCharacteristic = this.uartIndicate) != null) {
            if (bluetoothGatt != null) {
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, false);
            }
            BluetoothGatt bluetoothGatt2 = this.bluetoothGatt;
            if (bluetoothGatt2 != null) {
                bluetoothGatt2.disconnect();
            }
            this.isConnected = false;
            SystemClock.sleep(2000L);
            return;
        }
        boolean z = true;
        this.aapsLogger.error(LTag.PUMPBTCOMM, "Cannot disconnect: (bluetoothAdapter == null) " + (getBluetoothAdapter() == null));
        this.aapsLogger.error(LTag.PUMPBTCOMM, "Cannot disconnect: (bluetoothGatt == null) " + (this.bluetoothGatt == null));
        AAPSLogger aAPSLogger = this.aapsLogger;
        LTag lTag = LTag.PUMPBTCOMM;
        if (this.uartIndicate != null) {
            z = false;
        }
        aAPSLogger.error(lTag, "Cannot disconnect: (uartIndicate == null) " + z);
        this.isConnected = false;
    }

    public final synchronized void close() {
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "Closing Bluetooth adapter");
        BluetoothGatt bluetoothGatt = this.bluetoothGatt;
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
        this.bluetoothGatt = null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final int indexOfSequence(byte[] bArr, byte[] bArr2) {
        int length;
        if (bArr2.length != 0 && bArr.length >= bArr2.length && (length = bArr.length - bArr2.length) >= 0) {
            int i = 0;
            while (true) {
                int length2 = bArr2.length;
                for (int i2 = 0; i2 < length2; i2++) {
                    if (bArr[i + i2] != bArr2[i2]) {
                        if (i == length) {
                            break;
                        }
                        i++;
                    }
                }
                return i;
            }
        }
        return -1;
    }

    private final synchronized void writeCharacteristicNoResponse(final BluetoothGattCharacteristic characteristic, final byte[] data) {
        new Thread(new Runnable() { // from class: app.aaps.pump.lenomed.service.BLECommonService$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                BLECommonService.writeCharacteristicNoResponse$lambda$0(this.f$0, characteristic, data);
            }
        }).start();
        SystemClock.sleep(WRITE_DELAY_MILLIS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void writeCharacteristicNoResponse$lambda$0(BLECommonService bLECommonService, BluetoothGattCharacteristic bluetoothGattCharacteristic, byte[] bArr) {
        synchronized (bLECommonService) {
            SystemClock.sleep(WRITE_DELAY_MILLIS);
            if (bLECommonService.getBluetoothAdapter() != null && bLECommonService.bluetoothGatt != null) {
                bluetoothGattCharacteristic.setValue(bArr);
                bluetoothGattCharacteristic.setWriteType(1);
                BluetoothGatt bluetoothGatt = bLECommonService.bluetoothGatt;
                if (bluetoothGatt != null) {
                    bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                }
                Unit unit = Unit.INSTANCE;
                return;
            }
            bLECommonService.aapsLogger.error("BluetoothAdapter not initialized_ERROR");
            bLECommonService.isConnecting = false;
            bLECommonService.isConnected = false;
        }
    }

    private final BluetoothGattCharacteristic getUartWriteBTGattChar() {
        BluetoothGattCharacteristic bluetoothGattCharacteristic = this.uartWrite;
        if (bluetoothGattCharacteristic != null) {
            return bluetoothGattCharacteristic;
        }
        BluetoothGattCharacteristic bluetoothGattCharacteristic2 = new BluetoothGattCharacteristic(UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"), 12, 0);
        this.uartWrite = bluetoothGattCharacteristic2;
        return bluetoothGattCharacteristic2;
    }

    private final List<BluetoothGattService> getSupportedGattServices() {
        BluetoothGatt bluetoothGatt;
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "getSupportedGattServices");
        if (getBluetoothAdapter() != null && (bluetoothGatt = this.bluetoothGatt) != null) {
            if (bluetoothGatt != null) {
                return bluetoothGatt.getServices();
            }
            return null;
        }
        this.aapsLogger.error("BluetoothAdapter not initialized_ERROR");
        this.isConnecting = false;
        this.isConnected = false;
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final synchronized void findCharacteristic() {
        List<BluetoothGattService> supportedGattServices = getSupportedGattServices();
        if (supportedGattServices == null) {
            return;
        }
        Iterator<BluetoothGattService> it = supportedGattServices.iterator();
        while (it.hasNext()) {
            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : it.next().getCharacteristics()) {
                String string = bluetoothGattCharacteristic.getUuid().toString();
                Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
                if (Intrinsics.areEqual("0000ffe1-0000-1000-8000-00805f9b34fb", string)) {
                    this.uartIndicate = bluetoothGattCharacteristic;
                    BluetoothGatt bluetoothGatt = this.bluetoothGatt;
                    if (bluetoothGatt != null) {
                        bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                    }
                    BluetoothGattCharacteristic bluetoothGattCharacteristic2 = this.uartIndicate;
                    Intrinsics.checkNotNull(bluetoothGattCharacteristic2);
                    BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic2.getDescriptor(UUID.fromString(CHARACTERISTIC_CONFIG_UUID));
                    Intrinsics.checkNotNullExpressionValue(descriptor, "getDescriptor(...)");
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    BluetoothGatt bluetoothGatt2 = this.bluetoothGatt;
                    if (bluetoothGatt2 != null) {
                        bluetoothGatt2.writeDescriptor(descriptor);
                    }
                }
                if (Intrinsics.areEqual("0000ffe1-0000-1000-8000-00805f9b34fb", string)) {
                    this.uartWrite = bluetoothGattCharacteristic;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final synchronized void onConnectionStateChangeSynchronized(BluetoothGatt gatt, int newState) {
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "onConnectionStateChange newState : " + newState);
        if (newState == 0) {
            close();
            this.isConnected = false;
            this.isConnecting = false;
            startBroadcastScan();
            this.rxBus.send(new EventPumpStatusChanged(EventPumpStatusChanged.Status.DISCONNECTED));
            this.aapsLogger.debug(LTag.PUMPBTCOMM, "Device was disconnected " + gatt.getDevice().getName());
        } else if (newState == 2) {
            gatt.discoverServices();
            stopBroadcastScan();
            this.rxBus.send(new EventPumpStatusChanged(EventPumpStatusChanged.Status.CONNECTED));
        }
    }

    public final void setRequestMtu(int mtu) {
        BluetoothGatt bluetoothGatt = this.bluetoothGatt;
        if (bluetoothGatt != null) {
            bluetoothGatt.requestMtu(mtu);
        }
        SystemClock.sleep(1000L);
    }

    public final synchronized void sendMessage(LenomedPacket message, long waitMillis) {
        Intrinsics.checkNotNullParameter(message, "message");
        int msgSequence = getMsgSequence();
        this.aapsLogger.debug(LTag.PUMPBTCOMM, ">>>>> Sending " + message.getFriendlyName() + " Sequence >>" + msgSequence);
        byte[] bArrEncode = message.encode(msgSequence);
        this.processedMessageByte = bArrEncode;
        if (this.bluetoothGatt == null) {
            this.aapsLogger.debug(LTag.PUMPBTCOMM, ">>>>> Ignoring (not connected) " + message.getFriendlyName());
            return;
        }
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "sendMessage() Before send, mSendQueue.size: " + this.mSendQueue.size());
        writeCharacteristicNoResponse(getUartWriteBTGattChar(), bArrEncode);
        synchronized (this.mSendQueue) {
            if (this.mSendQueue.size() > 20) {
                this.aapsLogger.warn(LTag.PUMPBTCOMM, "mSendQueue size >20, clearing to prevent overflow");
                this.mSendQueue.clear();
            }
            int msgType = message.getMsgType() & 255;
            this.mSendQueue.add(new Triple<>(message, Integer.valueOf(msgType), bArrEncode));
            AAPSLogger aAPSLogger = this.aapsLogger;
            LTag lTag = LTag.PUMPBTCOMM;
            String string = Integer.toString(msgType, CharsKt.checkRadix(16));
            Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
            aAPSLogger.debug(lTag, "Added message to queue, expected response command: 0x" + string + ", message: " + message.getFriendlyName());
            Unit unit = Unit.INSTANCE;
        }
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "sendMessage() After send, mSendQueue.size: " + this.mSendQueue.size());
        synchronized (message) {
            try {
                ConcurrencyKt.waitMillis$default(message, waitMillis, 0, 2, null);
            } catch (InterruptedException e) {
                this.aapsLogger.error(LTag.PUMPBTCOMM, "sendMessage InterruptedException: " + e.getMessage(), e);
            }
            Unit unit2 = Unit.INSTANCE;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void processResponseMessage(byte[] data) {
        processMessage(data, false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void processSettingResponseMessage(byte[] data) {
        processMessage(data, true);
    }

    static /* synthetic */ void processMessage$default(BLECommonService bLECommonService, byte[] bArr, boolean z, int i, Object obj) {
        if ((i & 2) != 0) {
            z = false;
        }
        bLECommonService.processMessage(bArr, z);
    }

    private final void processMessage(byte[] data, boolean isSetting) {
        LenomedPacket lenomedPacketFindMessage;
        this.isConnected = true;
        this.isConnecting = false;
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "Processing packet: " + LenomedPacket.INSTANCE.toHex(data));
        if (data.length == 0) {
            this.aapsLogger.error(LTag.PUMPBTCOMM, "Empty packet received");
            return;
        }
        int i = data[0] & 255;
        AAPSLogger aAPSLogger = this.aapsLogger;
        LTag lTag = LTag.PUMPBTCOMM;
        String string = Integer.toString(i, CharsKt.checkRadix(16));
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        aAPSLogger.debug(lTag, "Received command: 0x" + string);
        if (isSetting) {
            lenomedPacketFindMessage = this.lenomedSettingResponseMessageHashTable.findMessage(i);
        } else {
            lenomedPacketFindMessage = this.lenomedResponseMessageHashTable.findMessage(i);
        }
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "<<<<< Processing " + lenomedPacketFindMessage.getFriendlyName() + StringUtils.SPACE + LenomedPacket.INSTANCE.toHex(data));
        try {
            lenomedPacketFindMessage.handleMessage(data);
            if (lenomedPacketFindMessage.getFailed()) {
                return;
            }
            matchAndNotifyQueue(i, lenomedPacketFindMessage);
        } catch (Exception e) {
            this.aapsLogger.error(LTag.PUMPBTCOMM, "Error processing " + lenomedPacketFindMessage.getFriendlyName() + ": " + e.getMessage(), e);
        }
    }

    private final void matchAndNotifyQueue(int receivedCommand, LenomedPacket handler) {
        synchronized (this.mSendQueue) {
            Iterator<Triple<LenomedPacket, Integer, byte[]>> it = this.mSendQueue.iterator();
            Intrinsics.checkNotNullExpressionValue(it, "iterator(...)");
            while (it.hasNext()) {
                Triple<LenomedPacket, Integer, byte[]> next = it.next();
                Intrinsics.checkNotNullExpressionValue(next, "next(...)");
                Triple<LenomedPacket, Integer, byte[]> triple = next;
                if (triple.getSecond().intValue() == receivedCommand) {
                    triple.getFirst().setReceived();
                    synchronized (triple.getFirst()) {
                        triple.getFirst().notifyAll();
                        Unit unit = Unit.INSTANCE;
                    }
                    it.remove();
                    this.aapsLogger.debug(LTag.PUMPBTCOMM, "Matched and removed from queue: " + triple.getFirst().getFriendlyName());
                    return;
                }
            }
            Unit unit2 = Unit.INSTANCE;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r9v3 */
    /* JADX WARN: Type inference failed for: r9v4, types: [boolean, int] */
    /* JADX WARN: Type inference failed for: r9v5 */
    /* JADX WARN: Type inference fix 'apply assigned field type' failed
    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: class jadx.core.dex.instructions.args.ArgType$UnknownArg
    	at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:593)
    	at jadx.core.dex.attributes.nodes.ClassTypeVarsAttr.getTypeVarsMapFor(ClassTypeVarsAttr.java:35)
    	at jadx.core.dex.nodes.utils.TypeUtils.replaceClassGenerics(TypeUtils.java:177)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.insertExplicitUseCast(FixTypesVisitor.java:397)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.tryFieldTypeWithNewCasts(FixTypesVisitor.java:359)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.applyFieldType(FixTypesVisitor.java:309)
    	at jadx.core.dex.visitors.typeinference.FixTypesVisitor.visit(FixTypesVisitor.java:94)
     */
    public final void parseBroadcastPacket(BluetoothDevice device, byte[] data) {
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "🚨 Broadcast raw data from " + device.getName() + " (" + data.length + " bytes): " + ArraysKt.joinToString$default(data, (CharSequence) StringUtils.SPACE, (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, new Function1() { // from class: app.aaps.pump.lenomed.service.BLECommonService$$ExternalSyntheticLambda0
            @Override // kotlin.jvm.functions.Function1
            public final Object invoke(Object obj) {
                return BLECommonService.parseBroadcastPacket$lambda$0(((Byte) obj).byteValue());
            }
        }, 30, (Object) null));
        StringBuilder sb = new StringBuilder("Byte index: ");
        int length = data.length;
        for (int i = 0; i < length; i++) {
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%02X", Arrays.copyOf(new Object[]{Byte.valueOf(data[i])}, 1));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            sb.append("[" + i + "]=0x" + str + StringUtils.SPACE);
        }
        AAPSLogger aAPSLogger = this.aapsLogger;
        LTag lTag = LTag.PUMPBTCOMM;
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        aAPSLogger.debug(lTag, string);
        if (data.length < 4) {
            this.aapsLogger.warn(LTag.PUMPBTCOMM, "Broadcast data too short, ignored");
            return;
        }
        byte[] byteArray = CollectionsKt.toByteArray(ArraysKt.takeLast(data, 4));
        int i2 = byteArray[0] & 255;
        double d = ((double) (((byteArray[2] & 255) * 256) + (byteArray[1] & 255))) / 100.0d;
        byte b = byteArray[3];
        boolean z = (b & 128) != 0;
        boolean z2 = (b & 64) != 0;
        ?? r9 = (b & 32) != 0 ? 1 : 0;
        boolean z3 = (b & 16) != 0;
        int i3 = b & 15;
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "🚨 Ai Pump Broadcast → " + device.getName() + " | Battery=" + i2 + "% | Insulin=" + d + " U | TempBasal=" + (z ? "ON" : "OFF") + " | Infusion=" + (z2 ? "OFF" : "ON") + " | Blocked=" + ((boolean) r9) + " | Error=" + z3 + " | ProtocolVer=" + i3);
        String name = device.getName();
        LenomedPump lenomedPump = this.lenomedPump;
        Intrinsics.checkNotNull(name);
        lenomedPump.setSerialNo(name);
        this.lenomedPump.setSystemRemainBattery(i2);
        this.lenomedPump.setSystemRemainInsulin(d);
        this.lenomedPump.setInfusionEnabled(!z2);
        this.lenomedPump.setBlockStatus(r9);
        this.lenomedPump.setBuildVersion(i3);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final CharSequence parseBroadcastPacket$lambda$0(byte b) {
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String str = String.format("%02X", Arrays.copyOf(new Object[]{Byte.valueOf(b)}, 1));
        Intrinsics.checkNotNullExpressionValue(str, "format(...)");
        return str;
    }

    public final void startBroadcastScan() {
        BluetoothLeScanner bluetoothLeScanner;
        if (ActivityCompat.checkSelfPermission(this.context, "android.permission.BLUETOOTH_SCAN") != 0) {
            this.aapsLogger.error(LTag.PUMPBTCOMM, "Missing BLUETOOTH_SCAN permission");
            return;
        }
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null || (bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner()) == null) {
            return;
        }
        bluetoothLeScanner.startScan((List<ScanFilter>) null, new ScanSettings.Builder().setScanMode(2).build(), this.mScanCallback);
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "Started broadcast scan for Ai pumps (every 10s)");
    }

    public final void stopBroadcastScan() {
        BluetoothLeScanner bluetoothLeScanner;
        if (ActivityCompat.checkSelfPermission(this.context, "android.permission.BLUETOOTH_SCAN") != 0) {
            return;
        }
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter != null && (bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner()) != null) {
            bluetoothLeScanner.stopScan(this.mScanCallback);
        }
        this.aapsLogger.debug(LTag.PUMPBTCOMM, "Stopped broadcast scan");
    }
}