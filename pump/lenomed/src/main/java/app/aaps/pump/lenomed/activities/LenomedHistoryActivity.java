package app.aaps.pump.lenomed.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.aaps.core.data.time.T;
import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.plugin.ActivePlugin;
import app.aaps.core.interfaces.profile.ProfileFunction;
import app.aaps.core.interfaces.queue.CommandQueue;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.AapsSchedulers;
import app.aaps.core.interfaces.rx.bus.RxBus;
import app.aaps.core.interfaces.rx.events.EventPumpStatusChanged;
import app.aaps.core.interfaces.utils.DateUtil;
import app.aaps.core.interfaces.utils.DecimalFormatter;
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy;
import app.aaps.core.ui.R;
import app.aaps.core.ui.activities.TranslatedDaggerAppCompatActivity;
import app.aaps.pump.lenomed.database.LenomedHistoryDatabases;
import app.aaps.pump.lenomed.database.LenomedHistoryRecord;
import app.aaps.pump.lenomed.database.LenomedHistoryRecordDao;
import app.aaps.pump.lenomed.databinding.LenomedHistoryActivityBinding;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.kotlin.DisposableKt;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import kotlin.Unit;
import kotlin.jvm.internal.Intrinsics;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineScopeKt;
import app.aaps.pump.lenomed.LenomedSupportKt;

/* JADX INFO: compiled from: LenomedHistoryActivity.kt */
/* JADX INFO: loaded from: classes4.dex */
public final class LenomedHistoryActivity extends TranslatedDaggerAppCompatActivity {

    private CoroutineScope eventScope;

    public final CoroutineScope getEventScope() {
        if (this.eventScope == null) {
            this.eventScope = LenomedSupportKt.lenomedIoScope();
        }
        return this.eventScope;
    }

    public final void releaseEventScope() {
        CoroutineScope scope = this.eventScope;
        if (scope != null) {
            CoroutineScopeKt.cancel(scope, null);
            this.eventScope = null;
        }
    }


    @Inject
    public AapsSchedulers aapsSchedulers;

    @Inject
    public ActivePlugin activePlugin;

    @Inject
    public AAPSLogger aapsLogger;
    private LenomedHistoryActivityBinding binding;

    @Inject
    public CommandQueue commandQueue;

    @Inject
    public DateUtil dateUtil;

    @Inject
    public DecimalFormatter decimalFormatter;

    @Inject
    public FabricPrivacy fabricPrivacy;

    @Inject
    public LenomedHistoryRecordDao lenomedHistoryRecordDao;

    @Inject
    public ProfileFunction profileFunction;

    @Inject
    public ResourceHelper rh;

    @Inject
    public RxBus rxBus;
    private final CompositeDisposable disposable = new CompositeDisposable();
    private byte showingType = 1;
    private List<LenomedHistoryRecord> historyList = new ArrayList();

    public final ProfileFunction getProfileFunction() {
        ProfileFunction profileFunction = this.profileFunction;
        if (profileFunction != null) {
            return profileFunction;
        }
        Intrinsics.throwUninitializedPropertyAccessException("profileFunction");
        return null;
    }

    public final void setProfileFunction(ProfileFunction profileFunction) {
        Intrinsics.checkNotNullParameter(profileFunction, "<set-?>");
        this.profileFunction = profileFunction;
    }

    public final AAPSLogger getAapsLogger() {
        AAPSLogger aapsLogger = this.aapsLogger;
        if (aapsLogger != null) {
            return aapsLogger;
        }
        Intrinsics.throwUninitializedPropertyAccessException("aapsLogger");
        return null;
    }

    public final void setAapsLogger(AAPSLogger aapsLogger) {
        Intrinsics.checkNotNullParameter(aapsLogger, "<set-?>");
        this.aapsLogger = aapsLogger;
    }

    public final FabricPrivacy getFabricPrivacy() {
        FabricPrivacy fabricPrivacy = this.fabricPrivacy;
        if (fabricPrivacy != null) {
            return fabricPrivacy;
        }
        Intrinsics.throwUninitializedPropertyAccessException("fabricPrivacy");
        return null;
    }

    public final void setFabricPrivacy(FabricPrivacy fabricPrivacy) {
        Intrinsics.checkNotNullParameter(fabricPrivacy, "<set-?>");
        this.fabricPrivacy = fabricPrivacy;
    }

    public final ActivePlugin getActivePlugin() {
        ActivePlugin activePlugin = this.activePlugin;
        if (activePlugin != null) {
            return activePlugin;
        }
        Intrinsics.throwUninitializedPropertyAccessException("activePlugin");
        return null;
    }

    public final void setActivePlugin(ActivePlugin activePlugin) {
        Intrinsics.checkNotNullParameter(activePlugin, "<set-?>");
        this.activePlugin = activePlugin;
    }

    public final CommandQueue getCommandQueue() {
        CommandQueue commandQueue = this.commandQueue;
        if (commandQueue != null) {
            return commandQueue;
        }
        Intrinsics.throwUninitializedPropertyAccessException("commandQueue");
        return null;
    }

    public final void setCommandQueue(CommandQueue commandQueue) {
        Intrinsics.checkNotNullParameter(commandQueue, "<set-?>");
        this.commandQueue = commandQueue;
    }

    public final LenomedHistoryRecordDao getLenomedHistoryRecordDao() {
        LenomedHistoryRecordDao lenomedHistoryRecordDao = this.lenomedHistoryRecordDao;
        if (lenomedHistoryRecordDao != null) {
            return lenomedHistoryRecordDao;
        }
        // Nothing assigns this field any more: in the build this driver came from it was filled by
        // Dagger field injection, which the dev branch replaced with Metro (see the
        // TranslatedDaggerAppCompatActivity shim). Without this the history screen would throw
        // UninitializedPropertyAccessException on its first query.
        lenomedHistoryRecordDao = LenomedHistoryDatabases.get(getApplicationContext()).historyRecordDao();
        this.lenomedHistoryRecordDao = lenomedHistoryRecordDao;
        return lenomedHistoryRecordDao;
    }

    public final void setLenomedHistoryRecordDao(LenomedHistoryRecordDao lenomedHistoryRecordDao) {
        Intrinsics.checkNotNullParameter(lenomedHistoryRecordDao, "<set-?>");
        this.lenomedHistoryRecordDao = lenomedHistoryRecordDao;
    }

    public final DateUtil getDateUtil() {
        DateUtil dateUtil = this.dateUtil;
        if (dateUtil != null) {
            return dateUtil;
        }
        Intrinsics.throwUninitializedPropertyAccessException("dateUtil");
        return null;
    }

    public final void setDateUtil(DateUtil dateUtil) {
        Intrinsics.checkNotNullParameter(dateUtil, "<set-?>");
        this.dateUtil = dateUtil;
    }

    public final AapsSchedulers getAapsSchedulers() {
        AapsSchedulers aapsSchedulers = this.aapsSchedulers;
        if (aapsSchedulers != null) {
            return aapsSchedulers;
        }
        Intrinsics.throwUninitializedPropertyAccessException("aapsSchedulers");
        return null;
    }

    public final void setAapsSchedulers(AapsSchedulers aapsSchedulers) {
        Intrinsics.checkNotNullParameter(aapsSchedulers, "<set-?>");
        this.aapsSchedulers = aapsSchedulers;
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

    public final ResourceHelper getRh() {
        ResourceHelper resourceHelper = this.rh;
        if (resourceHelper != null) {
            return resourceHelper;
        }
        Intrinsics.throwUninitializedPropertyAccessException("rh");
        return null;
    }

    public final void setRh(ResourceHelper resourceHelper) {
        Intrinsics.checkNotNullParameter(resourceHelper, "<set-?>");
        this.rh = resourceHelper;
    }

    public final DecimalFormatter getDecimalFormatter() {
        DecimalFormatter decimalFormatter = this.decimalFormatter;
        if (decimalFormatter != null) {
            return decimalFormatter;
        }
        Intrinsics.throwUninitializedPropertyAccessException("decimalFormatter");
        return null;
    }

    public final void setDecimalFormatter(DecimalFormatter decimalFormatter) {
        Intrinsics.checkNotNullParameter(decimalFormatter, "<set-?>");
        this.decimalFormatter = decimalFormatter;
    }

    /* JADX INFO: compiled from: LenomedHistoryActivity.kt */
    public static final class TypeList {
        private String name;
        private byte type;

        public TypeList(byte b, String name) {
            Intrinsics.checkNotNullParameter(name, "name");
            this.type = b;
            this.name = name;
        }

        public final String getName() {
            return this.name;
        }

        public final byte getType() {
            return this.type;
        }

        public final void setName(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            this.name = str;
        }

        public final void setType(byte b) {
            this.type = b;
        }

        public String toString() {
            return this.name;
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onResume() {
        super.onResume();
        CompositeDisposable compositeDisposable = this.disposable;
        Disposable disposableSubscribe = LenomedSupportKt.rxBusObservable(getRxBus(), getAapsLogger(), kotlin.jvm.internal.Reflection.getOrCreateKotlinClass(EventPumpStatusChanged.class), this.getEventScope(), getAapsSchedulers().getMain()).subscribe(new Consumer<EventPumpStatusChanged>() {
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventPumpStatusChanged it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedHistoryActivityBinding lenomedHistoryActivityBinding = LenomedHistoryActivity.this.binding;
                if (lenomedHistoryActivityBinding == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("binding");
                    lenomedHistoryActivityBinding = null;
                }
                lenomedHistoryActivityBinding.status.setText(getRh().gs(LenomedSupportKt.pumpStatusText(it)));
            }
        }, new Consumer<Throwable>() {
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(Throwable it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedHistoryActivity.this.getFabricPrivacy().logException(it);
            }
        });
        Intrinsics.checkNotNullExpressionValue(disposableSubscribe, "subscribe(...)");
        DisposableKt.plusAssign(compositeDisposable, disposableSubscribe);
        swapAdapter(this.showingType);
    }

    @Override // androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onPause() {
        super.onPause();
        this.disposable.clear();
        releaseEventScope();
    }

    @Override // app.aaps.core.ui.activities.TranslatedDaggerAppCompatActivity, dagger.android.support.DaggerAppCompatActivity, androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LenomedHistoryActivityBinding lenomedHistoryActivityBindingInflate = LenomedHistoryActivityBinding.inflate(getLayoutInflater());
        Intrinsics.checkNotNullExpressionValue(lenomedHistoryActivityBindingInflate, "inflate(...)");
        this.binding = lenomedHistoryActivityBindingInflate;
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding = null;
        if (lenomedHistoryActivityBindingInflate == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBindingInflate = null;
        }
        setContentView(lenomedHistoryActivityBindingInflate.getRoot());
        setTitle(getRh().gs(R.string.pump_history));
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        ActionBar supportActionBar2 = getSupportActionBar();
        if (supportActionBar2 != null) {
            supportActionBar2.setDisplayShowHomeEnabled(true);
        }
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding2 = this.binding;
        if (lenomedHistoryActivityBinding2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding2 = null;
        }
        lenomedHistoryActivityBinding2.recyclerview.setHasFixedSize(true);
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding3 = this.binding;
        if (lenomedHistoryActivityBinding3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding3 = null;
        }
        LenomedHistoryActivity lenomedHistoryActivity = this;
        lenomedHistoryActivityBinding3.recyclerview.setLayoutManager(new LinearLayoutManager(lenomedHistoryActivity));
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding4 = this.binding;
        if (lenomedHistoryActivityBinding4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding4 = null;
        }
        lenomedHistoryActivityBinding4.recyclerview.setAdapter(new RecyclerViewAdapter(this.historyList));
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding5 = this.binding;
        if (lenomedHistoryActivityBinding5 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding5 = null;
        }
        lenomedHistoryActivityBinding5.status.setVisibility(8);
        final ArrayList arrayList = new ArrayList();
        arrayList.add(new TypeList((byte) 1, getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_history_basal)));
        arrayList.add(new TypeList((byte) 2, getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_history_dailyinsulin)));
        arrayList.add(new TypeList((byte) 3, getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_history_alarm)));
        arrayList.add(new TypeList((byte) 4, getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_history_bolus)));
        arrayList.add(new TypeList((byte) 5, getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_history_extended_bolus)));
        arrayList.add(new TypeList((byte) 6, getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_history_refill)));
        arrayList.add(new TypeList((byte) 7, getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_history_basal_hourly)));
        arrayList.add(new TypeList((byte) 8, getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_history_tempbasal)));
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding6 = this.binding;
        if (lenomedHistoryActivityBinding6 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding6 = null;
        }
        lenomedHistoryActivityBinding6.typeList.setAdapter(new ArrayAdapter(lenomedHistoryActivity, android.R.layout.simple_spinner_item, arrayList));
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding7 = this.binding;
        if (lenomedHistoryActivityBinding7 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding7 = null;
        }
        lenomedHistoryActivityBinding7.reload.setOnClickListener(v -> LenomedHistoryActivity.onCreate$lambda$0(arrayList, LenomedHistoryActivity.this, v));
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding8 = this.binding;
        if (lenomedHistoryActivityBinding8 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
        } else {
            lenomedHistoryActivityBinding = lenomedHistoryActivityBinding8;
        }
        lenomedHistoryActivityBinding.typeList.setOnItemClickListener((adapterView, view, i, j) -> LenomedHistoryActivity.onCreate$lambda$1(arrayList, LenomedHistoryActivity.this, adapterView, view, i, j));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$0(ArrayList arrayList, final LenomedHistoryActivity lenomedHistoryActivity, View view) {
        Object obj;
        Iterator it = arrayList.iterator();
        while (true) {
            obj = null;
            LenomedHistoryActivityBinding lenomedHistoryActivityBinding = null;
            if (!it.hasNext()) {
                break;
            }
            Object next = it.next();
            String name = ((TypeList) next).getName();
            LenomedHistoryActivityBinding lenomedHistoryActivityBinding2 = lenomedHistoryActivity.binding;
            if (lenomedHistoryActivityBinding2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("binding");
            } else {
                lenomedHistoryActivityBinding = lenomedHistoryActivityBinding2;
            }
            if (Intrinsics.areEqual(name, lenomedHistoryActivityBinding.typeList.getText().toString())) {
                obj = next;
                break;
            }
        }
        TypeList typeList = (TypeList) obj;
        if (typeList == null) {
            return;
        }
        lenomedHistoryActivity.runOnUiThread(() -> LenomedHistoryActivity.onCreate$lambda$0$1(lenomedHistoryActivity));
        lenomedHistoryActivity.clearCardView();
        LenomedSupportKt.loadHistoryAsync(lenomedHistoryActivity.getCommandQueue(), typeList.getType(), lenomedHistoryActivity.getEventScope(), result -> {
            lenomedHistoryActivity.swapAdapter(typeList.getType());
            final LenomedHistoryActivity activity = lenomedHistoryActivity;
            activity.runOnUiThread(() -> {
                LenomedHistoryActivityBinding b = activity.binding;
                if (b == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("binding");
                    b = null;
                }
                b.reload.setVisibility(0);
                LenomedHistoryActivityBinding b2 = activity.binding;
                if (b2 == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("binding");
                    b2 = null;
                }
                b2.status.setVisibility(8);
            });
            return Unit.INSTANCE;
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$0$1(LenomedHistoryActivity lenomedHistoryActivity) {
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding = lenomedHistoryActivity.binding;
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding2 = null;
        if (lenomedHistoryActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding = null;
        }
        lenomedHistoryActivityBinding.reload.setVisibility(8);
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding3 = lenomedHistoryActivity.binding;
        if (lenomedHistoryActivityBinding3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
        } else {
            lenomedHistoryActivityBinding2 = lenomedHistoryActivityBinding3;
        }
        lenomedHistoryActivityBinding2.status.setVisibility(0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$1(ArrayList arrayList, LenomedHistoryActivity lenomedHistoryActivity, AdapterView adapterView, View view, int i, long j) {
        Object obj = arrayList.get(i);
        Intrinsics.checkNotNullExpressionValue(obj, "get(...)");
        TypeList typeList = (TypeList) obj;
        lenomedHistoryActivity.showingType = typeList.getType();
        lenomedHistoryActivity.swapAdapter(typeList.getType());
    }

    /* JADX INFO: compiled from: LenomedHistoryActivity.kt */
    public final class RecyclerViewAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
        private List<LenomedHistoryRecord> historyList;

        public RecyclerViewAdapter(List<LenomedHistoryRecord> historyList) {
            Intrinsics.checkNotNullParameter(historyList, "historyList");
            this.historyList = historyList;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public HistoryViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            Intrinsics.checkNotNullParameter(viewGroup, "viewGroup");
            View viewInflate = LayoutInflater.from(viewGroup.getContext()).inflate(app.aaps.pump.lenomed.R.layout.lenomed_history_item, viewGroup, false);
            Intrinsics.checkNotNullExpressionValue(viewInflate, "inflate(...)");
            return new HistoryViewHolder(viewInflate);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(HistoryViewHolder holder, int position) {
            String strGs;
            String strGs2;
            Intrinsics.checkNotNullParameter(holder, "holder");
            LenomedHistoryRecord lenomedHistoryRecord = this.historyList.get(position);
            holder.getTime().setText(LenomedHistoryActivity.this.getDateUtil().dateAndTimeAndSecondsString(lenomedHistoryRecord.getTimestamp()));
            holder.getValue().setText(LenomedHistoryActivity.this.getDecimalFormatter().to2Decimal(lenomedHistoryRecord.getValue()));
            TextView stringValue = holder.getStringValue();
            String stringValue2 = lenomedHistoryRecord.getStringValue();
            String str = "";
            stringValue.setText(stringValue2 != null ? stringValue2 : "");
            TextView bolusType = holder.getBolusType();
            String bolusType2 = lenomedHistoryRecord.getBolusType();
            bolusType.setText(bolusType2 != null ? bolusType2 : "");
            holder.getDuration().setText(LenomedHistoryActivity.this.getDecimalFormatter().to0Decimal(lenomedHistoryRecord.getDuration()));
            TextView alarm = holder.getAlarm();
            String alarm2 = lenomedHistoryRecord.getAlarm();
            alarm.setText(alarm2 != null ? alarm2 : "");
            holder.getDailyBasal().setText(LenomedHistoryActivity.this.getRh().gs(app.aaps.core.interfaces.R.string.format_insulin_units, Double.valueOf(lenomedHistoryRecord.getDailyBasal())));
            holder.getDailyBolus().setText(LenomedHistoryActivity.this.getRh().gs(app.aaps.core.interfaces.R.string.format_insulin_units, Double.valueOf(lenomedHistoryRecord.getDailyBolus())));
            holder.getDailyTotal().setText(LenomedHistoryActivity.this.getRh().gs(app.aaps.core.interfaces.R.string.format_insulin_units, Double.valueOf(lenomedHistoryRecord.getDailyBasal() + lenomedHistoryRecord.getDailyBolus())));
            byte b = LenomedHistoryActivity.this.showingType;
            if (b == 1) {
                holder.getTime().setVisibility(0);
                holder.getValue().setVisibility(0);
                holder.getStringValue().setVisibility(0);
                holder.getBolusType().setVisibility(0);
                holder.getDuration().setVisibility(8);
                holder.getDailyBasal().setVisibility(8);
                holder.getDailyBolus().setVisibility(8);
                holder.getDailyTotal().setVisibility(8);
                holder.getAlarm().setVisibility(8);
                TextView bolusType3 = holder.getBolusType();
                int i = Integer.parseInt(lenomedHistoryRecord.getBasalType());
                if (i == 1) {
                    str = "Basal A";
                } else if (i == 2) {
                    str = "Basal B";
                } else if (i == 3) {
                    str = "Temp Basal";
                }
                bolusType3.setText(str);
                return;
            }
            if (b == 2) {
                holder.getTime().setVisibility(0);
                holder.getValue().setVisibility(8);
                holder.getStringValue().setVisibility(8);
                holder.getBolusType().setVisibility(8);
                holder.getDuration().setVisibility(8);
                holder.getDailyBasal().setVisibility(0);
                holder.getDailyBolus().setVisibility(0);
                holder.getDailyTotal().setVisibility(0);
                holder.getAlarm().setVisibility(8);
                return;
            }
            if (b == 3) {
                holder.getTime().setVisibility(0);
                holder.getValue().setVisibility(8);
                holder.getStringValue().setVisibility(8);
                holder.getBolusType().setVisibility(8);
                holder.getDuration().setVisibility(8);
                holder.getDailyBasal().setVisibility(8);
                holder.getDailyBolus().setVisibility(8);
                holder.getDailyTotal().setVisibility(8);
                holder.getAlarm().setVisibility(0);
                TextView alarm3 = holder.getAlarm();
                int i2 = Integer.parseInt(lenomedHistoryRecord.getAlarm());
                if (i2 == 3) {
                    strGs2 = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_blocked);
                } else if (i2 == 4) {
                    strGs2 = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_drug_empty);
                } else if (i2 == 7) {
                    strGs2 = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_insulin_low);
                } else if (i2 == 8) {
                    strGs2 = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_checksum_error);
                } else {
                    switch (i2) {
                        case 13:
                            strGs2 = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_power_abnormal);
                            break;
                        case 14:
                            strGs2 = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_battery_low);
                            break;
                        case 15:
                            strGs2 = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_battery_empty);
                            break;
                        default:
                            strGs2 = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_unknown, lenomedHistoryRecord.getAlarm());
                            break;
                    }
                }
                alarm3.setText(strGs2);
                return;
            }
            if (b == 4) {
                holder.getTime().setVisibility(0);
                holder.getValue().setVisibility(0);
                holder.getStringValue().setVisibility(8);
                holder.getBolusType().setVisibility(8);
                holder.getDuration().setVisibility(8);
                holder.getDailyBasal().setVisibility(8);
                holder.getDailyBolus().setVisibility(8);
                holder.getDailyTotal().setVisibility(8);
                holder.getAlarm().setVisibility(8);
                return;
            }
            if (b == 5) {
                holder.getTime().setVisibility(0);
                holder.getValue().setVisibility(0);
                holder.getStringValue().setVisibility(0);
                holder.getBolusType().setVisibility(0);
                holder.getDuration().setVisibility(0);
                holder.getDailyBasal().setVisibility(8);
                holder.getDailyBolus().setVisibility(8);
                holder.getDailyTotal().setVisibility(8);
                holder.getAlarm().setVisibility(8);
                TextView bolusType4 = holder.getBolusType();
                int i3 = Integer.parseInt(lenomedHistoryRecord.getBolusType());
                if (i3 == 5) {
                    str = "方波";
                } else if (i3 == 6) {
                    str = "双波";
                } else if (i3 == 7) {
                    str = "曲线波";
                }
                bolusType4.setText(str);
                return;
            }
            if (b == 6) {
                holder.getTime().setVisibility(0);
                holder.getValue().setVisibility(0);
                holder.getStringValue().setVisibility(0);
                holder.getBolusType().setVisibility(0);
                holder.getDuration().setVisibility(8);
                holder.getDailyBasal().setVisibility(8);
                holder.getDailyBolus().setVisibility(8);
                holder.getDailyTotal().setVisibility(8);
                holder.getAlarm().setVisibility(8);
                TextView bolusType5 = holder.getBolusType();
                switch (Integer.parseInt(lenomedHistoryRecord.getRefillType())) {
                    case 10:
                        strGs = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_reset);
                        break;
                    case 11:
                        strGs = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_fixedprime);
                        break;
                    case 12:
                        strGs = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_manualprime);
                        break;
                    case 13:
                        strGs = LenomedHistoryActivity.this.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_factoryreset);
                        break;
                    default:
                        strGs = "未知 " + lenomedHistoryRecord.getRefillType();
                        break;
                }
                bolusType5.setText(strGs);
                return;
            }
            if (b == 7) {
                holder.getTime().setVisibility(0);
                holder.getValue().setVisibility(8);
                holder.getStringValue().setVisibility(0);
                holder.getBolusType().setVisibility(8);
                holder.getDuration().setVisibility(8);
                holder.getDailyBasal().setVisibility(8);
                holder.getDailyBolus().setVisibility(8);
                holder.getDailyTotal().setVisibility(8);
                holder.getAlarm().setVisibility(8);
                return;
            }
            if (b == 8) {
                holder.getTime().setVisibility(0);
                holder.getValue().setVisibility(0);
                holder.getStringValue().setVisibility(0);
                holder.getBolusType().setVisibility(8);
                holder.getDuration().setVisibility(0);
                holder.getDailyBasal().setVisibility(8);
                holder.getDailyBolus().setVisibility(8);
                holder.getDailyTotal().setVisibility(8);
                holder.getAlarm().setVisibility(8);
            }
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return this.historyList.size();
        }

    }

    /* JADX INFO: compiled from: LenomedHistoryActivity.kt */
    public final class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView alarm;
        private TextView bolusType;
        private TextView dailyBasal;
        private TextView dailyBolus;
        private TextView dailyTotal;
        private TextView duration;
        private TextView stringValue;
        private TextView time;
        private TextView value;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public HistoryViewHolder(View itemView) {
            super(itemView);
            Intrinsics.checkNotNullParameter(itemView, "itemView");
            View viewFindViewById = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_time);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById, "findViewById(...)");
            this.time = (TextView) viewFindViewById;
            View viewFindViewById2 = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_value);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById2, "findViewById(...)");
            this.value = (TextView) viewFindViewById2;
            View viewFindViewById3 = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_bolustype);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById3, "findViewById(...)");
            this.bolusType = (TextView) viewFindViewById3;
            View viewFindViewById4 = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_stringvalue);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById4, "findViewById(...)");
            this.stringValue = (TextView) viewFindViewById4;
            View viewFindViewById5 = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_duration);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById5, "findViewById(...)");
            this.duration = (TextView) viewFindViewById5;
            View viewFindViewById6 = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_dailybasal);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById6, "findViewById(...)");
            this.dailyBasal = (TextView) viewFindViewById6;
            View viewFindViewById7 = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_dailybolus);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById7, "findViewById(...)");
            this.dailyBolus = (TextView) viewFindViewById7;
            View viewFindViewById8 = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_dailytotal);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById8, "findViewById(...)");
            this.dailyTotal = (TextView) viewFindViewById8;
            View viewFindViewById9 = itemView.findViewById(app.aaps.pump.lenomed.R.id.embecta_history_alarm);
            Intrinsics.checkNotNullExpressionValue(viewFindViewById9, "findViewById(...)");
            this.alarm = (TextView) viewFindViewById9;
        }

            public final TextView getTime() {
                return this.time;
            }

            public final void setTime(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.time = textView;
            }

            public final TextView getValue() {
                return this.value;
            }

            public final void setValue(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.value = textView;
            }

            public final TextView getBolusType() {
                return this.bolusType;
            }

            public final void setBolusType(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.bolusType = textView;
            }

            public final TextView getStringValue() {
                return this.stringValue;
            }

            public final void setStringValue(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.stringValue = textView;
            }

            public final TextView getDuration() {
                return this.duration;
            }

            public final void setDuration(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.duration = textView;
            }

            public final TextView getDailyBasal() {
                return this.dailyBasal;
            }

            public final void setDailyBasal(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.dailyBasal = textView;
            }

            public final TextView getDailyBolus() {
                return this.dailyBolus;
            }

            public final void setDailyBolus(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.dailyBolus = textView;
            }

            public final TextView getDailyTotal() {
                return this.dailyTotal;
            }

            public final void setDailyTotal(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.dailyTotal = textView;
            }

            public final TextView getAlarm() {
                return this.alarm;
            }

            public final void setAlarm(TextView textView) {
                Intrinsics.checkNotNullParameter(textView, "<set-?>");
                this.alarm = textView;
            }
        }

    /* JADX INFO: Access modifiers changed from: private */
    public final void swapAdapter(byte type) {
        CompositeDisposable compositeDisposable = this.disposable;
        Disposable disposableSubscribe = getLenomedHistoryRecordDao().allFromByType(getDateUtil().now() - T.Companion.months(1L).msecs(), type).subscribeOn(getAapsSchedulers().getIo()).observeOn(getAapsSchedulers().getMain()).subscribe(new Consumer<List<LenomedHistoryRecord>>() { // from class: app.aaps.pump.lenomed.activities.LenomedHistoryActivity.swapAdapter.1
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(List<LenomedHistoryRecord> historyList) {
                Intrinsics.checkNotNullParameter(historyList, "historyList");
                LenomedHistoryActivityBinding lenomedHistoryActivityBinding = LenomedHistoryActivity.this.binding;
                if (lenomedHistoryActivityBinding == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("binding");
                    lenomedHistoryActivityBinding = null;
                }
                lenomedHistoryActivityBinding.recyclerview.swapAdapter(new RecyclerViewAdapter(historyList), false);
            }
        });
        Intrinsics.checkNotNullExpressionValue(disposableSubscribe, "subscribe(...)");
        DisposableKt.plusAssign(compositeDisposable, disposableSubscribe);
    }

    private final void clearCardView() {
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding = this.binding;
        if (lenomedHistoryActivityBinding == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding = null;
        }
        lenomedHistoryActivityBinding.recyclerview.swapAdapter(new RecyclerViewAdapter(new ArrayList()), false);
    }
}