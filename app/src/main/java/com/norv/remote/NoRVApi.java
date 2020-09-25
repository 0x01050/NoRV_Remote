package com.norv.remote;

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

    final private AsyncHttpClient httpClient;

    private NoRVApi() {
        httpClient = new AsyncHttpClient();
        httpClient.addHeader("Accept", "application/json");
        httpClient.setTimeout(30000);
    }

    public interface StatusListener {
        void onSuccess(String status, String ignorable, String runningTime, String breaksNumber);
        void onFailure(String errorMsg);
    }

    public void getStatus(final StatusListener listener) {
        try {
            httpClient.get(BuildConfig.CLIENT_SERVER + "/getStatus", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        String[] results = resp.split(",");
                        if(results.length == 4)
                            listener.onSuccess(results[0], results[1], results[2], results[3]);
                        else
                            listener.onFailure("Invalid Response");
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

    public interface ControlListener {
        void onSuccess(String respMsg);
        void onFailure(String errorMsg);
    }

    public void controlDeposition(String action, RequestParams params, final ControlListener listener) {
        try {
            httpClient.post(BuildConfig.CLIENT_SERVER + "/" + action, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        if (resp.contains(NoRVConst.SuccessKey)) {
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
