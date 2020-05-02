package com.alvin.androideatit.Retrofit;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ICloudFunctions {
    @GET("")
    Observable<ResponseBody> getCustomToken(@Query("access_token") String accessToken);
}
