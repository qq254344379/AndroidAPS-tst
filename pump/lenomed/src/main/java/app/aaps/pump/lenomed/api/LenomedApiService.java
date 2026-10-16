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
@Metadata(d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J,\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0001\u0010\u0005\u001a\u00020\u00062\b\b\u0001\u0010\u0007\u001a\u00020\u00062\b\b\u0001\u0010\b\u001a\u00020\tH'J\u0018\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u000b0\u00032\b\b\u0001\u0010\f\u001a\u00020\rH'ø\u0001\u0000\u0082\u0002\u0006\n\u0004\b!0\u0001¨\u0006\u000eÀ\u0006\u0001"}, d2 = {"Lapp/aaps/pump/lenomed/api/LenomedApiService;", "", "getPumpLastNo", "Lretrofit2/Call;", "Lapp/aaps/pump/lenomed/api/LastNoResponse;", "pump_uid", "", "pump_version", "incarnation_num", "", "uploadPumpLogs", "Lapp/aaps/pump/lenomed/api/ApiResponse;", "pumpLogDto", "Lapp/aaps/pump/lenomed/api/PumpLogDto;", "lenomed_fullRelease"}, k = 1, mv = {2, 2, 0}, xi = 48)
public interface LenomedApiService {
    @Headers({"api-key: D7B3DA9FA8229D5253F3D75E1E2B1BA4"})
    @GET("v1/pumplog/last_no")
    Call<LastNoResponse> getPumpLastNo(@Query("pump_uid") String pump_uid, @Query("pump_version") String pump_version, @Query("incarnation_num") int incarnation_num);

    @Headers({"api-key: D7B3DA9FA8229D5253F3D75E1E2B1BA4"})
    @POST("v1/pumplog/save")
    Call<ApiResponse> uploadPumpLogs(@Body PumpLogDto pumpLogDto);
}