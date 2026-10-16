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
            LenomedBLEScanActivity.this.addBleDevice(result.getDevice());
        }

        @Override // android.bluetooth.le.ScanCallback
        public void onScanFailed(int errorCode) {
            Log.d(LenomedBLEScanActivity.INSTANCE.getTAG(), "Scan failed with error code: " + errorCode);
        }
    };
    private final android.bluetooth.BluetoothGattCallback debugGattCallback = new BluetoothGattCallback() { // from class: app.aaps.pump.lenomed.activities.LenomedBLEScanActivity$debugGattCallback$1
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
                    LenomedBLEScanActivity lenomedBLEScanActivity = LenomedBLEScanActivity.this;
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
        Intrinsics.throwUninitializedPropertyAccessException("");
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
        getBlePreCheck().prerequisitesCheck(this, null);
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
        if (name.startsWith("Ai") && name.length() == 11) {
            String strSubstring = name.substring(2);
            Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
            String str = strSubstring;
            for (int i = 0; i < str.length(); i++) {
                if (!Character.isDigit(str.charAt(i))) {
                    return;
                }
            }
            BluetoothDeviceItem bluetoothDeviceItem = new BluetoothDeviceItem(device);
            if (this.devices.contains(bluetoothDeviceItem)) {
                return;
            }
            this.devices.add(bluetoothDeviceItem);
            new Handler(Looper.getMainLooper()).post(() -> LenomedBLEScanActivity.addBleDevice$lambda$1(LenomedBLEScanActivity.this));
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
                viewHolder = new ViewHolder(convertView);
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
        private final class ViewHolder implements View.OnClickListener {
            private final TextView address;
            private BluetoothDeviceItem item;
            private final TextView name;

            public ViewHolder(View v) {
                Intrinsics.checkNotNullParameter(v, "v");
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
                LenomedBLEScanActivity.this.getSp().putString(app.aaps.pump.lenomed.R.string.key_pump_lenomed_name, this.name.getText().toString());
                SP sp = LenomedBLEScanActivity.this.getSp();
                int i = app.aaps.pump.lenomed.R.string.key_pump_lenomed_address;
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
    public final class BluetoothDeviceItem {
        private final BluetoothDevice device;

        public BluetoothDeviceItem(BluetoothDevice device) {
            Intrinsics.checkNotNullParameter(device, "device");
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
        return String.join(" | ", arrayList);
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
        return String.join(" | ", arrayList);
    }
}