package app.aaps.pump.lenomed.api;

import app.aaps.core.interfaces.logging.AAPSLogger;
import app.aaps.core.interfaces.logging.LTag;
import javax.inject.Inject;
import javax.inject.Singleton;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/* JADX INFO: compiled from: LenomedLogUploader.kt */
/* JADX INFO: loaded from: classes4.dex */
@Singleton
@Metadata(d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u0007\u0018\u0000 \t2\u00020\u0001:\u0001\tB\u0011\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\b\u0010\b\u001a\u0004\u0018\u00010\u0007R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e¢\u0006\u0002\n\u0000¨\u0006\n"}, d2 = {"Lapp/aaps/pump/lenomed/api/LenomedLogUploader;", "", "aapsLogger", "Lapp/aaps/core/interfaces/logging/AAPSLogger;", "<init>", "(Lapp/aaps/core/interfaces/logging/AAPSLogger;)V", "retrofit", "Lretrofit2/Retrofit;", "getRetrofitInstance", "Companion", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public final class LenomedLogUploader {
    private static final String BASE_URL = "https://api.diaconn.com/aaps/";
    public static final String UPLOAD_API_KEY = "D7B3DA9FA8229D5253F3D75E1E2B1BA4";
    private final AAPSLogger aapsLogger;
    private Retrofit retrofit;

    @Inject
    public LenomedLogUploader(AAPSLogger aapsLogger) {
        Intrinsics.checkNotNullParameter(aapsLogger, "aapsLogger");
        this.aapsLogger = aapsLogger;
    }

    public final Retrofit getRetrofitInstance() {
        this.aapsLogger.debug(LTag.PUMPCOMM, "diaconn pump logs upload instance");
        if (this.retrofit == null) {
            this.retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        }
        return this.retrofit;
    }
}