package com.example.parkseva.api;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://pms-backend-nine.vercel.app/";
    private static Retrofit retrofit = null;
    private static Context context;

    public static void init(Context ctx) {
        context = ctx;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .addInterceptor(logging);

            // Add auth token interceptor
            if (context != null) {
                clientBuilder.addInterceptor(chain -> {
                    SharedPreferences prefs = context.getSharedPreferences("ParkSevaPrefs", Context.MODE_PRIVATE);
                    String token = prefs.getString("auth_token", null);
                    
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder();
                    
                    if (token != null) {
                        requestBuilder.addHeader("Authorization", "Bearer " + token);
                    }
                    
                    return chain.proceed(requestBuilder.build());
                });
            }

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(clientBuilder.build())
                    .build();
        }
        return retrofit;
    }

    public static ParkingApi getParkingApi() {
        return getClient().create(ParkingApi.class);
    }
}