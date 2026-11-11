package app.aaps.pump.lenomed.api;

import kotlin.Metadata;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/* JADX INFO: compiled from: LenomedApiService.kt */
/* JADX INFO: loaded from: classes4.dex */
public interface LenomedApiService {
    @Headers({"api-key: D7B3DA9FA8229D5253F3D75E1E2B1BA4"})
    @GET("v1/pumplog/last_no")
    Call<LastNoResponse> getPumpLastNo(@Query("pump_uid") String pump_uid, @Query("pump_version") String pump_version, @Query("incarnation_num") int incarnation_num);

    @Headers({"api-key: D7B3DA9FA8229D5253F3D75E1E2B1BA4"})
    @POST("v1/pumplog/save")
    Call<ApiResponse> uploadPumpLogs(@Body PumpLogDto pumpLogDto);
}