package com.udp.master;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.bean.common.FunCode;
import com.bean.common.STBIPAddress;
import com.bean.common.SimpleDate;
import com.udp.master.model.FailedRequestJsonModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MasterHttpHelper
{

    private static class MaReqJson extends JSONObject {

        private JSONObject headerJson;
        private JSONObject bodyJson;
        private static String funCode = "funcode";
        private static String reqTime = "reqtime";
        private static String deviceIp = "deviceip";
        private static String header = "header";
        private static String body = "body";

        MaReqJson(int funCodeInt, JSONObject bodyJson) throws JSONException {

            this.bodyJson = bodyJson;
            this.headerJson = new JSONObject();
            headerJson.put(funCode, funCodeInt);
            headerJson.put(reqTime, SimpleDate.getCurrentDate());
            headerJson.put(deviceIp, STBIPAddress.getLocalHostIp());

            put(header, this.headerJson);
            put(body, this.bodyJson);
        }

        MaReqJson(FunCode funCode, JSONObject bodyJson) throws JSONException {
            this(funCode.value(), bodyJson);
        }
    }

    private static MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

    public static void Post(JSONObject jsonBody, String ip, int port, FunCode funCode) {

        try {
            final MaReqJson maReqJson = new MaReqJson(funCode, jsonBody);
            final String requestJsonString = maReqJson.toString();

            Post(requestJsonString, FunCode.urlString(ip, port), new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("REQ", "失败:"+e.getMessage());
                    saveFailedInfoToDB(requestJsonString);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        final String responseStr = responseBody.string();
                        Log.e("REQ", "成功:"+responseStr);

                        try {
                            MaJson jsonObject = new MaJson(responseStr);
                            final String resultCode = jsonObject.resultCode();
                            final String successCode = "0";
                            if (!TextUtils.equals(resultCode, successCode)) {
                                // 请求失败 缓存至本地
                                saveFailedInfoToDB(requestJsonString);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public static void Post(JSONObject jsonBody, FunCode funCode) {

        try {
            final MaReqJson maReqJson = new MaReqJson(funCode, jsonBody);
            final String requestJsonString = maReqJson.toString();

            Post(requestJsonString, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("REQ", "失败:"+e.getMessage());
                    saveFailedInfoToDB(requestJsonString);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        final String responseStr = responseBody.string();
                        Log.e("REQ", "成功:"+responseStr);

                        try {
                            MaJson jsonObject = new MaJson(responseStr);
                            final String resultCode = jsonObject.resultCode();
                            final String successCode = "0";
                            if (!TextUtils.equals(resultCode, successCode)) {
                                // 缓存至本地
                                saveFailedInfoToDB(requestJsonString);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }

            });


        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public static MasterListener msListener;
    public static synchronized String PostInt(JSONObject jsonBody, int funCodeInt, final MasterListener msListener) {


        String str = "";
        if (MasterHttpHelper.msListener == null)
            MasterHttpHelper.msListener = msListener;
        try {

            str = new MaReqJson(funCodeInt, jsonBody).toString();
            Log.e("MasterHttpHelper", str);

            final String requestJsonString = str;
            Post(requestJsonString, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (MasterHttpHelper.msListener != null)
                        MasterHttpHelper.msListener.msgFromServerErr(e.getMessage());

                    saveFailedInfoToDB(requestJsonString);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                    ResponseBody requestBody = response.body();
                    if (requestBody != null) {

                        final String responseStr = requestBody.string();
                        if (MasterHttpHelper.msListener != null) {
                            MasterHttpHelper.msListener.msgFromServerListData(responseStr);
                            Log.e("MasterHttpHelper", responseStr);
                        }
                    }

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static void Post(JSONObject jsonBody, FunCode funCode, Callback callback) {

        try {
            Post(new MaReqJson(funCode, jsonBody).toString(), callback);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void Post(final String requestJsonString) {

        Log.e("R-POST",requestJsonString);
        Post(requestJsonString, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("REQ", "失败:"+e.getMessage());
                saveFailedInfoToDB(requestJsonString);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    final String responseStr = responseBody.string();

                    Log.e("REQ", "成功:"+responseStr);

                    try {
                        MaJson jsonObject = new MaJson(responseStr);
                        final String resultCode = jsonObject.resultCode();
                        final String successCode = "0";
                        if (!TextUtils.equals(resultCode, successCode)) {
                            // 缓存至本地
                            saveFailedInfoToDB(requestJsonString);
                        } else {
                            if (!TextUtils.equals(jsonObject.funCode(), "")) {
                                FunCode funCode = FunCode.fromInt(Integer.valueOf(jsonObject.funCode()));
                                if (funCode == FunCode.table) {
                                    returnMainPageData(responseStr);
                                }
                                else if (funCode == FunCode.updateDateTime) {
                                    updateTime(responseStr);
                                }
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            // TODO: 返回主页
            private synchronized void returnMainPageData(String responseStr) {
                if (msListener != null)
                    msListener.msgFromServerListData(responseStr);
            }

            // TODO: 设置时间
            private synchronized void updateTime(String responseStr) {
                Log.e("UPDATE DATE AND TIME", "result: "+responseStr);

                try {
                    MaJson jsonObject = new MaJson(responseStr);
                    final String resultCode = jsonObject.resultCode();
                    final String successCode = "0";
                    if (TextUtils.equals(resultCode, successCode)) {
                        // 获取信息成功
                        String dateTime = jsonObject.resultDateTime();
                        // 设置当前机器系统时间日期
                        try {
                            SimpleDate.Sys.setDateTime(MasterManager.getContext(), Long.valueOf(SimpleDate.dateToStamp(dateTime)));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        MasterHttpHelper.saveFailedInfoToDB(requestJsonString);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    // TODO: 保存失败请求信息
    public static synchronized void saveFailedInfoToDB(String requestJsonString) {
        Log.e("REQ", "--: 失败信息正在保存至数据库:"+requestJsonString);

        checkRepeat(requestJsonString);

        FailedRequestJsonModel failedRequestJsonModel = new FailedRequestJsonModel();
        failedRequestJsonModel.setJsonStr(requestJsonString);
        MasterDBDao.getInstance().insertFailedModel(failedRequestJsonModel);
    }

    public static synchronized void Post(String jsonRequestStr, Callback callback) {

        Post(jsonRequestStr, FunCode.urlString, callback);
    }

    public static synchronized void Post(String jsonRequestStr,String urlStr, Callback callback) {

        Log.e("REQ", jsonRequestStr);
        checkRepeat(jsonRequestStr);
        OkHttpClient okHttpClient = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(mediaType, jsonRequestStr);
        final Request request = new Request.Builder().url(urlStr).post(requestBody).build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(callback);
    }

    // TODO: 过滤重复
    private static void checkRepeat(String requestJsonString) {
        List<FailedRequestJsonModel> lists = MasterDBDao.getInstance().queryFailedModels();
        if (lists.size() > 0) {

            try {
                JSONObject jsonObject = new JSONObject(requestJsonString);
                int funCode = jsonObject.getJSONObject("header").getInt("funcode");
                for (FailedRequestJsonModel str:lists) {
                    JSONObject fa = new JSONObject(str.getJsonStr());
                    int failCode = fa.getJSONObject("header").getInt("funcode");
                    if (funCode == failCode) {
                        MasterDBDao.getInstance().removeFailed(str);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
