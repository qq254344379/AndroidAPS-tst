package app.aaps.pump.lenomed.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.aaps.core.data.time.T;
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
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: LenomedHistoryActivity.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u0000\u0088\u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0005\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\u0018\u00002\u00020\u0001:\u0002RSB\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\b\u0010I\u001a\u00020JH\u0014J\b\u0010K\u001a\u00020JH\u0014J\u0012\u0010L\u001a\u00020J2\b\u0010M\u001a\u0004\u0018\u00010NH\u0014J\u0010\u0010O\u001a\u00020J2\u0006\u0010P\u001a\u00020CH\u0002J\b\u0010Q\u001a\u00020JH\u0002R\u001e\u0010\u0004\u001a\u00020\u00058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0006\u0010\u0007\"\u0004\b\b\u0010\tR\u001e\u0010\n\u001a\u00020\u000b8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000fR\u001e\u0010\u0010\u001a\u00020\u00118\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015R\u001e\u0010\u0016\u001a\u00020\u00178\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u001e\u0010\u001c\u001a\u00020\u001d8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\u001f\"\u0004\b \u0010!R\u001e\u0010\"\u001a\u00020#8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b$\u0010%\"\u0004\b&\u0010'R\u001e\u0010(\u001a\u00020)8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b*\u0010+\"\u0004\b,\u0010-R\u001e\u0010.\u001a\u00020/8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b0\u00101\"\u0004\b2\u00103R\u001e\u00104\u001a\u0002058\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b6\u00107\"\u0004\b8\u00109R\u001e\u0010:\u001a\u00020;8\u0006@\u0006X\u0087.¢\u0006\u000e\n\u0000\u001a\u0004\b<\u0010=\"\u0004\b>\u0010?R\u000e\u0010@\u001a\u00020AX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010B\u001a\u00020CX\u0082\u000e¢\u0006\u0002\n\u0000R\u0014\u0010D\u001a\b\u0012\u0004\u0012\u00020F0EX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010G\u001a\u00020HX\u0082.¢\u0006\u0002\n\u0000¨\u0006T"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedHistoryActivity;", "Lapp/aaps/core/ui/activities/TranslatedDaggerAppCompatActivity;", "<init>", "()V", "profileFunction", "Lapp/aaps/core/interfaces/profile/ProfileFunction;", "getProfileFunction", "()Lapp/aaps/core/interfaces/profile/ProfileFunction;", "setProfileFunction", "(Lapp/aaps/core/interfaces/profile/ProfileFunction;)V", "fabricPrivacy", "Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;", "getFabricPrivacy", "()Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;", "setFabricPrivacy", "(Lapp/aaps/core/interfaces/utils/fabric/FabricPrivacy;)V", "activePlugin", "Lapp/aaps/core/interfaces/plugin/ActivePlugin;", "getActivePlugin", "()Lapp/aaps/core/interfaces/plugin/ActivePlugin;", "setActivePlugin", "(Lapp/aaps/core/interfaces/plugin/ActivePlugin;)V", "commandQueue", "Lapp/aaps/core/interfaces/queue/CommandQueue;", "getCommandQueue", "()Lapp/aaps/core/interfaces/queue/CommandQueue;", "setCommandQueue", "(Lapp/aaps/core/interfaces/queue/CommandQueue;)V", "lenomedHistoryRecordDao", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;", "getLenomedHistoryRecordDao", "()Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;", "setLenomedHistoryRecordDao", "(Lapp/aaps/pump/lenomed/database/LenomedHistoryRecordDao;)V", "dateUtil", "Lapp/aaps/core/interfaces/utils/DateUtil;", "getDateUtil", "()Lapp/aaps/core/interfaces/utils/DateUtil;", "setDateUtil", "(Lapp/aaps/core/interfaces/utils/DateUtil;)V", "aapsSchedulers", "Lapp/aaps/core/interfaces/rx/AapsSchedulers;", "getAapsSchedulers", "()Lapp/aaps/core/interfaces/rx/AapsSchedulers;", "setAapsSchedulers", "(Lapp/aaps/core/interfaces/rx/AapsSchedulers;)V", "rxBus", "Lapp/aaps/core/interfaces/rx/bus/RxBus;", "getRxBus", "()Lapp/aaps/core/interfaces/rx/bus/RxBus;", "setRxBus", "(Lapp/aaps/core/interfaces/rx/bus/RxBus;)V", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", "getRh", "()Lapp/aaps/core/interfaces/resources/ResourceHelper;", "setRh", "(Lapp/aaps/core/interfaces/resources/ResourceHelper;)V", "decimalFormatter", "Lapp/aaps/core/interfaces/utils/DecimalFormatter;", "getDecimalFormatter", "()Lapp/aaps/core/interfaces/utils/DecimalFormatter;", "setDecimalFormatter", "(Lapp/aaps/core/interfaces/utils/DecimalFormatter;)V", "disposable", "Lio/reactivex/rxjava3/disposables/CompositeDisposable;", "showingType", "", "historyList", "", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecord;", "binding", "Lapp/aaps/pump/lenomed/databinding/LenomedHistoryActivityBinding;", "onResume", "", "onPause", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "swapAdapter", "type", "clearCardView", "TypeList", "RecyclerViewAdapter", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedHistoryActivity extends TranslatedDaggerAppCompatActivity {

    @Inject
    public AapsSchedulers aapsSchedulers;

    @Inject
    public ActivePlugin activePlugin;
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
        Intrinsics.throwUninitializedPropertyAccessException("lenomedHistoryRecordDao");
        return null;
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
    @Metadata(d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0005\n\u0000\n\u0002\u0010\u000e\n\u0002\b\f\u0018\u00002\u00020\u0001B\u0019\b\u0000\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005¢\u0006\u0004\b\u0006\u0010\u0007J\b\u0010\u0010\u001a\u00020\u0005H\u0016R\u001a\u0010\u0002\u001a\u00020\u0003X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001a\u0010\u0004\u001a\u00020\u0005X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\r\"\u0004\b\u000e\u0010\u000f¨\u0006\u0011"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedHistoryActivity$TypeList;", "", "type", "", "name", "", "<init>", "(BLjava/lang/String;)V", "getType", "()B", "setType", "(B)V", "getName", "()Ljava/lang/String;", "setName", "(Ljava/lang/String;)V", "toString", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
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
        Disposable disposableSubscribe = getRxBus().toObservable(EventPumpStatusChanged.class).observeOn(getAapsSchedulers().getMain()).subscribe(new Consumer() { // from class: app.aaps.pump.lenomed.activities.LenomedHistoryActivity.onResume.1
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(EventPumpStatusChanged it) {
                Intrinsics.checkNotNullParameter(it, "it");
                LenomedHistoryActivityBinding lenomedHistoryActivityBinding = LenomedHistoryActivity.this.binding;
                if (lenomedHistoryActivityBinding == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("binding");
                    lenomedHistoryActivityBinding = null;
                }
                lenomedHistoryActivityBinding.status.setText(it.getStatus(LenomedHistoryActivity.this));
            }
        }, new Consumer() { // from class: app.aaps.pump.lenomed.activities.LenomedHistoryActivity.onResume.2
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
        lenomedHistoryActivityBinding4.recyclerview.setAdapter(new RecyclerViewAdapter(this, this.historyList));
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
        lenomedHistoryActivityBinding6.typeList.setAdapter(new ArrayAdapter(lenomedHistoryActivity, R.layout.spinner_centered, arrayList));
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding7 = this.binding;
        if (lenomedHistoryActivityBinding7 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
            lenomedHistoryActivityBinding7 = null;
        }
        lenomedHistoryActivityBinding7.reload.setOnClickListener(new View.OnClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedHistoryActivity$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                LenomedHistoryActivity.onCreate$lambda$0(arrayList, this, view);
            }
        });
        LenomedHistoryActivityBinding lenomedHistoryActivityBinding8 = this.binding;
        if (lenomedHistoryActivityBinding8 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("binding");
        } else {
            lenomedHistoryActivityBinding = lenomedHistoryActivityBinding8;
        }
        lenomedHistoryActivityBinding.typeList.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: app.aaps.pump.lenomed.activities.LenomedHistoryActivity$$ExternalSyntheticLambda2
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                LenomedHistoryActivity.onCreate$lambda$1(arrayList, this, adapterView, view, i, j);
            }
        });
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
        lenomedHistoryActivity.runOnUiThread(new Runnable() { // from class: app.aaps.pump.lenomed.activities.LenomedHistoryActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                LenomedHistoryActivity.onCreate$lambda$0$1(this.f$0);
            }
        });
        lenomedHistoryActivity.clearCardView();
        lenomedHistoryActivity.getCommandQueue().loadHistory(typeList.getType(), new LenomedHistoryActivity$onCreate$1$2(lenomedHistoryActivity, typeList));
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
    @Metadata(d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u0086\u0004\u0018\u00002\u0010\u0012\f\u0012\n0\u0002R\u00060\u0000R\u00020\u00030\u0001:\u0001\u0013B\u0017\b\u0000\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005¢\u0006\u0004\b\u0007\u0010\bJ \u0010\t\u001a\n0\u0002R\u00060\u0000R\u00020\u00032\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH\u0016J \u0010\u000e\u001a\u00020\u000f2\u000e\u0010\u0010\u001a\n0\u0002R\u00060\u0000R\u00020\u00032\u0006\u0010\u0011\u001a\u00020\rH\u0016J\b\u0010\u0012\u001a\u00020\rH\u0016R\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u000e¢\u0006\u0002\n\u0000¨\u0006\u0014"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedHistoryActivity$RecyclerViewAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lapp/aaps/pump/lenomed/activities/LenomedHistoryActivity$RecyclerViewAdapter$HistoryViewHolder;", "Lapp/aaps/pump/lenomed/activities/LenomedHistoryActivity;", "historyList", "", "Lapp/aaps/pump/lenomed/database/LenomedHistoryRecord;", "<init>", "(Lapp/aaps/pump/lenomed/activities/LenomedHistoryActivity;Ljava/util/List;)V", "onCreateViewHolder", "viewGroup", "Landroid/view/ViewGroup;", "viewType", "", "onBindViewHolder", "", "holder", "position", "getItemCount", "HistoryViewHolder", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
    public final class RecyclerViewAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
        private List<LenomedHistoryRecord> historyList;
        final /* synthetic */ LenomedHistoryActivity this$0;

        public RecyclerViewAdapter(LenomedHistoryActivity lenomedHistoryActivity, List<LenomedHistoryRecord> historyList) {
            Intrinsics.checkNotNullParameter(historyList, "historyList");
            this.this$0 = lenomedHistoryActivity;
            this.historyList = historyList;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public HistoryViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            Intrinsics.checkNotNullParameter(viewGroup, "viewGroup");
            View viewInflate = LayoutInflater.from(viewGroup.getContext()).inflate(app.aaps.pump.lenomed.R.layout.lenomed_history_item, viewGroup, false);
            Intrinsics.checkNotNullExpressionValue(viewInflate, "inflate(...)");
            return new HistoryViewHolder(this, viewInflate);
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(HistoryViewHolder holder, int position) {
            String strGs;
            String strGs2;
            Intrinsics.checkNotNullParameter(holder, "holder");
            LenomedHistoryRecord lenomedHistoryRecord = this.historyList.get(position);
            holder.getTime().setText(this.this$0.getDateUtil().dateAndTimeAndSecondsString(lenomedHistoryRecord.getTimestamp()));
            holder.getValue().setText(this.this$0.getDecimalFormatter().to2Decimal(lenomedHistoryRecord.getValue()));
            TextView stringValue = holder.getStringValue();
            String stringValue2 = lenomedHistoryRecord.getStringValue();
            String str = "";
            stringValue.setText(stringValue2 != null ? stringValue2 : "");
            TextView bolusType = holder.getBolusType();
            String bolusType2 = lenomedHistoryRecord.getBolusType();
            bolusType.setText(bolusType2 != null ? bolusType2 : "");
            holder.getDuration().setText(this.this$0.getDecimalFormatter().to0Decimal(lenomedHistoryRecord.getDuration()));
            TextView alarm = holder.getAlarm();
            String alarm2 = lenomedHistoryRecord.getAlarm();
            alarm.setText(alarm2 != null ? alarm2 : "");
            holder.getDailyBasal().setText(this.this$0.getRh().gs(R.string.format_insulin_units, Double.valueOf(lenomedHistoryRecord.getDailyBasal())));
            holder.getDailyBolus().setText(this.this$0.getRh().gs(R.string.format_insulin_units, Double.valueOf(lenomedHistoryRecord.getDailyBolus())));
            holder.getDailyTotal().setText(this.this$0.getRh().gs(R.string.format_insulin_units, Double.valueOf(lenomedHistoryRecord.getDailyBasal() + lenomedHistoryRecord.getDailyBolus())));
            byte b = this.this$0.showingType;
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
                    strGs2 = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_blocked);
                } else if (i2 == 4) {
                    strGs2 = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_drug_empty);
                } else if (i2 == 7) {
                    strGs2 = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_insulin_low);
                } else if (i2 == 8) {
                    strGs2 = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_checksum_error);
                } else {
                    switch (i2) {
                        case 13:
                            strGs2 = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_power_abnormal);
                            break;
                        case 14:
                            strGs2 = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_battery_low);
                            break;
                        case 15:
                            strGs2 = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_battery_empty);
                            break;
                        default:
                            strGs2 = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_alarm_unknown, lenomedHistoryRecord.getAlarm());
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
                        strGs = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_reset);
                        break;
                    case 11:
                        strGs = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_fixedprime);
                        break;
                    case 12:
                        strGs = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_manualprime);
                        break;
                    case 13:
                        strGs = this.this$0.getRh().gs(app.aaps.pump.lenomed.R.string.lenomed_factoryreset);
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

        /* JADX INFO: compiled from: LenomedHistoryActivity.kt */
        @Metadata(d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u001d\b\u0086\u0004\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005R\u001a\u0010\u0006\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001a\u0010\f\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\t\"\u0004\b\u000e\u0010\u000bR\u001a\u0010\u000f\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0010\u0010\t\"\u0004\b\u0011\u0010\u000bR\u001a\u0010\u0012\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\t\"\u0004\b\u0014\u0010\u000bR\u001a\u0010\u0015\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0016\u0010\t\"\u0004\b\u0017\u0010\u000bR\u001a\u0010\u0018\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0019\u0010\t\"\u0004\b\u001a\u0010\u000bR\u001a\u0010\u001b\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u001c\u0010\t\"\u0004\b\u001d\u0010\u000bR\u001a\u0010\u001e\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u001f\u0010\t\"\u0004\b \u0010\u000bR\u001a\u0010!\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\"\u0010\t\"\u0004\b#\u0010\u000b¨\u0006$"}, d2 = {"Lapp/aaps/pump/lenomed/activities/LenomedHistoryActivity$RecyclerViewAdapter$HistoryViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "itemView", "Landroid/view/View;", "<init>", "(Lapp/aaps/pump/lenomed/activities/LenomedHistoryActivity$RecyclerViewAdapter;Landroid/view/View;)V", "time", "Landroid/widget/TextView;", "getTime", "()Landroid/widget/TextView;", "setTime", "(Landroid/widget/TextView;)V", "value", "getValue", "setValue", "bolusType", "getBolusType", "setBolusType", "stringValue", "getStringValue", "setStringValue", TypedValues.TransitionType.S_DURATION, "getDuration", "setDuration", "dailyBasal", "getDailyBasal", "setDailyBasal", "dailyBolus", "getDailyBolus", "setDailyBolus", "dailyTotal", "getDailyTotal", "setDailyTotal", "alarm", "getAlarm", "setAlarm", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
        public final class HistoryViewHolder extends RecyclerView.ViewHolder {
            private TextView alarm;
            private TextView bolusType;
            private TextView dailyBasal;
            private TextView dailyBolus;
            private TextView dailyTotal;
            private TextView duration;
            private TextView stringValue;
            final /* synthetic */ RecyclerViewAdapter this$0;
            private TextView time;
            private TextView value;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            public HistoryViewHolder(RecyclerViewAdapter recyclerViewAdapter, View itemView) {
                super(itemView);
                Intrinsics.checkNotNullParameter(itemView, "itemView");
                this.this$0 = recyclerViewAdapter;
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
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void swapAdapter(byte type) {
        CompositeDisposable compositeDisposable = this.disposable;
        Disposable disposableSubscribe = getLenomedHistoryRecordDao().allFromByType(getDateUtil().now() - T.INSTANCE.months(1L).msecs(), type).subscribeOn(getAapsSchedulers().getIo()).observeOn(getAapsSchedulers().getMain()).subscribe(new Consumer() { // from class: app.aaps.pump.lenomed.activities.LenomedHistoryActivity.swapAdapter.1
            @Override // io.reactivex.rxjava3.functions.Consumer
            public final void accept(List<LenomedHistoryRecord> historyList) {
                Intrinsics.checkNotNullParameter(historyList, "historyList");
                LenomedHistoryActivityBinding lenomedHistoryActivityBinding = LenomedHistoryActivity.this.binding;
                if (lenomedHistoryActivityBinding == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("binding");
                    lenomedHistoryActivityBinding = null;
                }
                lenomedHistoryActivityBinding.recyclerview.swapAdapter(new RecyclerViewAdapter(LenomedHistoryActivity.this, historyList), false);
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
        lenomedHistoryActivityBinding.recyclerview.swapAdapter(new RecyclerViewAdapter(this, new ArrayList()), false);
    }
}