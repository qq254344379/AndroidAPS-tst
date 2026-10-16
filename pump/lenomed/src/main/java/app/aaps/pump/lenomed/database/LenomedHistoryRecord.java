package app.aaps.pump.lenomed.database;

import androidx.constraintlayout.core.motion.utils.TypedValues;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import net.openid.appauth.ResponseTypeValues;

/* JADX INFO: compiled from: LenomedHistoryRecord.kt */
/* JADX INFO: loaded from: classes4.dex */
@Metadata(d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0005\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\bD\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0087\b\u0018\u00002\u00020\u0001B\u009b\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u0012\b\b\u0002\u0010\n\u001a\u00020\t\u0012\b\b\u0002\u0010\u000b\u001a\u00020\f\u0012\b\b\u0002\u0010\r\u001a\u00020\u0007\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0007\u0012\b\b\u0002\u0010\u000f\u001a\u00020\t\u0012\b\b\u0002\u0010\u0010\u001a\u00020\f\u0012\b\b\u0002\u0010\u0011\u001a\u00020\f\u0012\b\b\u0002\u0010\u0012\u001a\u00020\t\u0012\b\b\u0002\u0010\u0013\u001a\u00020\t\u0012\b\b\u0002\u0010\u0014\u001a\u00020\t\u0012\b\b\u0002\u0010\u0015\u001a\u00020\t¢\u0006\u0004\b\u0016\u0010\u0017J\t\u0010@\u001a\u00020\u0003HÆ\u0003J\t\u0010A\u001a\u00020\u0005HÆ\u0003J\t\u0010B\u001a\u00020\u0007HÆ\u0003J\t\u0010C\u001a\u00020\tHÆ\u0003J\t\u0010D\u001a\u00020\tHÆ\u0003J\t\u0010E\u001a\u00020\fHÆ\u0003J\t\u0010F\u001a\u00020\u0007HÆ\u0003J\t\u0010G\u001a\u00020\u0007HÆ\u0003J\t\u0010H\u001a\u00020\tHÆ\u0003J\t\u0010I\u001a\u00020\fHÆ\u0003J\t\u0010J\u001a\u00020\fHÆ\u0003J\t\u0010K\u001a\u00020\tHÆ\u0003J\t\u0010L\u001a\u00020\tHÆ\u0003J\t\u0010M\u001a\u00020\tHÆ\u0003J\t\u0010N\u001a\u00020\tHÆ\u0003J\u009f\u0001\u0010O\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\t2\b\b\u0002\u0010\n\u001a\u00020\t2\b\b\u0002\u0010\u000b\u001a\u00020\f2\b\b\u0002\u0010\r\u001a\u00020\u00072\b\b\u0002\u0010\u000e\u001a\u00020\u00072\b\b\u0002\u0010\u000f\u001a\u00020\t2\b\b\u0002\u0010\u0010\u001a\u00020\f2\b\b\u0002\u0010\u0011\u001a\u00020\f2\b\b\u0002\u0010\u0012\u001a\u00020\t2\b\b\u0002\u0010\u0013\u001a\u00020\t2\b\b\u0002\u0010\u0014\u001a\u00020\t2\b\b\u0002\u0010\u0015\u001a\u00020\tHÆ\u0001J\u0013\u0010P\u001a\u00020Q2\b\u0010R\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010S\u001a\u00020\fHÖ\u0001J\t\u0010T\u001a\u00020\tHÖ\u0001R\u001e\u0010\u0002\u001a\u00020\u00038\u0006@\u0006X\u0087\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0018\u0010\u0019\"\u0004\b\u001a\u0010\u001bR\u001a\u0010\u0004\u001a\u00020\u0005X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u001c\u0010\u001d\"\u0004\b\u001e\u0010\u001fR\u001a\u0010\u0006\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b \u0010!\"\u0004\b\"\u0010#R\u001a\u0010\b\u001a\u00020\tX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b$\u0010%\"\u0004\b&\u0010'R\u001a\u0010\n\u001a\u00020\tX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b(\u0010%\"\u0004\b)\u0010'R\u001a\u0010\u000b\u001a\u00020\fX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b*\u0010+\"\u0004\b,\u0010-R\u001a\u0010\r\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b.\u0010!\"\u0004\b/\u0010#R\u001a\u0010\u000e\u001a\u00020\u0007X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b0\u0010!\"\u0004\b1\u0010#R\u001a\u0010\u000f\u001a\u00020\tX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b2\u0010%\"\u0004\b3\u0010'R\u001a\u0010\u0010\u001a\u00020\fX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b4\u0010+\"\u0004\b5\u0010-R\u001a\u0010\u0011\u001a\u00020\fX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b6\u0010+\"\u0004\b7\u0010-R\u001a\u0010\u0012\u001a\u00020\tX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b8\u0010%\"\u0004\b9\u0010'R\u001a\u0010\u0013\u001a\u00020\tX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b:\u0010%\"\u0004\b;\u0010'R\u001a\u0010\u0014\u001a\u00020\tX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b<\u0010%\"\u0004\b=\u0010'R\u001a\u0010\u0015\u001a\u00020\tX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b>\u0010%\"\u0004\b?\u0010'¨\u0006U"}, d2 = {"Lapp/aaps/pump/lenomed/database/LenomedHistoryRecord;", "", "timestamp", "", ResponseTypeValues.CODE, "", "value", "", "bolusType", "", "stringValue", TypedValues.TransitionType.S_DURATION, "", "dailyBasal", "dailyBolus", "alarm", "lognum", "wrappingCount", "pumpUid", "refillType", "basalType", "dateTimeString", "<init>", "(JBDLjava/lang/String;Ljava/lang/String;IDDLjava/lang/String;IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getTimestamp", "()J", "setTimestamp", "(J)V", "getCode", "()B", "setCode", "(B)V", "getValue", "()D", "setValue", "(D)V", "getBolusType", "()Ljava/lang/String;", "setBolusType", "(Ljava/lang/String;)V", "getStringValue", "setStringValue", "getDuration", "()I", "setDuration", "(I)V", "getDailyBasal", "setDailyBasal", "getDailyBolus", "setDailyBolus", "getAlarm", "setAlarm", "getLognum", "setLognum", "getWrappingCount", "setWrappingCount", "getPumpUid", "setPumpUid", "getRefillType", "setRefillType", "getBasalType", "setBasalType", "getDateTimeString", "setDateTimeString", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "component10", "component11", "component12", "component13", "component14", "component15", "copy", "equals", "", "other", "hashCode", "toString", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final /* data */ class LenomedHistoryRecord {
    private String alarm;
    private String basalType;
    private String bolusType;
    private byte code;
    private double dailyBasal;
    private double dailyBolus;
    private String dateTimeString;
    private int duration;
    private int lognum;
    private String pumpUid;
    private String refillType;
    private String stringValue;
    private long timestamp;
    private double value;
    private int wrappingCount;

    public static /* synthetic */ LenomedHistoryRecord copy$default(LenomedHistoryRecord lenomedHistoryRecord, long j, byte b, double d, String str, String str2, int i, double d2, double d3, String str3, int i2, int i3, String str4, String str5, String str6, String str7, int i4, Object obj) {
        long j2 = (i4 & 1) != 0 ? lenomedHistoryRecord.timestamp : j;
        byte b2 = (i4 & 2) != 0 ? lenomedHistoryRecord.code : b;
        double d4 = (i4 & 4) != 0 ? lenomedHistoryRecord.value : d;
        String str8 = (i4 & 8) != 0 ? lenomedHistoryRecord.bolusType : str;
        String str9 = (i4 & 16) != 0 ? lenomedHistoryRecord.stringValue : str2;
        int i5 = (i4 & 32) != 0 ? lenomedHistoryRecord.duration : i;
        double d5 = (i4 & 64) != 0 ? lenomedHistoryRecord.dailyBasal : d2;
        double d6 = (i4 & 128) != 0 ? lenomedHistoryRecord.dailyBolus : d3;
        String str10 = (i4 & 256) != 0 ? lenomedHistoryRecord.alarm : str3;
        int i6 = (i4 & 512) != 0 ? lenomedHistoryRecord.lognum : i2;
        long j3 = j2;
        int i7 = (i4 & 1024) != 0 ? lenomedHistoryRecord.wrappingCount : i3;
        return lenomedHistoryRecord.copy(j3, b2, d4, str8, str9, i5, d5, d6, str10, i6, i7, (i4 & 2048) != 0 ? lenomedHistoryRecord.pumpUid : str4, (i4 & 4096) != 0 ? lenomedHistoryRecord.refillType : str5, (i4 & 8192) != 0 ? lenomedHistoryRecord.basalType : str6, (i4 & 16384) != 0 ? lenomedHistoryRecord.dateTimeString : str7);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final long getTimestamp() {
        return this.timestamp;
    }

    /* JADX INFO: renamed from: component10, reason: from getter */
    public final int getLognum() {
        return this.lognum;
    }

    /* JADX INFO: renamed from: component11, reason: from getter */
    public final int getWrappingCount() {
        return this.wrappingCount;
    }

    /* JADX INFO: renamed from: component12, reason: from getter */
    public final String getPumpUid() {
        return this.pumpUid;
    }

    /* JADX INFO: renamed from: component13, reason: from getter */
    public final String getRefillType() {
        return this.refillType;
    }

    /* JADX INFO: renamed from: component14, reason: from getter */
    public final String getBasalType() {
        return this.basalType;
    }

    /* JADX INFO: renamed from: component15, reason: from getter */
    public final String getDateTimeString() {
        return this.dateTimeString;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final byte getCode() {
        return this.code;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final double getValue() {
        return this.value;
    }

    /* JADX INFO: renamed from: component4, reason: from getter */
    public final String getBolusType() {
        return this.bolusType;
    }

    /* JADX INFO: renamed from: component5, reason: from getter */
    public final String getStringValue() {
        return this.stringValue;
    }

    /* JADX INFO: renamed from: component6, reason: from getter */
    public final int getDuration() {
        return this.duration;
    }

    /* JADX INFO: renamed from: component7, reason: from getter */
    public final double getDailyBasal() {
        return this.dailyBasal;
    }

    /* JADX INFO: renamed from: component8, reason: from getter */
    public final double getDailyBolus() {
        return this.dailyBolus;
    }

    /* JADX INFO: renamed from: component9, reason: from getter */
    public final String getAlarm() {
        return this.alarm;
    }

    public final LenomedHistoryRecord copy(long timestamp, byte code, double value, String bolusType, String stringValue, int duration, double dailyBasal, double dailyBolus, String alarm, int lognum, int wrappingCount, String pumpUid, String refillType, String basalType, String dateTimeString) {
        Intrinsics.checkNotNullParameter(bolusType, "bolusType");
        Intrinsics.checkNotNullParameter(stringValue, "stringValue");
        Intrinsics.checkNotNullParameter(alarm, "alarm");
        Intrinsics.checkNotNullParameter(pumpUid, "pumpUid");
        Intrinsics.checkNotNullParameter(refillType, "refillType");
        Intrinsics.checkNotNullParameter(basalType, "basalType");
        Intrinsics.checkNotNullParameter(dateTimeString, "dateTimeString");
        return new LenomedHistoryRecord(timestamp, code, value, bolusType, stringValue, duration, dailyBasal, dailyBolus, alarm, lognum, wrappingCount, pumpUid, refillType, basalType, dateTimeString);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LenomedHistoryRecord)) {
            return false;
        }
        LenomedHistoryRecord lenomedHistoryRecord = (LenomedHistoryRecord) other;
        return this.timestamp == lenomedHistoryRecord.timestamp && this.code == lenomedHistoryRecord.code && Double.compare(this.value, lenomedHistoryRecord.value) == 0 && Intrinsics.areEqual(this.bolusType, lenomedHistoryRecord.bolusType) && Intrinsics.areEqual(this.stringValue, lenomedHistoryRecord.stringValue) && this.duration == lenomedHistoryRecord.duration && Double.compare(this.dailyBasal, lenomedHistoryRecord.dailyBasal) == 0 && Double.compare(this.dailyBolus, lenomedHistoryRecord.dailyBolus) == 0 && Intrinsics.areEqual(this.alarm, lenomedHistoryRecord.alarm) && this.lognum == lenomedHistoryRecord.lognum && this.wrappingCount == lenomedHistoryRecord.wrappingCount && Intrinsics.areEqual(this.pumpUid, lenomedHistoryRecord.pumpUid) && Intrinsics.areEqual(this.refillType, lenomedHistoryRecord.refillType) && Intrinsics.areEqual(this.basalType, lenomedHistoryRecord.basalType) && Intrinsics.areEqual(this.dateTimeString, lenomedHistoryRecord.dateTimeString);
    }

    public int hashCode() {
        return (((((((((((((((((((((((((((Long.hashCode(this.timestamp) * 31) + Byte.hashCode(this.code)) * 31) + Double.hashCode(this.value)) * 31) + this.bolusType.hashCode()) * 31) + this.stringValue.hashCode()) * 31) + Integer.hashCode(this.duration)) * 31) + Double.hashCode(this.dailyBasal)) * 31) + Double.hashCode(this.dailyBolus)) * 31) + this.alarm.hashCode()) * 31) + Integer.hashCode(this.lognum)) * 31) + Integer.hashCode(this.wrappingCount)) * 31) + this.pumpUid.hashCode()) * 31) + this.refillType.hashCode()) * 31) + this.basalType.hashCode()) * 31) + this.dateTimeString.hashCode();
    }

    public String toString() {
        long j = this.timestamp;
        byte b = this.code;
        return "LenomedHistoryRecord(timestamp=" + j + ", code=" + ((int) b) + ", value=" + this.value + ", bolusType=" + this.bolusType + ", stringValue=" + this.stringValue + ", duration=" + this.duration + ", dailyBasal=" + this.dailyBasal + ", dailyBolus=" + this.dailyBolus + ", alarm=" + this.alarm + ", lognum=" + this.lognum + ", wrappingCount=" + this.wrappingCount + ", pumpUid=" + this.pumpUid + ", refillType=" + this.refillType + ", basalType=" + this.basalType + ", dateTimeString=" + this.dateTimeString + ")";
    }

    public LenomedHistoryRecord(long j, byte b, double d, String bolusType, String stringValue, int i, double d2, double d3, String alarm, int i2, int i3, String pumpUid, String refillType, String basalType, String dateTimeString) {
        Intrinsics.checkNotNullParameter(bolusType, "bolusType");
        Intrinsics.checkNotNullParameter(stringValue, "stringValue");
        Intrinsics.checkNotNullParameter(alarm, "alarm");
        Intrinsics.checkNotNullParameter(pumpUid, "pumpUid");
        Intrinsics.checkNotNullParameter(refillType, "refillType");
        Intrinsics.checkNotNullParameter(basalType, "basalType");
        Intrinsics.checkNotNullParameter(dateTimeString, "dateTimeString");
        this.timestamp = j;
        this.code = b;
        this.value = d;
        this.bolusType = bolusType;
        this.stringValue = stringValue;
        this.duration = i;
        this.dailyBasal = d2;
        this.dailyBolus = d3;
        this.alarm = alarm;
        this.lognum = i2;
        this.wrappingCount = i3;
        this.pumpUid = pumpUid;
        this.refillType = refillType;
        this.basalType = basalType;
        this.dateTimeString = dateTimeString;
    }

    public final long getTimestamp() {
        return this.timestamp;
    }

    public final void setTimestamp(long j) {
        this.timestamp = j;
    }

    public final byte getCode() {
        return this.code;
    }

    public final void setCode(byte b) {
        this.code = b;
    }

    public final double getValue() {
        return this.value;
    }

    public final void setValue(double d) {
        this.value = d;
    }

    /*  JADX ERROR: NullPointerException in pass: InitCodeVariables
        java.lang.NullPointerException: Cannot invoke "jadx.core.dex.instructions.args.SSAVar.getPhiList()" because "resultVar" is null
        	at jadx.core.dex.visitors.InitCodeVariables.collectConnectedVars(InitCodeVariables.java:119)
        	at jadx.core.dex.visitors.InitCodeVariables.setCodeVar(InitCodeVariables.java:82)
        	at jadx.core.dex.visitors.InitCodeVariables.initCodeVar(InitCodeVariables.java:74)
        	at jadx.core.dex.visitors.InitCodeVariables.initCodeVars(InitCodeVariables.java:48)
        	at jadx.core.dex.visitors.InitCodeVariables.visit(InitCodeVariables.java:29)
        */
    public /* synthetic */ LenomedHistoryRecord(long r23, byte r25, double r26, java.lang.String r28, java.lang.String r29, int r30, double r31, double r33, java.lang.String r35, int r36, int r37, java.lang.String r38, java.lang.String r39, java.lang.String r40, java.lang.String r41, int r42, kotlin.jvm.internal.DefaultConstructorMarker r43) {
        /*
            r22 = this;
            r0 = r42
            r1 = r0 & 2
            if (r1 == 0) goto La
            r1 = 15
            r5 = r1
            goto Lc
        La:
            r5 = r25
        Lc:
            r1 = r0 & 4
            r2 = 0
            if (r1 == 0) goto L14
            r6 = r2
            goto L16
        L14:
            r6 = r26
        L16:
            r1 = r0 & 8
            if (r1 == 0) goto L1e
            java.lang.String r1 = "None"
            r8 = r1
            goto L20
        L1e:
            r8 = r28
        L20:
            r1 = r0 & 16
            java.lang.String r4 = ""
            if (r1 == 0) goto L28
            r9 = r4
            goto L2a
        L28:
            r9 = r29
        L2a:
            r1 = r0 & 32
            r10 = 0
            if (r1 == 0) goto L31
            r1 = r10
            goto L33
        L31:
            r1 = r30
        L33:
            r11 = r0 & 64
            if (r11 == 0) goto L39
            r11 = r2
            goto L3b
        L39:
            r11 = r31
        L3b:
            r13 = r0 & 128(0x80, float:1.8E-43)
            if (r13 == 0) goto L41
            r13 = r2
            goto L43
        L41:
            r13 = r33
        L43:
            r2 = r0 & 256(0x100, float:3.59E-43)
            if (r2 == 0) goto L49
            r15 = r4
            goto L4b
        L49:
            r15 = r35
        L4b:
            r2 = r0 & 512(0x200, float:7.17E-43)
            if (r2 == 0) goto L52
            r16 = r10
            goto L54
        L52:
            r16 = r36
        L54:
            r2 = r0 & 1024(0x400, float:1.435E-42)
            if (r2 == 0) goto L5b
            r17 = r10
            goto L5d
        L5b:
            r17 = r37
        L5d:
            r2 = r0 & 2048(0x800, float:2.87E-42)
            if (r2 == 0) goto L64
            r18 = r4
            goto L66
        L64:
            r18 = r38
        L66:
            r2 = r0 & 4096(0x1000, float:5.74E-42)
            if (r2 == 0) goto L6d
            r19 = r4
            goto L6f
        L6d:
            r19 = r39
        L6f:
            r2 = r0 & 8192(0x2000, float:1.148E-41)
            if (r2 == 0) goto L76
            r20 = r4
            goto L78
        L76:
            r20 = r40
        L78:
            r0 = r0 & 16384(0x4000, float:2.2959E-41)
            if (r0 == 0) goto L84
            r21 = r4
            r2 = r22
            r10 = r1
            r3 = r23
            goto L8b
        L84:
            r21 = r41
            r2 = r22
            r3 = r23
            r10 = r1
        L8b:
            r2.<init>(r3, r5, r6, r8, r9, r10, r11, r13, r15, r16, r17, r18, r19, r20, r21)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: app.aaps.pump.lenomed.database.LenomedHistoryRecord.<init>(long, byte, double, java.lang.String, java.lang.String, int, double, double, java.lang.String, int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, kotlin.jvm.internal.DefaultConstructorMarker):void");
    }

    public final String getBolusType() {
        return this.bolusType;
    }

    public final void setBolusType(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.bolusType = str;
    }

    public final String getStringValue() {
        return this.stringValue;
    }

    public final void setStringValue(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.stringValue = str;
    }

    public final int getDuration() {
        return this.duration;
    }

    public final void setDuration(int i) {
        this.duration = i;
    }

    public final double getDailyBasal() {
        return this.dailyBasal;
    }

    public final void setDailyBasal(double d) {
        this.dailyBasal = d;
    }

    public final double getDailyBolus() {
        return this.dailyBolus;
    }

    public final void setDailyBolus(double d) {
        this.dailyBolus = d;
    }

    public final String getAlarm() {
        return this.alarm;
    }

    public final void setAlarm(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.alarm = str;
    }

    public final int getLognum() {
        return this.lognum;
    }

    public final void setLognum(int i) {
        this.lognum = i;
    }

    public final int getWrappingCount() {
        return this.wrappingCount;
    }

    public final void setWrappingCount(int i) {
        this.wrappingCount = i;
    }

    public final String getPumpUid() {
        return this.pumpUid;
    }

    public final void setPumpUid(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.pumpUid = str;
    }

    public final String getRefillType() {
        return this.refillType;
    }

    public final void setRefillType(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.refillType = str;
    }

    public final String getBasalType() {
        return this.basalType;
    }

    public final void setBasalType(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.basalType = str;
    }

    public final String getDateTimeString() {
        return this.dateTimeString;
    }

    public final void setDateTimeString(String str) {
        Intrinsics.checkNotNullParameter(str, "<set-?>");
        this.dateTimeString = str;
    }
}