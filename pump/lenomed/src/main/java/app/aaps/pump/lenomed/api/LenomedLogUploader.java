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