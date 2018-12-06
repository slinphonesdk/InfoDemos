package com.udp.infodemo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MdJson extends JSONObject
{

    MdJson(String jsonStr) throws JSONException {
        super(jsonStr);
    }

    public String getResultCode() {

        try {
            JSONObject headerJson = getJSONObject("header");
            if (headerJson != null) {
                return headerJson.getString("resultcode");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public int getTotalNumber() {
        try {
            return getJSONObject("header").getInt("totalnum");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public JSONArray getJSONBody() {
        try {
            return getJSONArray("body");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
