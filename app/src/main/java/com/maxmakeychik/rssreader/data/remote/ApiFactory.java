package com.maxmakeychik.rssreader.data.remote;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.maxmakeychik.rssreader.App;
import com.maxmakeychik.rssreader.data.model.ErrorMessage;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class ApiFactory {

    private static final String BASE_URL = "http://www.feedforall.com";

    private static final long READ_TIMEOUT = 60;
    private static final int WRITE_TIMEOUT = 120;
    private static final int CONNECT_TIMEOUT = 10;

    private static final String TAG = "ApiFactory";

    static class ErrorInterceptor implements Interceptor {

        private static final String TAG = "ErrorInterceptor";

        @Override
        public Response intercept(Chain chain) throws IOException {
            //Log.d(TAG, "intercept");
            Request request = chain.request();
            String url = request.url().url().toString();
            Log.i(TAG, "intercept: " + url);
            Response response = chain.proceed(request);
            if (response.code() >= 300 && App.getAppContext() != null) {    //  show error toast
                try {
                    Gson gson = new Gson();
                    String responseBody = response.body().string();
                    Log.i(TAG, "intercept: " + responseBody);
                    ErrorMessage errorMessage = gson.fromJson(responseBody, ErrorMessage.class);
                    createHandler(App.getAppContext(), errorMessage.errorMessage);
                } catch (JsonSyntaxException | IOException e) {
                    e.printStackTrace();
                }
            }
            return response;
        }

        private Handler handler;

        public void createHandler(Context context, String text) {
            handler = new Handler(context.getMainLooper());
            showToast(text);
        }

        public void showToast(String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (text.contains("session id expected"))
                        return;
                    Toast.makeText(App.getAppContext(), text, Toast.LENGTH_LONG).show();
                }
            });
        }

        private void runOnUiThread(Runnable r) {
            handler.post(r);
        }
    }

    private static OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            //.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .addInterceptor(new ErrorInterceptor());

    private static GsonBuilder gsonBuilder = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setDateFormat("dd-MM-yyyy' 'HH:mm:ss");

    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(SimpleXmlConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(httpClientBuilder.build());

    public static <S> S createService(Class<S> serviceClass) {
        Retrofit retrofit = builder.client(httpClientBuilder.build()).build();
        return retrofit.create(serviceClass);
    }
}