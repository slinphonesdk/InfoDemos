package com.udp.master;

import com.udp.master.model.ExPhoneModel;
import com.udp.master.model.SessionIDModel;
import com.udp.master.model.SessionIDState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MaJson extends JSONObject
{

    public MaJson(SessionIDModel sessionIDModel) throws JSONException {

        put("callsip", sessionIDModel.getCallsip());
        put("tosip", sessionIDModel.getTosip());
        put("callip", sessionIDModel.getFromIPAddress().getHostAddress());
        put("tocallip", sessionIDModel.getToIPAddress().getHostAddress());
        put("callbegintime", sessionIDModel.getGeneratingDate());
        put("callreceivetime", sessionIDModel.getForwardDate());
        put("callendtime", sessionIDModel.getCallEndDate());
        put("callrefusetime", sessionIDModel.getRefuseData());
        put("callstatus", sessionIDModel.getSessionIDState() == SessionIDState.refuse ? 2 : 1);
    }

    public MaJson(ExPhoneModel exphoneModel) throws JSONException {

        put("deviceip", exphoneModel.getIPAddress());
        put("devicesip", exphoneModel.getSip());
        put("devicestatus", exphoneModel.getState().value());
        put("memo", exphoneModel.getDeviceType().toString());
    }

    public MaJson(String jsonStr) throws JSONException {
        super(jsonStr);
    }

    public String funCode() {
        try {
            final String resultCode = getJSONObject("header").getString("funcode");
            return resultCode;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String resultCode() {
        try {
            final String resultCode = getJSONObject("header").getString("resultcode");
            return resultCode;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String resultDateTime() {
        try {
            final JSONArray resultArray = getJSONArray("body");
            if (resultArray.length() > 0)
                return resultArray.getString(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }
}
