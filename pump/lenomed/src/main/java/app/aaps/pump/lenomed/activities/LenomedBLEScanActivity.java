package app.aaps.pump.lenomed.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import app.aaps.core.interfaces.pump.BlePreCheck;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.core.interfaces.sharedPreferences.SP;
import app.aaps.core.ui.activities.TranslatedDaggerAppCompatActivity;
import app.aaps.core.ui.toast.ToastUtils;
import app.aaps.pump.lenomed.R;
import app.aaps.pump.lenomed.databinding.LenomedBlescannerActivityBinding;
import app.aaps.pump.lenomed.events.EventEmbectaDeviceChange;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.helper.IntegerTokenConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/* JADX INFO: compiled from: LenomedBLEScanActivity.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000\u0093\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0007*\u0001=\u0018\u0000 D2\u00020\u0001:\u0003DEFB\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010-\u001a\u00020.2\b\u0010/\u001a\u0004\u0018\u000100H\u0015J\b\u00101\u001a\u00020.H\u0014J\b\u00102\u001a\u00020.H\u0014J\b\u00103\u001a\u000204H\u0003J\b\u00105\u001a\u000206H\u0003J\u0012\u00107\u001a\u00020.2\b\u00108\u001a\u0004\u0018\u000109H\u0002J\u0010\u0010?\u001a\u00020@2\u0006\u0010A\u001a\u000204H\u0002J\u0010\u0010B\u001a\u00020@2\u0006\u0010C\u001a\u000204H\u0002R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001e\u0010\u0010\u001a\u00020\u00118\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u001e\u0010\u0016\u001a\u00020\u00178\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u0014\u0010\u001c\u001a\b\u0018\u00010\u001dR\u00020\u0000X\u0082\u000e¢\u0006\u0002\n\u0000R&\u0010\u001e\u001a\u001a\u0012\b\u0012\u00060 R\u00020\u00000\u001fj\f\u0012\b\u0012\u00060 R\u00020\u0000`!X\u0082\u0004¢\u0006\u0002\n\u0000R\u0016\u0010\"\u001a\u0004\u0018\u00010#8BX\u0082\u0004¢\u0006\u0006\u001a\u0004\b$\u0010%R\u0010\u0010&\u001a\u0004\u0018\u00010'X\u0082\u000e¢\u0006\u0002\n\u0000R\u0016\u0010(\u001a\n **\u0004\u0018\u00010)0)X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010+\u001a\u00020,X\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010:\u001a\u00020;X\u0082\u0004¢\u0006\u0002\n\u0000R\u0010\u0010<\u001a\u00020=X\u0082\u0004¢\u0006\u0004\n\u0002\u0010>¨\u0006G"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity;", "Lapp/aaps/core/ui/activities/TranslatedDaggerAppCompatActivity;", "<init>", "()V", "sp", "Lapp/aaps/core/interfaces/sharedPreferences/SP;", "getSp", "()Lapp/aaps/core/interfaces/sharedPreferences/SP;", "setSp", "(Lapp/aaps/core/interfaces/sharedPreferences/SP;)V", "blePreCheck", "Lapp/aaps/core/interfaces/pump/BlePreCheck;", "getBlePreCheck", "()Lapp/aaps/core/interfaces/pump/BlePreCheck;", "setBlePreCheck", "(Lapp/aaps/core/interfaces/pump/BlePreCheck;)V", CoreConstants.CONTEXT_SCOPE_VALUE, "Landroid/content/Context;", "getContext", "()Landroid/content/Context;", "setContext", "(Landroid/content/Context;)V", "rxBus", "Lapp/aaps/core/interfaces/rx/bus/RxBus;", "getRxBus", "()Lapp/aaps/core/interfaces/rx/bus/RxBus;", "setRxBus", "(Lapp/aaps/core/interfaces/rx/bus/RxBus;)V", "listAdapter", "Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$ListAdapter;", "devices", "Ljava/util/ArrayList;", "Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$BluetoothDeviceItem;", "Lkotlin/collections/ArrayList;", "bluetoothAdapter", "Landroid/bluetooth/BluetoothAdapter;", "getBluetoothAdapter", "()Landroid/bluetooth/BluetoothAdapter;", "bluetoothLeScanner", "Landroid/bluetooth/le/BluetoothLeScanner;", "serviceUUID", "Ljava/util/UUID;", "kotlin.jvm.PlatformType", "binding", "Lapp/aaps/pump/lenomed/databinding/LenomedBlescannerActivityBinding;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onResume", "onPause", "startScan", "", "stopScan", "", "addBleDevice", "device", "Landroid/bluetooth/BluetoothDevice;", "mBleScanCallback", "Landroid/bluetooth/le/ScanCallback;", "debugGattCallback", "app/aaps/pump/lenomed/activities/LenomedBLEScanActivity$debugGattCallback$1", "Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$debugGattCallback$1;", "propertiesToString", "", "properties", "permissionsToString", "permissions", "Companion", "ListAdapter", "BluetoothDeviceItem", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedBLEScanActivity extends TranslatedDaggerAppCompatActivity {

    /* JADX INFO: renamed from: Companion, reason: from kotlin metadata */
    public static final Companion INSTANCE = new Companion(null);
    private static final String TAG = "LenomedBLE";
    private LenomedBlescannerActivityBinding binding;

    @Inject
    public BlePreCheck blePreCheck;
    private BluetoothLeScanner bluetoothLeScanner;

    @Inject
    public Context context;
    private ListAdapter listAdapter;

    @Inject
    public RxBus rxBus;

    @Inject
    public SP sp;
    private final ArrayList<BluetoothDeviceItem> devices = new ArrayList<>();
    private final UUID serviceUUID = UUID.fromString("0000a002-0000-1000-8000-00805f9b34fb");
    private final ScanCallback mBleScanCallback = new ScanCallback() { // from class: app.aaps.pump.lenomed.activities.LenomedBLEScanActivity$mBleScanCallback$1
        @Override // android.bluetooth.le.ScanCallback
        public void onScanResult(int callbackType, ScanResult result) {
            Intrinsics.checkNotNullParameter(result, "result");
            this.this$0.addBleDevice(result.getDevice());
        }

        @Override // android.bluetooth.le.ScanCallback
        public void onScanFailed(int errorCode) {
            Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "Scan failed with error code: " + errorCode);
        }
    };
    private final LenomedBLEScanActivity$debugGattCallback$1 debugGattCallback = new BluetoothGattCallback() { // from class: app.aaps.pump.lenomed.activities.LenomedBLEScanActivity$debugGattCallback$1
        @Override // android.bluetooth.BluetoothGattCallback
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == 0) {
                Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "Debug connection disconnected");
                if (gatt != null) {
                    gatt.close();
                    return;
                }
                return;
            }
            if (newState != 2) {
                return;
            }
            Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "Debug connection successful, discovering services...");
            if (gatt != null) {
                gatt.discoverServices();
            }
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services;
            if (status == 0) {
                Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "=== GATT Services & Characteristics (Debug Print) ===");
                if (gatt != null && (services = gatt.getServices()) != null) {
                    LenomedBLEScanActivity lenomedBLEScanActivity = this.this$0;
                    for (BluetoothGattService bluetoothGattService : services) {
                        Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "Service UUID: " + bluetoothGattService.getUuid());
                        List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
                        Intrinsics.checkNotNullExpressionValue(characteristics, "getCharacteristics(...)");
                        for (BluetoothGattCharacteristic bluetoothGattCharacteristic : characteristics) {
                            Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "  ├─ Characteristic UUID: " + bluetoothGattCharacteristic.getUuid());
                            Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "  │   Properties: " + lenomedBLEScanActivity.propertiesToString(bluetoothGattCharacteristic.getProperties()));
                            Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "  │   Permissions: " + lenomedBLEScanActivity.permissionsToString(bluetoothGattCharacteristic.getPermissions()));
                        }
                    }
                }
                Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "=== End of GATT Debug Print ===");
            } else {
                Log.e(LenomedBLEScanActivity.INSTANCE.getTAG(), "Service discovery failed: " + status);
            }
            if (gatt != null) {
                gatt.disconnect();
            }
        }
    };

    public final SP getSp() {
        SP sp = this.sp;
        if (sp != null) {
            return sp;
        }
        Intrinsics.throwUninitializedPropertyAccessException("sp");
        return null;
    }

    public final void setSp(SP sp) {
        Intrinsics.checkNotNullParameter(sp, "<set-?>");
        this.sp = sp;
    }

    public final BlePreCheck getBlePreCheck() {
        BlePreCheck blePreCheck = this.blePreCheck;
        if (blePreCheck != null) {
            return blePreCheck;
        }
        Intrinsics.throwUninitializedPropertyAccessException("blePreCheck");
        return null;
    }

    public final void setBlePreCheck(BlePreCheck blePreCheck) {
        Intrinsics.checkNotNullParameter(blePreCheck, "<set-?>");
        this.blePreCheck = blePreCheck;
    }

    public final Context getContext() {
        Context context = this.context;
        if (context != null) {
            return context;
        }
        Intrinsics.throwUninitializedPropertyAccessException(CoreConstants.CONTEXT_SCOPE_VALUE);
        return null;
    }

    public final void setContext(Context context) {
        Intrinsics.checkNotNullParameter(context, "<set-?>");
        this.context = context;
    }

    public final RxBus getRxBus() {
        RxBus rxBus = this.rxBus;
        if (rxBus != null) {
            return rxBus;
        }
        Intrinsics.throwUninitializedPropertyAccessException("rxBus");
        return null;
    }

    public final void setRxBus(RxBus rxBus) {
        Intrinsics.checkNotNullParameter(rxBus, "<set-?>");
        this.rxBus = rxBus;
    }

    private final BluetoothAdapter getBluetoothAdapter() {
        BluetoothManager bluetoothManager = (BluetoothManager) getContext().getSystemService("bluetooth");
        if (bluetoothManager != null) {
            return bluetoothManager.getAdapter();
        }
        return null;
    }

    /* JADX INFO: compiled from: LenomedBLEScanActivity.kt */
    @Metadata(d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003R\u0014\u0010\u0004\u001a\u00020\u0005X\u0086D¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007¨\u0006\b"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$Companion;", "", "<init>", "()V", "TAG", "", "getTAG", "()Ljava/lang/String;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public final String getTAG() {
            return LenomedBLEScanActivity.TAG;
        }
    }

    @Override // app.aaps.core.ui.activities.TranslatedDaggerAppCompatActivity, dagger.android.support.DaggerAppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LenomedBlescannerActivityBinding lenomedBlescannerActivityBindingInflate = LenomedBlescannerActivityBinding.inflate(getLayoutInflater());
        Intrinsics.checkNotNullExpressionValue(lenomedBlescannerActivityBindingInflate, "inflate(...)");
        this.binding = lenomedBlescannerActivityBindingInflate;
        LenomedBlescannerActivityBinding lenomedBlescannerActivityBinding = null;
        if (lenomedBlescannerActivityBindingInflate == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedBlescannerActivityBindingInflate = null;
        }
        setContentView(lenomedBlescannerActivityBindingInflate.getRoot());
        setRequestedOrientation(1);
        setTitle(getString(R.string.lenomed_pairing));
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        ActionBar supportActionBar2 = getSupportActionBar();
        if (supportActionBar2 != null) {
            supportActionBar2.setDisplayShowHomeEnabled(true);
        }
        BlePreCheck.prerequisitesCheck$default(getBlePreCheck(), this, null, 2, null);
        this.listAdapter = new ListAdapter();
        LenomedBlescannerActivityBinding lenomedBlescannerActivityBinding2 = this.binding;
        if (lenomedBlescannerActivityBinding2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedBlescannerActivityBinding2 = null;
        }
        ListView listView = lenomedBlescannerActivityBinding2.bleScannerListview;
        LenomedBlescannerActivityBinding lenomedBlescannerActivityBinding3 = this.binding;
        if (lenomedBlescannerActivityBinding3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedBlescannerActivityBinding3 = null;
        }
        listView.setEmptyView(lenomedBlescannerActivityBinding3.bleScannerNoDevice);
        LenomedBlescannerActivityBinding lenomedBlescannerActivityBinding4 = this.binding;
        if (lenomedBlescannerActivityBinding4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
        } else {
            lenomedBlescannerActivityBinding = lenomedBlescannerActivityBinding4;
        }
        lenomedBlescannerActivityBinding.bleScannerListview.setAdapter((android.widget.ListAdapter) this.listAdapter);
        ListAdapter listAdapter = this.listAdapter;
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(getContext(), "android.permission.BLUETOOTH_SCAN") == 0) {
            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                }
                this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
                startScan();
                return;
            }
            return;
        }
        ToastUtils.INSTANCE.errorToast(getContext(), getContext().getString(app.aaps.core.ui.R.string.need_connect_permission));
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        super.onPause();
        if (ActivityCompat.checkSelfPermission(getContext(), "android.permission.BLUETOOTH_SCAN") == 0) {
            stopScan();
        }
    }

    private final int startScan() {
        try {
            Log.d("serviceUUID=", String.valueOf(new ParcelUuid(this.serviceUUID)));
            ArrayList arrayList = new ArrayList();
            ScanFilter scanFilterBuild = new ScanFilter.Builder().build();
            Intrinsics.checkNotNull(scanFilterBuild);
            arrayList.add(scanFilterBuild);
            ScanSettings scanSettingsBuild = new ScanSettings.Builder().setScanMode(2).setReportDelay(0L).build();
            BluetoothLeScanner bluetoothLeScanner = this.bluetoothLeScanner;
            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.startScan(arrayList, scanSettingsBuild, this.mBleScanCallback);
            }
            return Log.d("serviceUUID=", String.valueOf(new ParcelUuid(this.serviceUUID)));
        } catch (IllegalStateException unused) {
            return Log.d("ignored serviceUUID=", String.valueOf(new ParcelUuid(this.serviceUUID)));
        }
    }

    private final Object stopScan() {
        try {
            BluetoothLeScanner bluetoothLeScanner = this.bluetoothLeScanner;
            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(this.mBleScanCallback);
            }
            return Integer.valueOf(Log.d("", "Stopped scan"));
        } catch (IllegalStateException unused) {
            return Unit.INSTANCE;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void addBleDevice(BluetoothDevice device) {
        String name;
        if (ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT") != 0) {
            ToastUtils.INSTANCE.errorToast(getContext(), getContext().getString(app.aaps.core.ui.R.string.need_connect_permission));
            return;
        }
        if (device == null || device.getName() == null || Intrinsics.areEqual(device.getName(), "") || (name = device.getName()) == null) {
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
            BluetoothDeviceItem bluetoothDeviceItem = new BluetoothDeviceItem(this, device);
            if (this.devices.contains(bluetoothDeviceItem)) {
                return;
            }
            this.devices.add(bluetoothDeviceItem);
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: app.aaps.pump.lenomed.activities.LenomedBLEScanActivity$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    LenomedBLEScanActivity.addBleDevice$lambda$1(this.f$0);
                }
            });
            Log.d(TAG, "Added device: " + device.getName() + " with address: " + device.getAddress());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void addBleDevice$lambda$1(LenomedBLEScanActivity lenomedBLEScanActivity) {
        ListAdapter listAdapter = lenomedBLEScanActivity.listAdapter;
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    /* JADX INFO: compiled from: LenomedBLEScanActivity.kt */
    @Metadata(d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0080\u0004\u0018\u00002\u00020\u0001:\u0001\u0011B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\u0004\u001a\u00020\u0005H\u0016J\u0014\u0010\u0006\u001a\u00060\u0007R\u00020\b2\u0006\u0010\t\u001a\u00020\u0005H\u0016J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\t\u001a\u00020\u0005H\u0016J$\u0010\f\u001a\u00020\r2\u0006\u0010\t\u001a\u00020\u00052\b\u0010\u000e\u001a\u0004\u0018\u00010\r2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u0016¨\u0006\u0012"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$ListAdapter;", "Landroid/widget/BaseAdapter;", "<init>", "(Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity;)V", "getCount", "", "getItem", "Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$BluetoothDeviceItem;", "Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity;", IntegerTokenConverter.CONVERTER_KEY, "getItemId", "", "getView", "Landroid/view/View;", "convertView", "parent", "Landroid/view/ViewGroup;", "ViewHolder", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public final class ListAdapter extends BaseAdapter {
        @Override // android.widget.Adapter
        public long getItemId(int i) {
            return 0L;
        }

        public ListAdapter() {
        }

        @Override // android.widget.Adapter
        public int getCount() {
            return LenomedBLEScanActivity.this.devices.size();
        }

        @Override // android.widget.Adapter
        public BluetoothDeviceItem getItem(int i) {
            Object obj = LenomedBLEScanActivity.this.devices.get(i);
            Intrinsics.checkNotNullExpressionValue(obj, "get(...)");
            return (BluetoothDeviceItem) obj;
        }

        @Override // android.widget.Adapter
        public View getView(int i, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = View.inflate(LenomedBLEScanActivity.this.getApplicationContext(), R.layout.lenomed_blescanner_item, null);
                Intrinsics.checkNotNull(convertView);
                viewHolder = new ViewHolder(this, convertView);
                convertView.setTag(viewHolder);
            } else {
                Object tag = convertView.getTag();
                Intrinsics.checkNotNull(tag, "null cannot be cast to non-null type app.aaps.pump.lenomed.activities.LenomedBLEScanActivity.ListAdapter.ViewHolder");
                viewHolder = (ViewHolder) tag;
            }
            viewHolder.setData(getItem(i));
            Intrinsics.checkNotNull(convertView);
            return convertView;
        }

        /* JADX INFO: compiled from: LenomedBLEScanActivity.kt */
        @Metadata(d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u0082\u0004\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\f\u001a\u00020\r2\u0006\u0010\u0002\u001a\u00020\u0003H\u0017J\u0014\u0010\u000e\u001a\u00020\r2\n\u0010\u000f\u001a\u00060\u0007R\u00020\bH\u0007R\u0012\u0010\u0006\u001a\u00060\u0007R\u00020\bX\u0082.¢\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\nX\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006\u0010"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$ListAdapter$ViewHolder;", "Landroid/view/View$OnClickListener;", "v", "Landroid/view/View;", "<init>", "(Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$ListAdapter;Landroid/view/View;)V", "item", "Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$BluetoothDeviceItem;", "Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity;", "name", "Landroid/widget/TextView;", "address", "onClick", "", "setData", "data", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
        private final class ViewHolder implements View.OnClickListener {
            private final TextView address;
            private BluetoothDeviceItem item;
            private final TextView name;
            final /* synthetic */ ListAdapter this$0;

            public ViewHolder(ListAdapter listAdapter, View v) {
                Intrinsics.checkNotNullParameter(v, "v");
                this.this$0 = listAdapter;
                View viewFindViewById = v.findViewById(R.id.ble_name);
                Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
                this.name = (TextView) viewFindViewById;
                View viewFindViewById2 = v.findViewById(R.id.ble_address);
                Intrinsics.checkNotNullExpressionValue(viewFindViewById2, "findViewById(...)");
                this.address = (TextView) viewFindViewById2;
                v.setOnClickListener(this);
            }

            @Override // android.view.View.OnClickListener
            public void onClick(View v) {
                Intrinsics.checkNotNullParameter(v, "v");
                LenomedBLEScanActivity.this.getSp().putString(app.aaps.core.utils.R.string.key_pump_lenomed_name, this.name.getText().toString());
                SP sp = LenomedBLEScanActivity.this.getSp();
                int i = app.aaps.core.utils.R.string.key_pump_lenomed_address;
                BluetoothDeviceItem bluetoothDeviceItem = this.item;
                BluetoothDeviceItem bluetoothDeviceItem2 = null;
                if (bluetoothDeviceItem == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("item");
                    bluetoothDeviceItem = null;
                }
                String address = bluetoothDeviceItem.getDevice().getAddress();
                Intrinsics.checkNotNullExpressionValue(address, "getAddress(...)");
                sp.putString(i, address);
                LenomedBLEScanActivity.this.getRxBus().send(new EventEmbectaDeviceChange());
                String tag = LenomedBLEScanActivity.INSTANCE.getTAG();
                BluetoothDeviceItem bluetoothDeviceItem3 = this.item;
                if (bluetoothDeviceItem3 == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("item");
                    bluetoothDeviceItem3 = null;
                }
                Log.d(tag, "Debug: Connecting to " + bluetoothDeviceItem3.getDevice().getName() + " for GATT discovery...");
                BluetoothDeviceItem bluetoothDeviceItem4 = this.item;
                if (bluetoothDeviceItem4 == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("item");
                } else {
                    bluetoothDeviceItem2 = bluetoothDeviceItem4;
                }
                bluetoothDeviceItem2.getDevice().connectGatt(LenomedBLEScanActivity.this, false, LenomedBLEScanActivity.this.debugGattCallback);
                LenomedBLEScanActivity.this.finish();
            }

            public final void setData(BluetoothDeviceItem data) {
                Intrinsics.checkNotNullParameter(data, "data");
                String name = data.getDevice().getName();
                if (name == null || Intrinsics.areEqual(name, "")) {
                    name = "(unknown)";
                }
                this.name.setText(name);
                this.address.setText(data.getDevice().getAddress());
                this.item = data;
            }
        }
    }

    /* JADX INFO: compiled from: LenomedBLEScanActivity.kt */
    @Metadata(d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\u0011\b\u0000\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0013\u0010\b\u001a\u00020\t2\b\u0010\n\u001a\u0004\u0018\u00010\u0001H\u0096\u0002J\u0018\u0010\u000b\u001a\u00020\t2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\rH\u0002J\b\u0010\u000f\u001a\u00020\u0010H\u0016R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0007¨\u0006\u0011"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity$BluetoothDeviceItem;", "", "device", "Landroid/bluetooth/BluetoothDevice;", "<init>", "(Lapp/aaps/pump/lenomed/activities/LenomedBLEScanActivity;Landroid/bluetooth/BluetoothDevice;)V", "getDevice", "()Landroid/bluetooth/BluetoothDevice;", "equals", "", "other", "stringEquals", "arg1", "", "arg2", "hashCode", "", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public final class BluetoothDeviceItem {
        private final BluetoothDevice device;
        final /* synthetic */ LenomedBLEScanActivity this$0;

        public BluetoothDeviceItem(LenomedBLEScanActivity lenomedBLEScanActivity, BluetoothDevice device) {
            Intrinsics.checkNotNullParameter(device, "device");
            this.this$0 = lenomedBLEScanActivity;
            this.device = device;
        }

        public final BluetoothDevice getDevice() {
            return this.device;
        }

        public boolean equals(Object other) {
            if (!(other instanceof BluetoothDeviceItem)) {
                return false;
            }
            String address = this.device.getAddress();
            Intrinsics.checkNotNullExpressionValue(address, "getAddress(...)");
            String address2 = ((BluetoothDeviceItem) other).device.getAddress();
            Intrinsics.checkNotNullExpressionValue(address2, "getAddress(...)");
            return stringEquals(address, address2);
        }

        private final boolean stringEquals(String arg1, String arg2) {
            try {
                return Intrinsics.areEqual(arg1, arg2);
            } catch (Exception unused) {
                return false;
            }
        }

        public int hashCode() {
            return this.device.hashCode();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String propertiesToString(int properties) {
        ArrayList arrayList = new ArrayList();
        if ((properties & 2) != 0) {
            arrayList.add("READ");
        }
        if ((properties & 8) != 0) {
            arrayList.add("WRITE");
        }
        if ((properties & 4) != 0) {
            arrayList.add("WRITE_NO_RESPONSE");
        }
        if ((properties & 16) != 0) {
            arrayList.add("NOTIFY");
        }
        if ((properties & 32) != 0) {
            arrayList.add("INDICATE");
        }
        return CollectionsKt.joinToString$default(arrayList, " | ", null, null, 0, null, null, 62, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String permissionsToString(int permissions) {
        ArrayList arrayList = new ArrayList();
        if ((permissions & 1) != 0) {
            arrayList.add("READ");
        }
        if ((permissions & 16) != 0) {
            arrayList.add("WRITE");
        }
        return CollectionsKt.joinToString$default(arrayList, " | ", null, null, 0, null, null, 62, null);
    }
}