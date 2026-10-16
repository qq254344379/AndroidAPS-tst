package app.aaps.pump.lenomed;

import app.aaps.core.data.time.T;
import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import app.aaps.core.interfaces.profile.Profile;
import app.aaps.core.interfaces.pump.PumpSync;
import app.aaps.core.interfaces.resources.ResourceHelper;
import app.aaps.core.interfaces.rx.events.EventOverviewBolusProgress;
import app.aaps.core.interfaces.utils.DateUtil;
import app.aaps.core.interfaces.utils.DecimalFormatter;
import com.google.android.libraries.places.api.model.PlaceTypes;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;
import kotlin.math.MathKt;
import org.apache.commons.lang3.StringUtils;

/* JADX INFO: compiled from: LenomedPump.kt */
/* JADX INFO: loaded from: classes4.dex */
@Singleton
@Metadata(d1 = {"\u0000\u0085\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\u0005\n\u0002\b\u0007\n\u0002\u0010\u0006\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b'\n\u0002\u0010\t\n\u0002\b\f\n\u0002\u0010\u0002\n\u0002\b2\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0018\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\u0011\n\u0002\b!\n\u0002\u0018\u0002\n\u0002\b)\n\u0002\u0018\u0002\n\u0003\bî\u0002\b\u0007\u0018\u0000 â\u00042\u00020\u0001:\u0002â\u0004B)\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t¢\u0006\u0004\b\n\u0010\u000bJ\u000e\u0010T\u001a\u00020U2\u0006\u0010V\u001a\u00020HJ\u0006\u0010W\u001a\u00020HJ\b\u0010\u0087\u0001\u001a\u00030\u0088\u0001J\u0013\u0010\u0089\u0001\u001a\u00020U2\n\u0010\u008a\u0001\u001a\u0005\u0018\u00010\u008b\u0001J\b\u0010¡\u0001\u001a\u00030\u0088\u0001J\u0013\u0010¢\u0001\u001a\u00020U2\n\u0010£\u0001\u001a\u0005\u0018\u00010¤\u0001J\u001e\u0010ó\u0001\u001a\t\u0012\u0004\u0012\u00020\u001a0©\u00012\b\u0010ô\u0001\u001a\u00030õ\u0001¢\u0006\u0003\u0010ö\u0001J\u0013\u0010÷\u0001\u001a\u00030\u0088\u00012\u0007\u0010ø\u0001\u001a\u00020\rH\u0007J\u0007\u0010ù\u0001\u001a\u00020UR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004¢\u0006\u0002\n\u0000R\u001a\u0010\f\u001a\u00020\rX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\f\u0010\u000e\"\u0004\b\u000f\u0010\u0010R\u001a\u0010\u0011\u001a\u00020\u0012X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0013\u0010\u0014\"\u0004\b\u0015\u0010\u0016R\u001a\u0010\u0017\u001a\u00020\rX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0017\u0010\u000e\"\u0004\b\u0018\u0010\u0010R\u001a\u0010\u0019\u001a\u00020\u001aX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u001b\u0010\u001c\"\u0004\b\u001d\u0010\u001eR\u001a\u0010\u001f\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b!\u0010\"\"\u0004\b#\u0010$R\u001a\u0010%\u001a\u00020\rX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b%\u0010\u000e\"\u0004\b&\u0010\u0010R\u001a\u0010'\u001a\u00020\rX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b'\u0010\u000e\"\u0004\b(\u0010\u0010R\u001a\u0010)\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b*\u0010\"\"\u0004\b+\u0010$R\u001a\u0010,\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b-\u0010\"\"\u0004\b.\u0010$R\u001a\u0010/\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b0\u0010\"\"\u0004\b1\u0010$R\u001a\u00102\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b3\u0010\"\"\u0004\b4\u0010$R\u001a\u00105\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b6\u0010\"\"\u0004\b7\u0010$R\u001a\u00108\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b9\u0010\"\"\u0004\b:\u0010$R\u001a\u0010;\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b<\u0010\"\"\u0004\b=\u0010$R\u001a\u0010>\u001a\u00020\u001aX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b?\u0010\u001c\"\u0004\b@\u0010\u001eR\u001a\u0010A\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bB\u0010\"\"\u0004\bC\u0010$R\u001a\u0010D\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bE\u0010\"\"\u0004\bF\u0010$R\u001a\u0010G\u001a\u00020HX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bI\u0010J\"\u0004\bK\u0010LR\u001a\u0010M\u001a\u00020HX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bN\u0010J\"\u0004\bO\u0010LR\u001a\u0010P\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bQ\u0010\"\"\u0004\bR\u0010$R\u000e\u0010S\u001a\u00020HX\u0082\u000e¢\u0006\u0002\n\u0000R\u001a\u0010X\u001a\u00020\rX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bY\u0010\u000e\"\u0004\bZ\u0010\u0010R\u001a\u0010[\u001a\u00020\u001aX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\\\u0010\u001c\"\u0004\b]\u0010\u001eR\u001a\u0010^\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b_\u0010\"\"\u0004\b`\u0010$R\u001a\u0010a\u001a\u00020\u001aX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bb\u0010\u001c\"\u0004\bc\u0010\u001eR\u001a\u0010d\u001a\u00020\u001aX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\be\u0010\u001c\"\u0004\bf\u0010\u001eR\u001a\u0010g\u001a\u00020\u001aX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bh\u0010\u001c\"\u0004\bi\u0010\u001eR\u001a\u0010j\u001a\u00020\rX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bk\u0010\u000e\"\u0004\bl\u0010\u0010R\u001a\u0010m\u001a\u00020HX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bn\u0010J\"\u0004\bo\u0010LR\u001a\u0010p\u001a\u00020\u001aX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bq\u0010\u001c\"\u0004\br\u0010\u001eR\u001a\u0010s\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bt\u0010\"\"\u0004\bu\u0010$R\u001a\u0010v\u001a\u00020HX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bw\u0010J\"\u0004\bx\u0010LR\u001a\u0010y\u001a\u00020HX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\bz\u0010J\"\u0004\b{\u0010LR\u001a\u0010|\u001a\u00020 X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b}\u0010\"\"\u0004\b~\u0010$R\u001c\u0010\u007f\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0080\u0001\u0010\u001c\"\u0005\b\u0081\u0001\u0010\u001eR(\u0010\u0083\u0001\u001a\u00020\r2\u0007\u0010\u0082\u0001\u001a\u00020\r8F@FX\u0086\u000e¢\u0006\u000e\u001a\u0005\b\u0083\u0001\u0010\u000e\"\u0005\b\u0084\u0001\u0010\u0010R\u0013\u0010\u0085\u0001\u001a\u00020 8F¢\u0006\u0007\u001a\u0005\b\u0086\u0001\u0010\"R\u001d\u0010\u008c\u0001\u001a\u00020HX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u008d\u0001\u0010J\"\u0005\b\u008e\u0001\u0010LR\u001d\u0010\u008f\u0001\u001a\u00020HX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0090\u0001\u0010J\"\u0005\b\u0091\u0001\u0010LR\u001d\u0010\u0092\u0001\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0093\u0001\u0010\u001c\"\u0005\b\u0094\u0001\u0010\u001eR(\u0010\u0095\u0001\u001a\u00020\r2\u0007\u0010\u0082\u0001\u001a\u00020\r8F@FX\u0086\u000e¢\u0006\u000e\u001a\u0005\b\u0095\u0001\u0010\u000e\"\u0005\b\u0096\u0001\u0010\u0010R\u0013\u0010\u0097\u0001\u001a\u00020 8F¢\u0006\u0007\u001a\u0005\b\u0098\u0001\u0010\"R\u0013\u0010\u0099\u0001\u001a\u00020 8F¢\u0006\u0007\u001a\u0005\b\u009a\u0001\u0010\"R\u0016\u0010\u009b\u0001\u001a\u00020 8BX\u0082\u0004¢\u0006\u0007\u001a\u0005\b\u009c\u0001\u0010\"R(\u0010\u009e\u0001\u001a\u00020\u001a2\u0007\u0010\u009d\u0001\u001a\u00020\u001a8F@FX\u0086\u000e¢\u0006\u000e\u001a\u0005\b\u009f\u0001\u0010\u001c\"\u0005\b \u0001\u0010\u001eR\u001d\u0010¥\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¦\u0001\u0010\"\"\u0005\b§\u0001\u0010$R2\u0010¨\u0001\u001a\u0012\u0012\u000b\u0012\t\u0012\u0004\u0012\u00020\u001a0©\u0001\u0018\u00010©\u0001X\u0086\u000e¢\u0006\u0015\n\u0003\u0010®\u0001\u001a\u0006\bª\u0001\u0010«\u0001\"\u0006\b¬\u0001\u0010\u00ad\u0001R\u001d\u0010¯\u0001\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b°\u0001\u0010\u001c\"\u0005\b±\u0001\u0010\u001eR\u001d\u0010²\u0001\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b³\u0001\u0010\u001c\"\u0005\b´\u0001\u0010\u001eR\u001d\u0010µ\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¶\u0001\u0010\"\"\u0005\b·\u0001\u0010$R\u001d\u0010¸\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¹\u0001\u0010\"\"\u0005\bº\u0001\u0010$R\u001d\u0010»\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¼\u0001\u0010\"\"\u0005\b½\u0001\u0010$R\u001d\u0010¾\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¿\u0001\u0010\"\"\u0005\bÀ\u0001\u0010$R\u001d\u0010Á\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÂ\u0001\u0010\"\"\u0005\bÃ\u0001\u0010$R\u001d\u0010Ä\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÅ\u0001\u0010\"\"\u0005\bÆ\u0001\u0010$R\u001d\u0010Ç\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÈ\u0001\u0010\"\"\u0005\bÉ\u0001\u0010$R\"\u0010Ê\u0001\u001a\u0005\u0018\u00010Ë\u0001X\u0086\u000e¢\u0006\u0012\n\u0000\u001a\u0006\bÌ\u0001\u0010Í\u0001\"\u0006\bÎ\u0001\u0010Ï\u0001R\u001d\u0010Ð\u0001\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÑ\u0001\u0010\u001c\"\u0005\bÒ\u0001\u0010\u001eR\u001d\u0010Ó\u0001\u001a\u00020HX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÔ\u0001\u0010J\"\u0005\bÕ\u0001\u0010LR\u001d\u0010Ö\u0001\u001a\u00020\rX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b×\u0001\u0010\u000e\"\u0005\bØ\u0001\u0010\u0010R\u001d\u0010Ù\u0001\u001a\u00020\rX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÚ\u0001\u0010\u000e\"\u0005\bÛ\u0001\u0010\u0010R\u001d\u0010Ü\u0001\u001a\u00020\rX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÝ\u0001\u0010\u000e\"\u0005\bÞ\u0001\u0010\u0010R\u001d\u0010ß\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bà\u0001\u0010\"\"\u0005\bá\u0001\u0010$R\u001d\u0010â\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bã\u0001\u0010\"\"\u0005\bä\u0001\u0010$R\u001d\u0010å\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bæ\u0001\u0010\"\"\u0005\bç\u0001\u0010$R\u001d\u0010è\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bé\u0001\u0010\"\"\u0005\bê\u0001\u0010$R\u001d\u0010ë\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bì\u0001\u0010\"\"\u0005\bí\u0001\u0010$R\u0015\u0010î\u0001\u001a\u00030\u0088\u00018F¢\u0006\b\u001a\u0006\bï\u0001\u0010ð\u0001R\u0015\u0010ñ\u0001\u001a\u00030\u0088\u00018F¢\u0006\b\u001a\u0006\bò\u0001\u0010ð\u0001R\u001d\u0010ú\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bû\u0001\u0010\"\"\u0005\bü\u0001\u0010$R\u001d\u0010ý\u0001\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bþ\u0001\u0010\"\"\u0005\bÿ\u0001\u0010$R\u001d\u0010\u0080\u0002\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0081\u0002\u0010\u001c\"\u0005\b\u0082\u0002\u0010\u001eR\u001d\u0010\u0083\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0084\u0002\u0010\"\"\u0005\b\u0085\u0002\u0010$R\u001d\u0010\u0086\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0087\u0002\u0010\"\"\u0005\b\u0088\u0002\u0010$R\u001d\u0010\u0089\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u008a\u0002\u0010\"\"\u0005\b\u008b\u0002\u0010$R\u001d\u0010\u008c\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u008d\u0002\u0010\"\"\u0005\b\u008e\u0002\u0010$R\u001d\u0010\u008f\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0090\u0002\u0010\"\"\u0005\b\u0091\u0002\u0010$R\u001d\u0010\u0092\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0093\u0002\u0010\"\"\u0005\b\u0094\u0002\u0010$R\u001d\u0010\u0095\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0096\u0002\u0010\"\"\u0005\b\u0097\u0002\u0010$R\u001d\u0010\u0098\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0099\u0002\u0010\"\"\u0005\b\u009a\u0002\u0010$R\u001d\u0010\u009b\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u009c\u0002\u0010\"\"\u0005\b\u009d\u0002\u0010$R\u001d\u0010\u009e\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u009f\u0002\u0010\"\"\u0005\b \u0002\u0010$R\u001d\u0010¡\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¢\u0002\u0010\"\"\u0005\b£\u0002\u0010$R\u001d\u0010¤\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¥\u0002\u0010\"\"\u0005\b¦\u0002\u0010$R\u001d\u0010§\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¨\u0002\u0010\"\"\u0005\b©\u0002\u0010$R\u001d\u0010ª\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b«\u0002\u0010\"\"\u0005\b¬\u0002\u0010$R\u001d\u0010\u00ad\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b®\u0002\u0010\"\"\u0005\b¯\u0002\u0010$R\u001d\u0010°\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b±\u0002\u0010\"\"\u0005\b²\u0002\u0010$R\u001d\u0010³\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b´\u0002\u0010\"\"\u0005\bµ\u0002\u0010$R\u001d\u0010¶\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b·\u0002\u0010\"\"\u0005\b¸\u0002\u0010$R\u001d\u0010¹\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bº\u0002\u0010\"\"\u0005\b»\u0002\u0010$R\u001d\u0010¼\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b½\u0002\u0010\"\"\u0005\b¾\u0002\u0010$R \u0010¿\u0002\u001a\u00030\u0088\u0001X\u0086\u000e¢\u0006\u0012\n\u0000\u001a\u0006\bÀ\u0002\u0010ð\u0001\"\u0006\bÁ\u0002\u0010Â\u0002R\u001d\u0010Ã\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÄ\u0002\u0010\"\"\u0005\bÅ\u0002\u0010$R\u001d\u0010Æ\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÇ\u0002\u0010\"\"\u0005\bÈ\u0002\u0010$R\u001d\u0010É\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÊ\u0002\u0010\"\"\u0005\bË\u0002\u0010$R\u001d\u0010Ì\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÍ\u0002\u0010\"\"\u0005\bÎ\u0002\u0010$R\u001d\u0010Ï\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÐ\u0002\u0010\"\"\u0005\bÑ\u0002\u0010$R\u001d\u0010Ò\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÓ\u0002\u0010\"\"\u0005\bÔ\u0002\u0010$R\u001d\u0010Õ\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÖ\u0002\u0010\"\"\u0005\b×\u0002\u0010$R\u001d\u0010Ø\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÙ\u0002\u0010\"\"\u0005\bÚ\u0002\u0010$R\u001d\u0010Û\u0002\u001a\u00020\rX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÛ\u0002\u0010\u000e\"\u0005\bÜ\u0002\u0010\u0010R\u001d\u0010Ý\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÞ\u0002\u0010\"\"\u0005\bß\u0002\u0010$R\u001d\u0010à\u0002\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bá\u0002\u0010\u001c\"\u0005\bâ\u0002\u0010\u001eR\u001d\u0010ã\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bä\u0002\u0010\"\"\u0005\bå\u0002\u0010$R\u001d\u0010æ\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bç\u0002\u0010\"\"\u0005\bè\u0002\u0010$R\u001d\u0010é\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bê\u0002\u0010\"\"\u0005\bë\u0002\u0010$R\u001d\u0010ì\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bí\u0002\u0010\"\"\u0005\bî\u0002\u0010$R\u001d\u0010ï\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bð\u0002\u0010\"\"\u0005\bñ\u0002\u0010$R\u001d\u0010ò\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bó\u0002\u0010\"\"\u0005\bô\u0002\u0010$R\u001d\u0010õ\u0002\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bö\u0002\u0010\u001c\"\u0005\b÷\u0002\u0010\u001eR\u001d\u0010ø\u0002\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bù\u0002\u0010\u001c\"\u0005\bú\u0002\u0010\u001eR\u001d\u0010û\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bü\u0002\u0010\"\"\u0005\bý\u0002\u0010$R\u001d\u0010þ\u0002\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÿ\u0002\u0010\"\"\u0005\b\u0080\u0003\u0010$R\u001d\u0010\u0081\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0082\u0003\u0010\"\"\u0005\b\u0083\u0003\u0010$R\u001d\u0010\u0084\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0085\u0003\u0010\u001c\"\u0005\b\u0086\u0003\u0010\u001eR\u001d\u0010\u0087\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0088\u0003\u0010\u001c\"\u0005\b\u0089\u0003\u0010\u001eR\u001d\u0010\u008a\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u008b\u0003\u0010\"\"\u0005\b\u008c\u0003\u0010$R\u001d\u0010\u008d\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u008e\u0003\u0010\"\"\u0005\b\u008f\u0003\u0010$R\u001d\u0010\u0090\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0091\u0003\u0010\u001c\"\u0005\b\u0092\u0003\u0010\u001eR\u001d\u0010\u0093\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0094\u0003\u0010\u001c\"\u0005\b\u0095\u0003\u0010\u001eR\u001d\u0010\u0096\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0097\u0003\u0010\"\"\u0005\b\u0098\u0003\u0010$R\u001d\u0010\u0099\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u009a\u0003\u0010\"\"\u0005\b\u009b\u0003\u0010$R\u001d\u0010\u009c\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u009d\u0003\u0010\"\"\u0005\b\u009e\u0003\u0010$R\u001d\u0010\u009f\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b \u0003\u0010\"\"\u0005\b¡\u0003\u0010$R\u001d\u0010¢\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b£\u0003\u0010\u001c\"\u0005\b¤\u0003\u0010\u001eR\u001d\u0010¥\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¦\u0003\u0010\u001c\"\u0005\b§\u0003\u0010\u001eR\u001d\u0010¨\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b©\u0003\u0010\"\"\u0005\bª\u0003\u0010$R\u001d\u0010«\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¬\u0003\u0010\u001c\"\u0005\b\u00ad\u0003\u0010\u001eR\u001d\u0010®\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¯\u0003\u0010\u001c\"\u0005\b°\u0003\u0010\u001eR\u001d\u0010±\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b²\u0003\u0010\"\"\u0005\b³\u0003\u0010$R\u001d\u0010´\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bµ\u0003\u0010\"\"\u0005\b¶\u0003\u0010$R\u001d\u0010·\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¸\u0003\u0010\u001c\"\u0005\b¹\u0003\u0010\u001eR\u001d\u0010º\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b»\u0003\u0010\u001c\"\u0005\b¼\u0003\u0010\u001eR\u001d\u0010½\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¾\u0003\u0010\"\"\u0005\b¿\u0003\u0010$R\u001d\u0010À\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÁ\u0003\u0010\"\"\u0005\bÂ\u0003\u0010$R\u001d\u0010Ã\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÄ\u0003\u0010\u001c\"\u0005\bÅ\u0003\u0010\u001eR\u001d\u0010Æ\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÇ\u0003\u0010\"\"\u0005\bÈ\u0003\u0010$R\u001d\u0010É\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÊ\u0003\u0010\"\"\u0005\bË\u0003\u0010$R\u001d\u0010Ì\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÍ\u0003\u0010\u001c\"\u0005\bÎ\u0003\u0010\u001eR\u001d\u0010Ï\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÐ\u0003\u0010\u001c\"\u0005\bÑ\u0003\u0010\u001eR\u001d\u0010Ò\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÓ\u0003\u0010\u001c\"\u0005\bÔ\u0003\u0010\u001eR\u001d\u0010Õ\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÖ\u0003\u0010\u001c\"\u0005\b×\u0003\u0010\u001eR\u001d\u0010Ø\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÙ\u0003\u0010\"\"\u0005\bÚ\u0003\u0010$R\u001d\u0010Û\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÜ\u0003\u0010\u001c\"\u0005\bÝ\u0003\u0010\u001eR\u001d\u0010Þ\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bß\u0003\u0010\"\"\u0005\bà\u0003\u0010$R\u001d\u0010á\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bâ\u0003\u0010\u001c\"\u0005\bã\u0003\u0010\u001eR\u001d\u0010ä\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bå\u0003\u0010\"\"\u0005\bæ\u0003\u0010$R\u001d\u0010ç\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bè\u0003\u0010\u001c\"\u0005\bé\u0003\u0010\u001eR\u001d\u0010ê\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bë\u0003\u0010\"\"\u0005\bì\u0003\u0010$R\u001d\u0010í\u0003\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bî\u0003\u0010\"\"\u0005\bï\u0003\u0010$R\u001d\u0010ð\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bñ\u0003\u0010\u001c\"\u0005\bò\u0003\u0010\u001eR\u001d\u0010ó\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bô\u0003\u0010\u001c\"\u0005\bõ\u0003\u0010\u001eR\u001d\u0010ö\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b÷\u0003\u0010\u001c\"\u0005\bø\u0003\u0010\u001eR\u001d\u0010ù\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bú\u0003\u0010\u001c\"\u0005\bû\u0003\u0010\u001eR\u001d\u0010ü\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bý\u0003\u0010\u001c\"\u0005\bþ\u0003\u0010\u001eR\u001d\u0010ÿ\u0003\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0080\u0004\u0010\u001c\"\u0005\b\u0081\u0004\u0010\u001eR\u001d\u0010\u0082\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0083\u0004\u0010\u001c\"\u0005\b\u0084\u0004\u0010\u001eR\u001d\u0010\u0085\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0086\u0004\u0010\u001c\"\u0005\b\u0087\u0004\u0010\u001eR\u001d\u0010\u0088\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0089\u0004\u0010\u001c\"\u0005\b\u008a\u0004\u0010\u001eR\u001d\u0010\u008b\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u008c\u0004\u0010\u001c\"\u0005\b\u008d\u0004\u0010\u001eR\u001d\u0010\u008e\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u008f\u0004\u0010\u001c\"\u0005\b\u0090\u0004\u0010\u001eR\u001d\u0010\u0091\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0092\u0004\u0010\u001c\"\u0005\b\u0093\u0004\u0010\u001eR\u001d\u0010\u0094\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0095\u0004\u0010\u001c\"\u0005\b\u0096\u0004\u0010\u001eR\u001d\u0010\u0097\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u0098\u0004\u0010\u001c\"\u0005\b\u0099\u0004\u0010\u001eR\u001d\u0010\u009a\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u009b\u0004\u0010\u001c\"\u0005\b\u009c\u0004\u0010\u001eR\u001d\u0010\u009d\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u009e\u0004\u0010\u001c\"\u0005\b\u009f\u0004\u0010\u001eR\u001d\u0010 \u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¡\u0004\u0010\u001c\"\u0005\b¢\u0004\u0010\u001eR\u001d\u0010£\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¤\u0004\u0010\u001c\"\u0005\b¥\u0004\u0010\u001eR\u001d\u0010¦\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b§\u0004\u0010\u001c\"\u0005\b¨\u0004\u0010\u001eR\u001d\u0010©\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bª\u0004\u0010\u001c\"\u0005\b«\u0004\u0010\u001eR\u001d\u0010¬\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b\u00ad\u0004\u0010\u001c\"\u0005\b®\u0004\u0010\u001eR\u001d\u0010¯\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b°\u0004\u0010\u001c\"\u0005\b±\u0004\u0010\u001eR\u001d\u0010²\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b³\u0004\u0010\u001c\"\u0005\b´\u0004\u0010\u001eR\u001d\u0010µ\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¶\u0004\u0010\u001c\"\u0005\b·\u0004\u0010\u001eR\u001d\u0010¸\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¹\u0004\u0010\u001c\"\u0005\bº\u0004\u0010\u001eR\u001d\u0010»\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¼\u0004\u0010\u001c\"\u0005\b½\u0004\u0010\u001eR\u001d\u0010¾\u0004\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b¿\u0004\u0010\"\"\u0005\bÀ\u0004\u0010$R\u001d\u0010Á\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÂ\u0004\u0010\u001c\"\u0005\bÃ\u0004\u0010\u001eR\u001d\u0010Ä\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÅ\u0004\u0010\u001c\"\u0005\bÆ\u0004\u0010\u001eR\u001d\u0010Ç\u0004\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÈ\u0004\u0010\"\"\u0005\bÉ\u0004\u0010$R\u001d\u0010Ê\u0004\u001a\u00020 X\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bË\u0004\u0010\"\"\u0005\bÌ\u0004\u0010$R\u001d\u0010Í\u0004\u001a\u00020\rX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÎ\u0004\u0010\u000e\"\u0005\bÏ\u0004\u0010\u0010R\u001d\u0010Ð\u0004\u001a\u00020\rX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÑ\u0004\u0010\u000e\"\u0005\bÒ\u0004\u0010\u0010R\u001d\u0010Ó\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÔ\u0004\u0010\u001c\"\u0005\bÕ\u0004\u0010\u001eR\u001d\u0010Ö\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\b×\u0004\u0010\u001c\"\u0005\bØ\u0004\u0010\u001eR\u001d\u0010Ù\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÚ\u0004\u0010\u001c\"\u0005\bÛ\u0004\u0010\u001eR\u001d\u0010Ü\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bÝ\u0004\u0010\u001c\"\u0005\bÞ\u0004\u0010\u001eR\u001d\u0010ß\u0004\u001a\u00020\u001aX\u0086\u000e¢\u0006\u0010\n\u0000\u001a\u0005\bà\u0004\u0010\u001c\"\u0005\bá\u0004\u0010\u001e¨\u0006ã\u0004"}, d2 = {"Lapp/aaps/pump/lenomed/LenomedPump;", "", "aapsLogger", "Lapp/aaps/core/interfaces/logging/AAPSLogger;", "dateUtil", "Lapp/aaps/core/interfaces/utils/DateUtil;", "rh", "Lapp/aaps/core/interfaces/resources/ResourceHelper;", "decimalFormatter", "Lapp/aaps/core/interfaces/utils/DecimalFormatter;", "<init>", "(Lapp/aaps/core/interfaces/logging/AAPSLogger;Lapp/aaps/core/interfaces/utils/DateUtil;Lapp/aaps/core/interfaces/resources/ResourceHelper;Lapp/aaps/core/interfaces/utils/DecimalFormatter;)V", "isPumpLogUploadFailed", "", "()Z", "setPumpLogUploadFailed", "(Z)V", "bolusConfirmMessage", "", "getBolusConfirmMessage", "()B", "setBolusConfirmMessage", "(B)V", "isReadyToBolus", "setReadyToBolus", "maxBolusePerDay", "", "getMaxBolusePerDay", "()D", "setMaxBolusePerDay", "(D)V", "pumpIncarnationNum", "", "getPumpIncarnationNum", "()I", "setPumpIncarnationNum", "(I)V", "isPumpVersionGe2_63", "setPumpVersionGe2_63", "isPumpVersionGe3_53", "setPumpVersionGe3_53", "insulinWarningGrade", "getInsulinWarningGrade", "setInsulinWarningGrade", "insulinWarningProcess", "getInsulinWarningProcess", "setInsulinWarningProcess", "insulinWarningRemain", "getInsulinWarningRemain", "setInsulinWarningRemain", "batteryWaningGrade", "getBatteryWaningGrade", "setBatteryWaningGrade", "batteryWaningProcess", "getBatteryWaningProcess", "setBatteryWaningProcess", "batteryWaningRemain", "getBatteryWaningRemain", "setBatteryWaningRemain", "injectionBlockType", "getInjectionBlockType", "setInjectionBlockType", "injectionBlockRemainAmount", "getInjectionBlockRemainAmount", "setInjectionBlockRemainAmount", "injectionBlockProcess", "getInjectionBlockProcess", "setInjectionBlockProcess", "injectionBlockGrade", "getInjectionBlockGrade", "setInjectionBlockGrade", "lastConnection", "", "getLastConnection", "()J", "setLastConnection", "(J)V", "lastSettingsRead", "getLastSettingsRead", "setLastSettingsRead", "mealLimitTime", "getMealLimitTime", "setMealLimitTime", "pumpTime", "setPumpTime", "", "value", "getPumpTime", "pumpSuspended", "getPumpSuspended", "setPumpSuspended", "dailyTotalUnits", "getDailyTotalUnits", "setDailyTotalUnits", "maxDailyTotalUnits", "getMaxDailyTotalUnits", "setMaxDailyTotalUnits", "bolusStep", "getBolusStep", "setBolusStep", "basalStep", "getBasalStep", "setBasalStep", "iob", "getIob", "setIob", "bolusBlocked", "getBolusBlocked", "setBolusBlocked", "lastBolusTime", "getLastBolusTime", "setLastBolusTime", "lastBolusAmount", "getLastBolusAmount", "setLastBolusAmount", "bolusStatus", "getBolusStatus", "setBolusStatus", "tempBasalStart", "getTempBasalStart", "setTempBasalStart", "tempBasalDuration", "getTempBasalDuration", "setTempBasalDuration", "tempBasalPercent", "getTempBasalPercent", "setTempBasalPercent", "tempBasalAbsoluteRate", "getTempBasalAbsoluteRate", "setTempBasalAbsoluteRate", "isRunning", "isTempBasalInProgress", "setTempBasalInProgress", "tempBasalRemainingMin", "getTempBasalRemainingMin", "temporaryBasalToString", "", "fromTemporaryBasal", "tbr", "Lapp/aaps/core/interfaces/pump/PumpSync$PumpState$TemporaryBasal;", "extendedBolusStart", "getExtendedBolusStart", "setExtendedBolusStart", "extendedBolusDuration", "getExtendedBolusDuration", "setExtendedBolusDuration", "extendedBolusAmount", "getExtendedBolusAmount", "setExtendedBolusAmount", "isExtendedInProgress", "setExtendedInProgress", "extendedBolusPassedMinutes", "getExtendedBolusPassedMinutes", "extendedBolusRemainingMinutes", "getExtendedBolusRemainingMinutes", "extendedBolusDurationInMinutes", "getExtendedBolusDurationInMinutes", "rate", "extendedBolusAbsoluteRate", "getExtendedBolusAbsoluteRate", "setExtendedBolusAbsoluteRate", "extendedBolusToString", "fromExtendedBolus", "eb", "Lapp/aaps/core/interfaces/pump/PumpSync$PumpState$ExtendedBolus;", "activeProfile", "getActiveProfile", "setActiveProfile", "pumpProfiles", "", "getPumpProfiles", "()[[Ljava/lang/Double;", "setPumpProfiles", "([[Ljava/lang/Double;)V", "[[Ljava/lang/Double;", "maxBolus", "getMaxBolus", "setMaxBolus", "maxBasal", "getMaxBasal", "setMaxBasal", "setUserOptionType", "getSetUserOptionType", "setSetUserOptionType", "beepAndAlarm", "getBeepAndAlarm", "setBeepAndAlarm", "alarmIntensity", "getAlarmIntensity", "setAlarmIntensity", "lcdOnTimeSec", "getLcdOnTimeSec", "setLcdOnTimeSec", "selectedLanguage", "getSelectedLanguage", "setSelectedLanguage", "bolusSpeed", "getBolusSpeed", "setBolusSpeed", "resultErrorCode", "getResultErrorCode", "setResultErrorCode", "bolusingTreatment", "Lapp/aaps/core/interfaces/rx/events/EventOverviewBolusProgress$Treatment;", "getBolusingTreatment", "()Lapp/aaps/core/interfaces/rx/events/EventOverviewBolusProgress$Treatment;", "setBolusingTreatment", "(Lapp/aaps/core/interfaces/rx/events/EventOverviewBolusProgress$Treatment;)V", "bolusAmountToBeDelivered", "getBolusAmountToBeDelivered", "setBolusAmountToBeDelivered", "bolusProgressLastTimeStamp", "getBolusProgressLastTimeStamp", "setBolusProgressLastTimeStamp", "bolusStopped", "getBolusStopped", "setBolusStopped", "bolusStopForced", "getBolusStopForced", "setBolusStopForced", "bolusDone", "getBolusDone", "setBolusDone", "blockStatus", "getBlockStatus", "setBlockStatus", "infusionSwitchStatus", "getInfusionSwitchStatus", "setInfusionSwitchStatus", "wifiStatus", "getWifiStatus", "setWifiStatus", "bluetoothStatus", "getBluetoothStatus", "setBluetoothStatus", "alarmStatus", "getAlarmStatus", "setAlarmStatus", "pumpUid", "getPumpUid", "()Ljava/lang/String;", "pumpVersion", "getPumpVersion", "buildEmbectaProfileRecord", "nsProfile", "Lapp/aaps/core/interfaces/profile/Profile;", "(Lapp/aaps/core/interfaces/profile/Profile;)[Ljava/lang/Double;", "getTempBasalString", "isChinese", "reset", "totalPages", "getTotalPages", "setTotalPages", "result", "getResult", "setResult", "systemRemainInsulin", "getSystemRemainInsulin", "setSystemRemainInsulin", "systemRemainBattery", "getSystemRemainBattery", "setSystemRemainBattery", "systemBasePattern", "getSystemBasePattern", "setSystemBasePattern", "systemTbStatus", "getSystemTbStatus", "setSystemTbStatus", "systemInjectionMealStatus", "getSystemInjectionMealStatus", "setSystemInjectionMealStatus", "systemInjectionSnackStatus", "getSystemInjectionSnackStatus", "setSystemInjectionSnackStatus", "systemInjectionSquareStatue", "getSystemInjectionSquareStatue", "setSystemInjectionSquareStatue", "systemInjectionDualStatus", "getSystemInjectionDualStatus", "setSystemInjectionDualStatus", "basePauseStatus", "getBasePauseStatus", "setBasePauseStatus", "year", "getYear", "setYear", "month", "getMonth", "setMonth", "day", "getDay", "setDay", "hour", "getHour", "setHour", "minute", "getMinute", "setMinute", "second", "getSecond", "setSecond", PlaceTypes.COUNTRY, "getCountry", "setCountry", "productType", "getProductType", "setProductType", "makeYear", "getMakeYear", "setMakeYear", "makeMonth", "getMakeMonth", "setMakeMonth", "makeDay", "getMakeDay", "setMakeDay", "lotNo", "getLotNo", "setLotNo", "serialNo", "getSerialNo", "setSerialNo", "(Ljava/lang/String;)V", "majorVersion", "getMajorVersion", "setMajorVersion", "minorVersion", "getMinorVersion", "setMinorVersion", "patchVersion", "getPatchVersion", "setPatchVersion", "buildVersion", "getBuildVersion", "setBuildVersion", "pumpLastLogNum", "getPumpLastLogNum", "setPumpLastLogNum", "pumpWrappingCount", "getPumpWrappingCount", "setPumpWrappingCount", "apslastLogNum", "getApslastLogNum", "setApslastLogNum", "apsWrappingCount", "getApsWrappingCount", "setApsWrappingCount", "isPlatformUploadStarted", "setPlatformUploadStarted", "speed", "getSpeed", "setSpeed", "maxBasalPerHours", "getMaxBasalPerHours", "setMaxBasalPerHours", "tbStatus", "getTbStatus", "setTbStatus", "tbTime", "getTbTime", "setTbTime", "tbInjectRateRatio", "getTbInjectRateRatio", "setTbInjectRateRatio", "tbElapsedTime", "getTbElapsedTime", "setTbElapsedTime", "baseStatus", "getBaseStatus", "setBaseStatus", "baseHour", "getBaseHour", "setBaseHour", "baseAmount", "getBaseAmount", "setBaseAmount", "baseInjAmount", "getBaseInjAmount", "setBaseInjAmount", "mealKind", "getMealKind", "setMealKind", "mealStartTime", "getMealStartTime", "setMealStartTime", "mealStatus", "getMealStatus", "setMealStatus", "mealAmount", "getMealAmount", "setMealAmount", "mealInjAmount", "getMealInjAmount", "setMealInjAmount", "mealSpeed", "getMealSpeed", "setMealSpeed", "snackStatus", "getSnackStatus", "setSnackStatus", "snackAmount", "getSnackAmount", "setSnackAmount", "snackInjAmount", "getSnackInjAmount", "setSnackInjAmount", "snackSpeed", "getSnackSpeed", "setSnackSpeed", "squareStatus", "getSquareStatus", "setSquareStatus", "squareTime", "getSquareTime", "setSquareTime", "squareInjTime", "getSquareInjTime", "setSquareInjTime", "squareAmount", "getSquareAmount", "setSquareAmount", "squareInjAmount", "getSquareInjAmount", "setSquareInjAmount", "dualStatus", "getDualStatus", "setDualStatus", "dualAmount", "getDualAmount", "setDualAmount", "dualInjAmount", "getDualInjAmount", "setDualInjAmount", "dualSquareTime", "getDualSquareTime", "setDualSquareTime", "dualInjSquareTime", "getDualInjSquareTime", "setDualInjSquareTime", "dualSquareAmount", "getDualSquareAmount", "setDualSquareAmount", "dualInjSquareAmount", "getDualInjSquareAmount", "setDualInjSquareAmount", "recentKind1", "getRecentKind1", "setRecentKind1", "recentTime1", "getRecentTime1", "setRecentTime1", "recentAmount1", "getRecentAmount1", "setRecentAmount1", "recentKind2", "getRecentKind2", "setRecentKind2", "recentTime2", "getRecentTime2", "setRecentTime2", "recentAmount2", "getRecentAmount2", "setRecentAmount2", "todayBaseAmount", "getTodayBaseAmount", "setTodayBaseAmount", "todayMealAmount", "getTodayMealAmount", "setTodayMealAmount", "todaySnackAmount", "getTodaySnackAmount", "setTodaySnackAmount", "morningHour", "getMorningHour", "setMorningHour", "morningAmount", "getMorningAmount", "setMorningAmount", "lunchHour", "getLunchHour", "setLunchHour", "lunchAmount", "getLunchAmount", "setLunchAmount", "dinnerHour", "getDinnerHour", "setDinnerHour", "dinnerAmount", "getDinnerAmount", "setDinnerAmount", "currentBasePattern", "getCurrentBasePattern", "setCurrentBasePattern", "currentBaseHour", "getCurrentBaseHour", "setCurrentBaseHour", "currentBaseTbBeforeAmount", "getCurrentBaseTbBeforeAmount", "setCurrentBaseTbBeforeAmount", "currentBaseTbAfterAmount", "getCurrentBaseTbAfterAmount", "setCurrentBaseTbAfterAmount", "baseAmount1", "getBaseAmount1", "setBaseAmount1", "baseAmount2", "getBaseAmount2", "setBaseAmount2", "baseAmount3", "getBaseAmount3", "setBaseAmount3", "baseAmount4", "getBaseAmount4", "setBaseAmount4", "baseAmount5", "getBaseAmount5", "setBaseAmount5", "baseAmount6", "getBaseAmount6", "setBaseAmount6", "baseAmount7", "getBaseAmount7", "setBaseAmount7", "baseAmount8", "getBaseAmount8", "setBaseAmount8", "baseAmount9", "getBaseAmount9", "setBaseAmount9", "baseAmount10", "getBaseAmount10", "setBaseAmount10", "baseAmount11", "getBaseAmount11", "setBaseAmount11", "baseAmount12", "getBaseAmount12", "setBaseAmount12", "baseAmount13", "getBaseAmount13", "setBaseAmount13", "baseAmount14", "getBaseAmount14", "setBaseAmount14", "baseAmount15", "getBaseAmount15", "setBaseAmount15", "baseAmount16", "getBaseAmount16", "setBaseAmount16", "baseAmount17", "getBaseAmount17", "setBaseAmount17", "baseAmount18", "getBaseAmount18", "setBaseAmount18", "baseAmount19", "getBaseAmount19", "setBaseAmount19", "baseAmount20", "getBaseAmount20", "setBaseAmount20", "baseAmount21", "getBaseAmount21", "setBaseAmount21", "baseAmount22", "getBaseAmount22", "setBaseAmount22", "baseAmount23", "getBaseAmount23", "setBaseAmount23", "baseAmount24", "getBaseAmount24", "setBaseAmount24", "otpNumber", "getOtpNumber", "setOtpNumber", "bolusingSetAmount", "getBolusingSetAmount", "setBolusingSetAmount", "bolusingInjAmount", "getBolusingInjAmount", "setBolusingInjAmount", "bolusingSpeed", "getBolusingSpeed", "setBolusingSpeed", "bolusingInjProgress", "getBolusingInjProgress", "setBolusingInjProgress", "infusionEnabled", "getInfusionEnabled", "setInfusionEnabled", "bluetoothEnabled", "getBluetoothEnabled", "setBluetoothEnabled", "maxBolusSingle", "getMaxBolusSingle", "setMaxBolusSingle", "maxBasalSingle", "getMaxBasalSingle", "setMaxBasalSingle", "bolusDailyTotalThreshold", "getBolusDailyTotalThreshold", "setBolusDailyTotalThreshold", "basalDailyTotalThreshold", "getBasalDailyTotalThreshold", "setBasalDailyTotalThreshold", "fixedExhaustThreshold", "getFixedExhaustThreshold", "setFixedExhaustThreshold", "Companion", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedPump {
    public static final int ALARM = 0;
    public static final int BASAL_DAILY_TOTAL = 7;
    public static final int BLUETOOTH_SWITCH = 10;
    public static final int BOLUS_SPEED = 3;
    public static final int FIXED_EXHAUST = 8;
    public static final int INFUSION_SWITCH = 9;
    public static final int LANG = 2;
    public static final int LCD = 1;
    public static final int MAX_BASAL = 5;
    public static final int MAX_BOLUS = 4;
    public static final int MAX_BOLUS_DAILY = 6;
    public static final int THRESHOLD = 11;
    private final AAPSLogger aapsLogger;
    private int activeProfile;
    private int alarmIntensity;
    private int alarmStatus;
    private int apsWrappingCount;
    private int apslastLogNum;
    private double basalDailyTotalThreshold;
    private double basalStep;
    private double baseAmount;
    private double baseAmount1;
    private double baseAmount10;
    private double baseAmount11;
    private double baseAmount12;
    private double baseAmount13;
    private double baseAmount14;
    private double baseAmount15;
    private double baseAmount16;
    private double baseAmount17;
    private double baseAmount18;
    private double baseAmount19;
    private double baseAmount2;
    private double baseAmount20;
    private double baseAmount21;
    private double baseAmount22;
    private double baseAmount23;
    private double baseAmount24;
    private double baseAmount3;
    private double baseAmount4;
    private double baseAmount5;
    private double baseAmount6;
    private double baseAmount7;
    private double baseAmount8;
    private double baseAmount9;
    private int baseHour;
    private double baseInjAmount;
    private int basePauseStatus;
    private int baseStatus;
    private int batteryWaningGrade;
    private int batteryWaningProcess;
    private int batteryWaningRemain;
    private int beepAndAlarm;
    private int blockStatus;
    private boolean bluetoothEnabled;
    private int bluetoothStatus;
    private double bolusAmountToBeDelivered;
    private boolean bolusBlocked;
    private byte bolusConfirmMessage;
    private double bolusDailyTotalThreshold;
    private boolean bolusDone;
    private long bolusProgressLastTimeStamp;
    private int bolusSpeed;
    private int bolusStatus;
    private double bolusStep;
    private boolean bolusStopForced;
    private boolean bolusStopped;
    private double bolusingInjAmount;
    private int bolusingInjProgress;
    private double bolusingSetAmount;
    private int bolusingSpeed;
    private EventOverviewBolusProgress.Treatment bolusingTreatment;
    private int buildVersion;
    private int country;
    private int currentBaseHour;
    private int currentBasePattern;
    private double currentBaseTbAfterAmount;
    private double currentBaseTbBeforeAmount;
    private double dailyTotalUnits;
    private final DateUtil dateUtil;
    private int day;
    private final DecimalFormatter decimalFormatter;
    private double dinnerAmount;
    private int dinnerHour;
    private double dualAmount;
    private double dualInjAmount;
    private double dualInjSquareAmount;
    private int dualInjSquareTime;
    private double dualSquareAmount;
    private int dualSquareTime;
    private int dualStatus;
    private double extendedBolusAmount;
    private long extendedBolusDuration;
    private long extendedBolusStart;
    private double fixedExhaustThreshold;
    private int hour;
    private boolean infusionEnabled;
    private int infusionSwitchStatus;
    private int injectionBlockGrade;
    private int injectionBlockProcess;
    private double injectionBlockRemainAmount;
    private int injectionBlockType;
    private int insulinWarningGrade;
    private int insulinWarningProcess;
    private int insulinWarningRemain;
    private double iob;
    private boolean isPlatformUploadStarted;
    private boolean isPumpLogUploadFailed;
    private boolean isPumpVersionGe2_63;
    private boolean isPumpVersionGe3_53;
    private boolean isReadyToBolus;
    private double lastBolusAmount;
    private long lastBolusTime;
    private long lastConnection;
    private long lastSettingsRead;
    private int lcdOnTimeSec;
    private int lotNo;
    private double lunchAmount;
    private int lunchHour;
    private int majorVersion;
    private int makeDay;
    private int makeMonth;
    private int makeYear;
    private double maxBasal;
    private double maxBasalPerHours;
    private double maxBasalSingle;
    private double maxBolus;
    private double maxBolusSingle;
    private double maxBolusePerDay;
    private int maxDailyTotalUnits;
    private double mealAmount;
    private double mealInjAmount;
    private int mealKind;
    private int mealLimitTime;
    private int mealSpeed;
    private int mealStartTime;
    private int mealStatus;
    private int minorVersion;
    private int minute;
    private int month;
    private double morningAmount;
    private int morningHour;
    private int otpNumber;
    private int patchVersion;
    private int productType;
    private int pumpIncarnationNum;
    private int pumpLastLogNum;
    private Double[][] pumpProfiles;
    private boolean pumpSuspended;
    private long pumpTime;
    private int pumpWrappingCount;
    private double recentAmount1;
    private double recentAmount2;
    private int recentKind1;
    private int recentKind2;
    private int recentTime1;
    private int recentTime2;
    private int result;
    private int resultErrorCode;
    private final ResourceHelper rh;
    private int second;
    private int selectedLanguage;
    private String serialNo;
    private int setUserOptionType;
    private double snackAmount;
    private double snackInjAmount;
    private int snackSpeed;
    private int snackStatus;
    private int speed;
    private double squareAmount;
    private double squareInjAmount;
    private int squareInjTime;
    private int squareStatus;
    private int squareTime;
    private int systemBasePattern;
    private int systemInjectionDualStatus;
    private int systemInjectionMealStatus;
    private int systemInjectionSnackStatus;
    private int systemInjectionSquareStatue;
    private int systemRemainBattery;
    private double systemRemainInsulin;
    private int systemTbStatus;
    private int tbElapsedTime;
    private int tbInjectRateRatio;
    private int tbStatus;
    private int tbTime;
    private double tempBasalAbsoluteRate;
    private long tempBasalDuration;
    private int tempBasalPercent;
    private long tempBasalStart;
    private double todayBaseAmount;
    private double todayMealAmount;
    private double todaySnackAmount;
    private int totalPages;
    private int wifiStatus;
    private int year;

    @Inject
    public LenomedPump(AAPSLogger aapsLogger, DateUtil dateUtil, ResourceHelper rh, DecimalFormatter decimalFormatter) {
        Intrinsics.checkNotNullParameter(aapsLogger, "aapsLogger");
        Intrinsics.checkNotNullParameter(dateUtil, "dateUtil");
        Intrinsics.checkNotNullParameter(rh, "rh");
        Intrinsics.checkNotNullParameter(decimalFormatter, "decimalFormatter");
        this.aapsLogger = aapsLogger;
        this.dateUtil = dateUtil;
        this.rh = rh;
        this.decimalFormatter = decimalFormatter;
        this.pumpIncarnationNum = 65536;
        this.bolusStep = 0.01d;
        this.basalStep = 0.01d;
        this.maxBolus = 10.0d;
        this.maxBasal = 10.0d;
        this.totalPages = 1;
        this.serialNo = "";
        this.maxBolusSingle = 10.0d;
        this.maxBasalSingle = 10.0d;
    }

    /* JADX INFO: renamed from: isPumpLogUploadFailed, reason: from getter */
    public final boolean getIsPumpLogUploadFailed() {
        return this.isPumpLogUploadFailed;
    }

    public final void setPumpLogUploadFailed(boolean z) {
        this.isPumpLogUploadFailed = z;
    }

    public final byte getBolusConfirmMessage() {
        return this.bolusConfirmMessage;
    }

    public final void setBolusConfirmMessage(byte b) {
        this.bolusConfirmMessage = b;
    }

    /* JADX INFO: renamed from: isReadyToBolus, reason: from getter */
    public final boolean getIsReadyToBolus() {
        return this.isReadyToBolus;
    }

    public final void setReadyToBolus(boolean z) {
        this.isReadyToBolus = z;
    }

    public final double getMaxBolusePerDay() {
        return this.maxBolusePerDay;
    }

    public final void setMaxBolusePerDay(double d) {
        this.maxBolusePerDay = d;
    }

    public final int getPumpIncarnationNum() {
        return this.pumpIncarnationNum;
    }

    public final void setPumpIncarnationNum(int i) {
        this.pumpIncarnationNum = i;
    }

    /* JADX INFO: renamed from: isPumpVersionGe2_63, reason: from getter */
    public final boolean getIsPumpVersionGe2_63() {
        return this.isPumpVersionGe2_63;
    }

    public final void setPumpVersionGe2_63(boolean z) {
        this.isPumpVersionGe2_63 = z;
    }

    /* JADX INFO: renamed from: isPumpVersionGe3_53, reason: from getter */
    public final boolean getIsPumpVersionGe3_53() {
        return this.isPumpVersionGe3_53;
    }

    public final void setPumpVersionGe3_53(boolean z) {
        this.isPumpVersionGe3_53 = z;
    }

    public final int getInsulinWarningGrade() {
        return this.insulinWarningGrade;
    }

    public final void setInsulinWarningGrade(int i) {
        this.insulinWarningGrade = i;
    }

    public final int getInsulinWarningProcess() {
        return this.insulinWarningProcess;
    }

    public final void setInsulinWarningProcess(int i) {
        this.insulinWarningProcess = i;
    }

    public final int getInsulinWarningRemain() {
        return this.insulinWarningRemain;
    }

    public final void setInsulinWarningRemain(int i) {
        this.insulinWarningRemain = i;
    }

    public final int getBatteryWaningGrade() {
        return this.batteryWaningGrade;
    }

    public final void setBatteryWaningGrade(int i) {
        this.batteryWaningGrade = i;
    }

    public final int getBatteryWaningProcess() {
        return this.batteryWaningProcess;
    }

    public final void setBatteryWaningProcess(int i) {
        this.batteryWaningProcess = i;
    }

    public final int getBatteryWaningRemain() {
        return this.batteryWaningRemain;
    }

    public final void setBatteryWaningRemain(int i) {
        this.batteryWaningRemain = i;
    }

    public final int getInjectionBlockType() {
        return this.injectionBlockType;
    }

    public final void setInjectionBlockType(int i) {
        this.injectionBlockType = i;
    }

    public final double getInjectionBlockRemainAmount() {
        return this.injectionBlockRemainAmount;
    }

    public final void setInjectionBlockRemainAmount(double d) {
        this.injectionBlockRemainAmount = d;
    }

    public final int getInjectionBlockProcess() {
        return this.injectionBlockProcess;
    }

    public final void setInjectionBlockProcess(int i) {
        this.injectionBlockProcess = i;
    }

    public final int getInjectionBlockGrade() {
        return this.injectionBlockGrade;
    }

    public final void setInjectionBlockGrade(int i) {
        this.injectionBlockGrade = i;
    }

    public final long getLastConnection() {
        return this.lastConnection;
    }

    public final void setLastConnection(long j) {
        this.lastConnection = j;
    }

    public final long getLastSettingsRead() {
        return this.lastSettingsRead;
    }

    public final void setLastSettingsRead(long j) {
        this.lastSettingsRead = j;
    }

    public final int getMealLimitTime() {
        return this.mealLimitTime;
    }

    public final void setMealLimitTime(int i) {
        this.mealLimitTime = i;
    }

    public final void setPumpTime(long value) {
        this.pumpTime = value;
    }

    public final long getPumpTime() {
        return this.pumpTime;
    }

    public final boolean getPumpSuspended() {
        return this.pumpSuspended;
    }

    public final void setPumpSuspended(boolean z) {
        this.pumpSuspended = z;
    }

    public final double getDailyTotalUnits() {
        return this.dailyTotalUnits;
    }

    public final void setDailyTotalUnits(double d) {
        this.dailyTotalUnits = d;
    }

    public final int getMaxDailyTotalUnits() {
        return this.maxDailyTotalUnits;
    }

    public final void setMaxDailyTotalUnits(int i) {
        this.maxDailyTotalUnits = i;
    }

    public final double getBolusStep() {
        return this.bolusStep;
    }

    public final void setBolusStep(double d) {
        this.bolusStep = d;
    }

    public final double getBasalStep() {
        return this.basalStep;
    }

    public final void setBasalStep(double d) {
        this.basalStep = d;
    }

    public final double getIob() {
        return this.iob;
    }

    public final void setIob(double d) {
        this.iob = d;
    }

    public final boolean getBolusBlocked() {
        return this.bolusBlocked;
    }

    public final void setBolusBlocked(boolean z) {
        this.bolusBlocked = z;
    }

    public final long getLastBolusTime() {
        return this.lastBolusTime;
    }

    public final void setLastBolusTime(long j) {
        this.lastBolusTime = j;
    }

    public final double getLastBolusAmount() {
        return this.lastBolusAmount;
    }

    public final void setLastBolusAmount(double d) {
        this.lastBolusAmount = d;
    }

    public final int getBolusStatus() {
        return this.bolusStatus;
    }

    public final void setBolusStatus(int i) {
        this.bolusStatus = i;
    }

    public final long getTempBasalStart() {
        return this.tempBasalStart;
    }

    public final void setTempBasalStart(long j) {
        this.tempBasalStart = j;
    }

    public final long getTempBasalDuration() {
        return this.tempBasalDuration;
    }

    public final void setTempBasalDuration(long j) {
        this.tempBasalDuration = j;
    }

    public final int getTempBasalPercent() {
        return this.tempBasalPercent;
    }

    public final void setTempBasalPercent(int i) {
        this.tempBasalPercent = i;
    }

    public final double getTempBasalAbsoluteRate() {
        return this.tempBasalAbsoluteRate;
    }

    public final void setTempBasalAbsoluteRate(double d) {
        this.tempBasalAbsoluteRate = d;
    }

    public final boolean isTempBasalInProgress() {
        long j = this.tempBasalStart;
        if (j == 0) {
            return false;
        }
        long j2 = this.tempBasalDuration + j;
        long jNow = this.dateUtil.now();
        return j <= jNow && jNow <= j2;
    }

    public final void setTempBasalInProgress(boolean z) {
        if (z) {
            return;
        }
        this.tempBasalStart = 0L;
        this.tempBasalDuration = 0L;
        this.tempBasalAbsoluteRate = 0.0d;
    }

    public final int getTempBasalRemainingMin() {
        return Math.max((int) T.INSTANCE.msecs((this.tempBasalStart + this.tempBasalDuration) - this.dateUtil.now()).mins(), 0);
    }

    public final String temporaryBasalToString() {
        if (!isTempBasalInProgress()) {
            return "";
        }
        int iRoundToInt = MathKt.roundToInt(((Math.min(this.dateUtil.now(), this.tempBasalStart + this.tempBasalDuration) - this.tempBasalStart) / 60.0d) / ((double) 1000));
        return this.decimalFormatter.to2Decimal(this.tempBasalAbsoluteRate) + "U/h @" + this.dateUtil.timeString(this.tempBasalStart) + StringUtils.SPACE + iRoundToInt + "/" + T.INSTANCE.msecs(this.tempBasalDuration).mins() + "'";
    }

    public final void fromTemporaryBasal(PumpSync.PumpState.TemporaryBasal tbr) {
        if (tbr == null) {
            this.tempBasalStart = 0L;
            this.tempBasalDuration = 0L;
            this.tempBasalAbsoluteRate = 0.0d;
        } else {
            this.tempBasalStart = tbr.getTimestamp();
            this.tempBasalDuration = tbr.getDuration();
            this.tempBasalAbsoluteRate = tbr.getRate();
        }
    }

    public final long getExtendedBolusStart() {
        return this.extendedBolusStart;
    }

    public final void setExtendedBolusStart(long j) {
        this.extendedBolusStart = j;
    }

    public final long getExtendedBolusDuration() {
        return this.extendedBolusDuration;
    }

    public final void setExtendedBolusDuration(long j) {
        this.extendedBolusDuration = j;
    }

    public final double getExtendedBolusAmount() {
        return this.extendedBolusAmount;
    }

    public final void setExtendedBolusAmount(double d) {
        this.extendedBolusAmount = d;
    }

    public final boolean isExtendedInProgress() {
        long j = this.extendedBolusStart;
        if (j == 0) {
            return false;
        }
        long j2 = this.extendedBolusDuration + j;
        long jNow = this.dateUtil.now();
        return j <= jNow && jNow <= j2;
    }

    public final void setExtendedInProgress(boolean z) {
        if (z) {
            throw new IllegalArgumentException("Use to cancel EB only");
        }
        this.extendedBolusStart = 0L;
        this.extendedBolusDuration = 0L;
        this.extendedBolusAmount = 0.0d;
    }

    public final int getExtendedBolusPassedMinutes() {
        return (int) T.INSTANCE.msecs(Math.max(0L, this.dateUtil.now() - this.extendedBolusStart)).mins();
    }

    public final int getExtendedBolusRemainingMinutes() {
        return Math.max((int) T.INSTANCE.msecs((this.extendedBolusStart + this.extendedBolusDuration) - this.dateUtil.now()).mins(), 0);
    }

    private final int getExtendedBolusDurationInMinutes() {
        return (int) T.INSTANCE.msecs(this.extendedBolusDuration).mins();
    }

    public final double getExtendedBolusAbsoluteRate() {
        return (this.extendedBolusAmount * T.INSTANCE.hours(1L).msecs()) / this.extendedBolusDuration;
    }

    public final void setExtendedBolusAbsoluteRate(double d) {
        this.extendedBolusAmount = (d * this.extendedBolusDuration) / T.INSTANCE.hours(1L).msecs();
    }

    public final String extendedBolusToString() {
        if (!isExtendedInProgress()) {
            return "";
        }
        return "E " + this.decimalFormatter.to2Decimal(getExtendedBolusAbsoluteRate()) + "U/h @" + this.dateUtil.timeString(this.extendedBolusStart) + StringUtils.SPACE + getExtendedBolusPassedMinutes() + "/" + getExtendedBolusDurationInMinutes() + "'";
    }

    public final void fromExtendedBolus(PumpSync.PumpState.ExtendedBolus eb) {
        if (eb == null) {
            this.extendedBolusStart = 0L;
            this.extendedBolusDuration = 0L;
            this.extendedBolusAmount = 0.0d;
        } else {
            this.extendedBolusStart = eb.getTimestamp();
            this.extendedBolusDuration = eb.getDuration();
            this.extendedBolusAmount = eb.getAmount();
        }
    }

    public final int getActiveProfile() {
        return this.activeProfile;
    }

    public final void setActiveProfile(int i) {
        this.activeProfile = i;
    }

    public final Double[][] getPumpProfiles() {
        return this.pumpProfiles;
    }

    public final void setPumpProfiles(Double[][] dArr) {
        this.pumpProfiles = dArr;
    }

    public final double getMaxBolus() {
        return this.maxBolus;
    }

    public final void setMaxBolus(double d) {
        this.maxBolus = d;
    }

    public final double getMaxBasal() {
        return this.maxBasal;
    }

    public final void setMaxBasal(double d) {
        this.maxBasal = d;
    }

    public final int getSetUserOptionType() {
        return this.setUserOptionType;
    }

    public final void setSetUserOptionType(int i) {
        this.setUserOptionType = i;
    }

    public final int getBeepAndAlarm() {
        return this.beepAndAlarm;
    }

    public final void setBeepAndAlarm(int i) {
        this.beepAndAlarm = i;
    }

    public final int getAlarmIntensity() {
        return this.alarmIntensity;
    }

    public final void setAlarmIntensity(int i) {
        this.alarmIntensity = i;
    }

    public final int getLcdOnTimeSec() {
        return this.lcdOnTimeSec;
    }

    public final void setLcdOnTimeSec(int i) {
        this.lcdOnTimeSec = i;
    }

    public final int getSelectedLanguage() {
        return this.selectedLanguage;
    }

    public final void setSelectedLanguage(int i) {
        this.selectedLanguage = i;
    }

    public final int getBolusSpeed() {
        return this.bolusSpeed;
    }

    public final void setBolusSpeed(int i) {
        this.bolusSpeed = i;
    }

    public final int getResultErrorCode() {
        return this.resultErrorCode;
    }

    public final void setResultErrorCode(int i) {
        this.resultErrorCode = i;
    }

    public final EventOverviewBolusProgress.Treatment getBolusingTreatment() {
        return this.bolusingTreatment;
    }

    public final void setBolusingTreatment(EventOverviewBolusProgress.Treatment treatment) {
        this.bolusingTreatment = treatment;
    }

    public final double getBolusAmountToBeDelivered() {
        return this.bolusAmountToBeDelivered;
    }

    public final void setBolusAmountToBeDelivered(double d) {
        this.bolusAmountToBeDelivered = d;
    }

    public final long getBolusProgressLastTimeStamp() {
        return this.bolusProgressLastTimeStamp;
    }

    public final void setBolusProgressLastTimeStamp(long j) {
        this.bolusProgressLastTimeStamp = j;
    }

    public final boolean getBolusStopped() {
        return this.bolusStopped;
    }

    public final void setBolusStopped(boolean z) {
        this.bolusStopped = z;
    }

    public final boolean getBolusStopForced() {
        return this.bolusStopForced;
    }

    public final void setBolusStopForced(boolean z) {
        this.bolusStopForced = z;
    }

    public final boolean getBolusDone() {
        return this.bolusDone;
    }

    public final void setBolusDone(boolean z) {
        this.bolusDone = z;
    }

    public final int getBlockStatus() {
        return this.blockStatus;
    }

    public final void setBlockStatus(int i) {
        this.blockStatus = i;
    }

    public final int getInfusionSwitchStatus() {
        return this.infusionSwitchStatus;
    }

    public final void setInfusionSwitchStatus(int i) {
        this.infusionSwitchStatus = i;
    }

    public final int getWifiStatus() {
        return this.wifiStatus;
    }

    public final void setWifiStatus(int i) {
        this.wifiStatus = i;
    }

    public final int getBluetoothStatus() {
        return this.bluetoothStatus;
    }

    public final void setBluetoothStatus(int i) {
        this.bluetoothStatus = i;
    }

    public final int getAlarmStatus() {
        return this.alarmStatus;
    }

    public final void setAlarmStatus(int i) {
        this.alarmStatus = i;
    }

    /* JADX INFO: renamed from: getPumpUid, reason: from getter */
    public final String getSerialNo() {
        return this.serialNo;
    }

    public final String getPumpVersion() {
        return this.majorVersion + "." + this.minorVersion + "." + this.patchVersion + "." + this.buildVersion;
    }

    public final Double[] buildEmbectaProfileRecord(Profile nsProfile) {
        Intrinsics.checkNotNullParameter(nsProfile, "nsProfile");
        Double[] dArr = new Double[24];
        for (int i = 0; i < 24; i++) {
            dArr[i] = Double.valueOf(0.0d);
        }
        for (int i2 = 0; i2 < 24; i2++) {
            double basalTimeFromMidnight = nsProfile.getBasalTimeFromMidnight(i2 * 3600);
            StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
            String str = String.format("%.2f", Arrays.copyOf(new Object[]{Double.valueOf(basalTimeFromMidnight)}, 1));
            Intrinsics.checkNotNullExpressionValue(str, "format(...)");
            double d = Double.parseDouble(str) + 1.0E-5d;
            this.aapsLogger.debug(LTag.PUMP, "NS basal value for " + i2 + ":00 is " + d);
            dArr[i2] = Double.valueOf(d);
        }
        return dArr;
    }

    public final String getTempBasalString(boolean isChinese) {
        String str;
        if (!isTempBasalInProgress()) {
            if (isChinese) {
                return this.rh.gs(R.string.lenomed_temp_basal_none);
            }
            return this.rh.gs(R.string.lenomed_temp_basal_none);
        }
        if (this.tbStatus == 1) {
            str = ((int) (this.tempBasalAbsoluteRate * ((double) 100))) + "%";
        } else {
            str = this.decimalFormatter.to2Decimal(this.tempBasalAbsoluteRate) + "U/h";
        }
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String str2 = String.format("%02d:%02d", Arrays.copyOf(new Object[]{Integer.valueOf(this.tbTime / 60), Integer.valueOf(this.tbTime % 60)}, 2));
        Intrinsics.checkNotNullExpressionValue(str2, "format(...)");
        int i = (int) ((this.tempBasalDuration / ((long) 1000)) / ((long) 60));
        if (isChinese) {
            return this.rh.gs(R.string.lenomed_temp_basal, str, str2, Integer.valueOf(i));
        }
        return this.rh.gs(R.string.lenomed_temp_basal, str, str2, Integer.valueOf(i));
    }

    public final void reset() {
        this.aapsLogger.debug(LTag.PUMP, "Lenomed Pump reset");
        this.lastConnection = 0L;
        this.lastSettingsRead = 0L;
    }

    public final int getTotalPages() {
        return this.totalPages;
    }

    public final void setTotalPages(int i) {
        this.totalPages = i;
    }

    public final int getResult() {
        return this.result;
    }

    public final void setResult(int i) {
        this.result = i;
    }

    public final double getSystemRemainInsulin() {
        return this.systemRemainInsulin;
    }

    public final void setSystemRemainInsulin(double d) {
        this.systemRemainInsulin = d;
    }

    public final int getSystemRemainBattery() {
        return this.systemRemainBattery;
    }

    public final void setSystemRemainBattery(int i) {
        this.systemRemainBattery = i;
    }

    public final int getSystemBasePattern() {
        return this.systemBasePattern;
    }

    public final void setSystemBasePattern(int i) {
        this.systemBasePattern = i;
    }

    public final int getSystemTbStatus() {
        return this.systemTbStatus;
    }

    public final void setSystemTbStatus(int i) {
        this.systemTbStatus = i;
    }

    public final int getSystemInjectionMealStatus() {
        return this.systemInjectionMealStatus;
    }

    public final void setSystemInjectionMealStatus(int i) {
        this.systemInjectionMealStatus = i;
    }

    public final int getSystemInjectionSnackStatus() {
        return this.systemInjectionSnackStatus;
    }

    public final void setSystemInjectionSnackStatus(int i) {
        this.systemInjectionSnackStatus = i;
    }

    public final int getSystemInjectionSquareStatue() {
        return this.systemInjectionSquareStatue;
    }

    public final void setSystemInjectionSquareStatue(int i) {
        this.systemInjectionSquareStatue = i;
    }

    public final int getSystemInjectionDualStatus() {
        return this.systemInjectionDualStatus;
    }

    public final void setSystemInjectionDualStatus(int i) {
        this.systemInjectionDualStatus = i;
    }

    public final int getBasePauseStatus() {
        return this.basePauseStatus;
    }

    public final void setBasePauseStatus(int i) {
        this.basePauseStatus = i;
    }

    public final int getYear() {
        return this.year;
    }

    public final void setYear(int i) {
        this.year = i;
    }

    public final int getMonth() {
        return this.month;
    }

    public final void setMonth(int i) {
        this.month = i;
    }

    public final int getDay() {
        return this.day;
    }

    public final void setDay(int i) {
        this.day = i;
    }

    public final int getHour() {
        return this.hour;
    }

    public final void setHour(int i) {
        this.hour = i;
    }

    public final int getMinute() {
        return this.minute;
    }

    public final void setMinute(int i) {
        this.minute = i;
    }

    public final int getSecond() {
        return this.second;
    }

    public final void setSecond(int i) {
        this.second = i;
    }

    public final int getCountry() {
        return this.country;
    }

    public final void setCountry(int i) {
        this.country = i;
    }

    public final int getProductType() {
        return this.productType;
    }

    public final void setProductType(int i) {
        this.productType = i;
    }

    public final int getMakeYear() {
        return this.makeYear;
    }

    public final void setMakeYear(int i) {
        this.makeYear = i;
    }

    public final int getMakeMonth() {
        return this.makeMonth;
    }

    public final void setMakeMonth(int i) {
        this.makeMonth = i;
    }

    public final int getMakeDay() {
        return this.makeDay;
    }

    public final void setMakeDay(int i) {
        this.makeDay = i;
    }

    public final int getLotNo() {
        return this.lotNo;
    }

    public final void setLotNo(int i) {
        this.lotNo = i;
    }

    public final String getSerialNo() {
        return this.serialNo;
    }

    public final void setSerialNo(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.serialNo = str;
    }

    public final int getMajorVersion() {
        return this.majorVersion;
    }

    public final void setMajorVersion(int i) {
        this.majorVersion = i;
    }

    public final int getMinorVersion() {
        return this.minorVersion;
    }

    public final void setMinorVersion(int i) {
        this.minorVersion = i;
    }

    public final int getPatchVersion() {
        return this.patchVersion;
    }

    public final void setPatchVersion(int i) {
        this.patchVersion = i;
    }

    public final int getBuildVersion() {
        return this.buildVersion;
    }

    public final void setBuildVersion(int i) {
        this.buildVersion = i;
    }

    public final int getPumpLastLogNum() {
        return this.pumpLastLogNum;
    }

    public final void setPumpLastLogNum(int i) {
        this.pumpLastLogNum = i;
    }

    public final int getPumpWrappingCount() {
        return this.pumpWrappingCount;
    }

    public final void setPumpWrappingCount(int i) {
        this.pumpWrappingCount = i;
    }

    public final int getApslastLogNum() {
        return this.apslastLogNum;
    }

    public final void setApslastLogNum(int i) {
        this.apslastLogNum = i;
    }

    public final int getApsWrappingCount() {
        return this.apsWrappingCount;
    }

    public final void setApsWrappingCount(int i) {
        this.apsWrappingCount = i;
    }

    /* JADX INFO: renamed from: isPlatformUploadStarted, reason: from getter */
    public final boolean getIsPlatformUploadStarted() {
        return this.isPlatformUploadStarted;
    }

    public final void setPlatformUploadStarted(boolean z) {
        this.isPlatformUploadStarted = z;
    }

    public final int getSpeed() {
        return this.speed;
    }

    public final void setSpeed(int i) {
        this.speed = i;
    }

    public final double getMaxBasalPerHours() {
        return this.maxBasalPerHours;
    }

    public final void setMaxBasalPerHours(double d) {
        this.maxBasalPerHours = d;
    }

    public final int getTbStatus() {
        return this.tbStatus;
    }

    public final void setTbStatus(int i) {
        this.tbStatus = i;
    }

    public final int getTbTime() {
        return this.tbTime;
    }

    public final void setTbTime(int i) {
        this.tbTime = i;
    }

    public final int getTbInjectRateRatio() {
        return this.tbInjectRateRatio;
    }

    public final void setTbInjectRateRatio(int i) {
        this.tbInjectRateRatio = i;
    }

    public final int getTbElapsedTime() {
        return this.tbElapsedTime;
    }

    public final void setTbElapsedTime(int i) {
        this.tbElapsedTime = i;
    }

    public final int getBaseStatus() {
        return this.baseStatus;
    }

    public final void setBaseStatus(int i) {
        this.baseStatus = i;
    }

    public final int getBaseHour() {
        return this.baseHour;
    }

    public final void setBaseHour(int i) {
        this.baseHour = i;
    }

    public final double getBaseAmount() {
        return this.baseAmount;
    }

    public final void setBaseAmount(double d) {
        this.baseAmount = d;
    }

    public final double getBaseInjAmount() {
        return this.baseInjAmount;
    }

    public final void setBaseInjAmount(double d) {
        this.baseInjAmount = d;
    }

    public final int getMealKind() {
        return this.mealKind;
    }

    public final void setMealKind(int i) {
        this.mealKind = i;
    }

    public final int getMealStartTime() {
        return this.mealStartTime;
    }

    public final void setMealStartTime(int i) {
        this.mealStartTime = i;
    }

    public final int getMealStatus() {
        return this.mealStatus;
    }

    public final void setMealStatus(int i) {
        this.mealStatus = i;
    }

    public final double getMealAmount() {
        return this.mealAmount;
    }

    public final void setMealAmount(double d) {
        this.mealAmount = d;
    }

    public final double getMealInjAmount() {
        return this.mealInjAmount;
    }

    public final void setMealInjAmount(double d) {
        this.mealInjAmount = d;
    }

    public final int getMealSpeed() {
        return this.mealSpeed;
    }

    public final void setMealSpeed(int i) {
        this.mealSpeed = i;
    }

    public final int getSnackStatus() {
        return this.snackStatus;
    }

    public final void setSnackStatus(int i) {
        this.snackStatus = i;
    }

    public final double getSnackAmount() {
        return this.snackAmount;
    }

    public final void setSnackAmount(double d) {
        this.snackAmount = d;
    }

    public final double getSnackInjAmount() {
        return this.snackInjAmount;
    }

    public final void setSnackInjAmount(double d) {
        this.snackInjAmount = d;
    }

    public final int getSnackSpeed() {
        return this.snackSpeed;
    }

    public final void setSnackSpeed(int i) {
        this.snackSpeed = i;
    }

    public final int getSquareStatus() {
        return this.squareStatus;
    }

    public final void setSquareStatus(int i) {
        this.squareStatus = i;
    }

    public final int getSquareTime() {
        return this.squareTime;
    }

    public final void setSquareTime(int i) {
        this.squareTime = i;
    }

    public final int getSquareInjTime() {
        return this.squareInjTime;
    }

    public final void setSquareInjTime(int i) {
        this.squareInjTime = i;
    }

    public final double getSquareAmount() {
        return this.squareAmount;
    }

    public final void setSquareAmount(double d) {
        this.squareAmount = d;
    }

    public final double getSquareInjAmount() {
        return this.squareInjAmount;
    }

    public final void setSquareInjAmount(double d) {
        this.squareInjAmount = d;
    }

    public final int getDualStatus() {
        return this.dualStatus;
    }

    public final void setDualStatus(int i) {
        this.dualStatus = i;
    }

    public final double getDualAmount() {
        return this.dualAmount;
    }

    public final void setDualAmount(double d) {
        this.dualAmount = d;
    }

    public final double getDualInjAmount() {
        return this.dualInjAmount;
    }

    public final void setDualInjAmount(double d) {
        this.dualInjAmount = d;
    }

    public final int getDualSquareTime() {
        return this.dualSquareTime;
    }

    public final void setDualSquareTime(int i) {
        this.dualSquareTime = i;
    }

    public final int getDualInjSquareTime() {
        return this.dualInjSquareTime;
    }

    public final void setDualInjSquareTime(int i) {
        this.dualInjSquareTime = i;
    }

    public final double getDualSquareAmount() {
        return this.dualSquareAmount;
    }

    public final void setDualSquareAmount(double d) {
        this.dualSquareAmount = d;
    }

    public final double getDualInjSquareAmount() {
        return this.dualInjSquareAmount;
    }

    public final void setDualInjSquareAmount(double d) {
        this.dualInjSquareAmount = d;
    }

    public final int getRecentKind1() {
        return this.recentKind1;
    }

    public final void setRecentKind1(int i) {
        this.recentKind1 = i;
    }

    public final int getRecentTime1() {
        return this.recentTime1;
    }

    public final void setRecentTime1(int i) {
        this.recentTime1 = i;
    }

    public final double getRecentAmount1() {
        return this.recentAmount1;
    }

    public final void setRecentAmount1(double d) {
        this.recentAmount1 = d;
    }

    public final int getRecentKind2() {
        return this.recentKind2;
    }

    public final void setRecentKind2(int i) {
        this.recentKind2 = i;
    }

    public final int getRecentTime2() {
        return this.recentTime2;
    }

    public final void setRecentTime2(int i) {
        this.recentTime2 = i;
    }

    public final double getRecentAmount2() {
        return this.recentAmount2;
    }

    public final void setRecentAmount2(double d) {
        this.recentAmount2 = d;
    }

    public final double getTodayBaseAmount() {
        return this.todayBaseAmount;
    }

    public final void setTodayBaseAmount(double d) {
        this.todayBaseAmount = d;
    }

    public final double getTodayMealAmount() {
        return this.todayMealAmount;
    }

    public final void setTodayMealAmount(double d) {
        this.todayMealAmount = d;
    }

    public final double getTodaySnackAmount() {
        return this.todaySnackAmount;
    }

    public final void setTodaySnackAmount(double d) {
        this.todaySnackAmount = d;
    }

    public final int getMorningHour() {
        return this.morningHour;
    }

    public final void setMorningHour(int i) {
        this.morningHour = i;
    }

    public final double getMorningAmount() {
        return this.morningAmount;
    }

    public final void setMorningAmount(double d) {
        this.morningAmount = d;
    }

    public final int getLunchHour() {
        return this.lunchHour;
    }

    public final void setLunchHour(int i) {
        this.lunchHour = i;
    }

    public final double getLunchAmount() {
        return this.lunchAmount;
    }

    public final void setLunchAmount(double d) {
        this.lunchAmount = d;
    }

    public final int getDinnerHour() {
        return this.dinnerHour;
    }

    public final void setDinnerHour(int i) {
        this.dinnerHour = i;
    }

    public final double getDinnerAmount() {
        return this.dinnerAmount;
    }

    public final void setDinnerAmount(double d) {
        this.dinnerAmount = d;
    }

    public final int getCurrentBasePattern() {
        return this.currentBasePattern;
    }

    public final void setCurrentBasePattern(int i) {
        this.currentBasePattern = i;
    }

    public final int getCurrentBaseHour() {
        return this.currentBaseHour;
    }

    public final void setCurrentBaseHour(int i) {
        this.currentBaseHour = i;
    }

    public final double getCurrentBaseTbBeforeAmount() {
        return this.currentBaseTbBeforeAmount;
    }

    public final void setCurrentBaseTbBeforeAmount(double d) {
        this.currentBaseTbBeforeAmount = d;
    }

    public final double getCurrentBaseTbAfterAmount() {
        return this.currentBaseTbAfterAmount;
    }

    public final void setCurrentBaseTbAfterAmount(double d) {
        this.currentBaseTbAfterAmount = d;
    }

    public final double getBaseAmount1() {
        return this.baseAmount1;
    }

    public final void setBaseAmount1(double d) {
        this.baseAmount1 = d;
    }

    public final double getBaseAmount2() {
        return this.baseAmount2;
    }

    public final void setBaseAmount2(double d) {
        this.baseAmount2 = d;
    }

    public final double getBaseAmount3() {
        return this.baseAmount3;
    }

    public final void setBaseAmount3(double d) {
        this.baseAmount3 = d;
    }

    public final double getBaseAmount4() {
        return this.baseAmount4;
    }

    public final void setBaseAmount4(double d) {
        this.baseAmount4 = d;
    }

    public final double getBaseAmount5() {
        return this.baseAmount5;
    }

    public final void setBaseAmount5(double d) {
        this.baseAmount5 = d;
    }

    public final double getBaseAmount6() {
        return this.baseAmount6;
    }

    public final void setBaseAmount6(double d) {
        this.baseAmount6 = d;
    }

    public final double getBaseAmount7() {
        return this.baseAmount7;
    }

    public final void setBaseAmount7(double d) {
        this.baseAmount7 = d;
    }

    public final double getBaseAmount8() {
        return this.baseAmount8;
    }

    public final void setBaseAmount8(double d) {
        this.baseAmount8 = d;
    }

    public final double getBaseAmount9() {
        return this.baseAmount9;
    }

    public final void setBaseAmount9(double d) {
        this.baseAmount9 = d;
    }

    public final double getBaseAmount10() {
        return this.baseAmount10;
    }

    public final void setBaseAmount10(double d) {
        this.baseAmount10 = d;
    }

    public final double getBaseAmount11() {
        return this.baseAmount11;
    }

    public final void setBaseAmount11(double d) {
        this.baseAmount11 = d;
    }

    public final double getBaseAmount12() {
        return this.baseAmount12;
    }

    public final void setBaseAmount12(double d) {
        this.baseAmount12 = d;
    }

    public final double getBaseAmount13() {
        return this.baseAmount13;
    }

    public final void setBaseAmount13(double d) {
        this.baseAmount13 = d;
    }

    public final double getBaseAmount14() {
        return this.baseAmount14;
    }

    public final void setBaseAmount14(double d) {
        this.baseAmount14 = d;
    }

    public final double getBaseAmount15() {
        return this.baseAmount15;
    }

    public final void setBaseAmount15(double d) {
        this.baseAmount15 = d;
    }

    public final double getBaseAmount16() {
        return this.baseAmount16;
    }

    public final void setBaseAmount16(double d) {
        this.baseAmount16 = d;
    }

    public final double getBaseAmount17() {
        return this.baseAmount17;
    }

    public final void setBaseAmount17(double d) {
        this.baseAmount17 = d;
    }

    public final double getBaseAmount18() {
        return this.baseAmount18;
    }

    public final void setBaseAmount18(double d) {
        this.baseAmount18 = d;
    }

    public final double getBaseAmount19() {
        return this.baseAmount19;
    }

    public final void setBaseAmount19(double d) {
        this.baseAmount19 = d;
    }

    public final double getBaseAmount20() {
        return this.baseAmount20;
    }

    public final void setBaseAmount20(double d) {
        this.baseAmount20 = d;
    }

    public final double getBaseAmount21() {
        return this.baseAmount21;
    }

    public final void setBaseAmount21(double d) {
        this.baseAmount21 = d;
    }

    public final double getBaseAmount22() {
        return this.baseAmount22;
    }

    public final void setBaseAmount22(double d) {
        this.baseAmount22 = d;
    }

    public final double getBaseAmount23() {
        return this.baseAmount23;
    }

    public final void setBaseAmount23(double d) {
        this.baseAmount23 = d;
    }

    public final double getBaseAmount24() {
        return this.baseAmount24;
    }

    public final void setBaseAmount24(double d) {
        this.baseAmount24 = d;
    }

    public final int getOtpNumber() {
        return this.otpNumber;
    }

    public final void setOtpNumber(int i) {
        this.otpNumber = i;
    }

    public final double getBolusingSetAmount() {
        return this.bolusingSetAmount;
    }

    public final void setBolusingSetAmount(double d) {
        this.bolusingSetAmount = d;
    }

    public final double getBolusingInjAmount() {
        return this.bolusingInjAmount;
    }

    public final void setBolusingInjAmount(double d) {
        this.bolusingInjAmount = d;
    }

    public final int getBolusingSpeed() {
        return this.bolusingSpeed;
    }

    public final void setBolusingSpeed(int i) {
        this.bolusingSpeed = i;
    }

    public final int getBolusingInjProgress() {
        return this.bolusingInjProgress;
    }

    public final void setBolusingInjProgress(int i) {
        this.bolusingInjProgress = i;
    }

    public final boolean getInfusionEnabled() {
        return this.infusionEnabled;
    }

    public final void setInfusionEnabled(boolean z) {
        this.infusionEnabled = z;
    }

    public final boolean getBluetoothEnabled() {
        return this.bluetoothEnabled;
    }

    public final void setBluetoothEnabled(boolean z) {
        this.bluetoothEnabled = z;
    }

    public final double getMaxBolusSingle() {
        return this.maxBolusSingle;
    }

    public final void setMaxBolusSingle(double d) {
        this.maxBolusSingle = d;
    }

    public final double getMaxBasalSingle() {
        return this.maxBasalSingle;
    }

    public final void setMaxBasalSingle(double d) {
        this.maxBasalSingle = d;
    }

    public final double getBolusDailyTotalThreshold() {
        return this.bolusDailyTotalThreshold;
    }

    public final void setBolusDailyTotalThreshold(double d) {
        this.bolusDailyTotalThreshold = d;
    }

    public final double getBasalDailyTotalThreshold() {
        return this.basalDailyTotalThreshold;
    }

    public final void setBasalDailyTotalThreshold(double d) {
        this.basalDailyTotalThreshold = d;
    }

    public final double getFixedExhaustThreshold() {
        return this.fixedExhaustThreshold;
    }

    public final void setFixedExhaustThreshold(double d) {
        this.fixedExhaustThreshold = d;
    }
}