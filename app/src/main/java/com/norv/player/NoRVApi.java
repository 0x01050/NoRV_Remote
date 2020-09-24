package com.norv.player;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;

public class NoRVApi {

    private static NoRVApi __instance = null;
    public static NoRVApi getInstance() {
        if (__instance == null)
            __instance = new NoRVApi();
        return __instance;
    }

    private AsyncHttpClient httpClient;

    private NoRVApi() {
        httpClient = new AsyncHttpClient();
        httpClient.addHeader("Accept", "application/json");
        httpClient.setTimeout(30000);
    }

    public interface ApiListener {
        void onSuccess(String respMsg);
        void onFailure(String errorMsg);
    }

    public void getStatus(final ApiListener listener) {
        try {
            httpClient.get(BuildConfig.CLIENT_SERVER + "/getStatus", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        listener.onSuccess(resp);
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    listener.onFailure("Unknown Error");
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void controlDeposition(String action, RequestParams params, final ApiListener listener) {
        try {
            httpClient.post(BuildConfig.CLIENT_SERVER + "/" + action, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        if (resp.contains(NoRVConst.SucessKey)) {
                            listener.onSuccess(resp);
                        } else {
                            listener.onFailure(resp);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onFailure(e.getMessage());
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    listener.onFailure("Unknown Error");
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
