package com.norv.remote;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class NoRVApi {

    private static NoRVApi __instance = null;
    public static NoRVApi getInstance() {
        if (__instance == null)
            __instance = new NoRVApi();
        return __instance;
    }

    final private AsyncHttpClient checkClient;
    final private AsyncHttpClient controlClient;
    final private AsyncHttpClient routerClient;

    private NoRVApi() {
        checkClient = new AsyncHttpClient();
        checkClient.addHeader("Accept", "application/json");
        checkClient.setTimeout(2000);
        checkClient.setMaxRetriesAndTimeout(1, 2000);


        controlClient = new AsyncHttpClient();
        controlClient.addHeader("Accept", "application/json");
        controlClient.setTimeout(30000);

        routerClient = new AsyncHttpClient();
        routerClient.addHeader("Accept", "application/json");
        routerClient.setTimeout(15000);
    }

    public interface StatusListener {
        void onSuccess(String status, String ignorable, String runningTime, String breaksNumber);
        void onFailure(String errorMsg);
    }
    public void getStatus(final StatusListener listener) {
        try {
            checkClient.get(BuildConfig.CLIENT_SERVER + "/getStatus", new AsyncHttpResponseHandler() {
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
                    error.printStackTrace();
                    listener.onFailure("Network Failure");
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure("Network Error");
        }
    }

    public interface ControlListener {
        void onSuccess(String respMsg);
        void onFailure(String errorMsg);
    }
    public void controlDeposition(String action, RequestParams params, final ControlListener listener) {
        try {
            controlClient.post(BuildConfig.CLIENT_SERVER + "/" + action, params, new AsyncHttpResponseHandler() {
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
                    error.printStackTrace();
                    listener.onFailure("Network Failure");
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure("Network Error");
        }
    }

    public interface RouterListener {
        void onSuccess(ArrayList<RouterModel> routers);
        void onFailure(String errorMsg);
    }
    public void checkRouterLive(final RouterListener listener) {
        try {
            checkClient.get(BuildConfig.ROUTER + "/router/hello", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        JSONObject respObj = new JSONObject(resp);
                        if(respObj.getInt("code") == 0) {
                            listener.onSuccess(null);
                        } else {
                            String msg = respObj.optString("msg", "Error");
                            listener.onFailure(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onFailure("Invalid Response");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    error.printStackTrace();
                    listener.onFailure("Network Failure");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure("Network Error");
        }
    }

    public void checkRouterInternet(final RouterListener listener) {
        try {
            checkClient.get(BuildConfig.CLIENT_SERVER + "/getInternet", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        if("True".equals(resp))
                            listener.onSuccess(null);
                        else
                            listener.onFailure("Not Connected");
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onFailure("Invalid Response");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    error.printStackTrace();
                    listener.onFailure("Network Failure");
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure("Network Error");
        }
    }

    public void loginRouter(String pwd, final RouterListener listener) {
        try {
            RequestParams params = new RequestParams();
            params.add("pwd", pwd);
            routerClient.post(BuildConfig.ROUTER + "/router/login", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        routerClient.removeHeader("Authorization");
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        JSONObject respObj = new JSONObject(resp);
                        int code = respObj.getInt("code");
                        if(code == 0) {
                            String token = respObj.getString("token");
                            if(token.isEmpty()) {
                                listener.onFailure("Token is missing");
                            }
                            else {
                                routerClient.addHeader("Authorization", token);
                                listener.onSuccess(null);
                            }
                        } else {
                            String msg = respObj.optString("msg", "Error");
                            listener.onFailure(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onFailure("Invalid Response");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    error.printStackTrace();
                    listener.onFailure("Network Failure");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure("Network Error");
        }
    }

    public static class RouterModel {
        public final String mac;
        public final String ssid;
        public final int channel;
        public final String caps;

        public RouterModel(String mac, String ssid, int channel, String caps) {
            this.mac = mac;
            this.ssid = ssid;
            this.channel = channel;
            this.caps = caps;
        }

        @NotNull
        public String toString() {
            if(ssid == null)
                return "";
            return ssid;
        }
    }
    public void scanRouters(final RouterListener listener) {
        try {
            RequestParams params = new RequestParams();

            routerClient.post(BuildConfig.ROUTER + "/repeater/scan", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        JSONObject respObj = new JSONObject(resp);
                        int code = respObj.getInt("code");
                        if(code == 0) {
                            JSONArray wifis = respObj.optJSONArray("wifis");
                            if(wifis == null)
                                wifis = new JSONArray();
                            ArrayList<RouterModel> routerList = new ArrayList<>();
                            for(int i = 0; i < wifis.length(); i ++) {
                                JSONObject wifi = wifis.getJSONObject(i);
                                if(NoRVConst.Router_Default_Device.equals(wifi.getString("device")) && NoRVConst.Router_Default_Encrypt.equals(wifi.getString("encrypt")))
                                    routerList.add(new RouterModel(wifi.getString("mac"), wifi.getString("ssid"), wifi.getInt("channel"), wifi.getString("caps")));
                            }
                            listener.onSuccess(routerList);
                        } else {
                            String msg = respObj.optString("msg", "Error");
                            if(code == -1) {
                                routerClient.removeHeader("Authorization");
                            }
                            listener.onFailure(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onFailure("Invalid Response");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    error.printStackTrace();
                    listener.onFailure("Network Failure");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure("Network Error");
        }
    }

    public void joinRouter(String ssid, String mac, int channel, String caps, String pwd, final RouterListener listener) {
        try {
            RequestParams params = new RequestParams();
            params.add("ssid", ssid);
            params.add("key", pwd);
            params.add("save2uci", String.valueOf(true));
            params.add("channel", String.valueOf(channel));
            params.add("mac", mac);
            params.add("caps", caps);
            params.add("device", NoRVConst.Router_Default_Device);
            params.add("encrypt", NoRVConst.Router_Default_Encrypt);

            routerClient.post(BuildConfig.ROUTER + "/repeater/join", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    try {
                        String resp = new String(responseBody, StandardCharsets.UTF_8);
                        JSONObject respObj = new JSONObject(resp);
                        int code = respObj.getInt("code");
                        if(code == 0)
                            listener.onSuccess(null);
                        else {
                            String msg = respObj.optString("msg", "Error");
                            if(code == -1) {
                                routerClient.removeHeader("Authorization");
                            }
                            listener.onFailure(msg);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onFailure("Invalid Response");
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    error.printStackTrace();
                    listener.onFailure("Network Failure");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailure("Network Error");
        }
    }
}
