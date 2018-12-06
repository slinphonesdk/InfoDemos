package com.udp.infodemo;

import org.json.JSONException;
import org.json.JSONObject;

public class MPatient
{
        private String wardid = "无";//34",
        private String wardname = "无";//脊柱外科一病区",
        private String deptId = "无";//34",
        private String deptName = "无";//,
        private String bedno = "无";//01",
        private String bedname = "无";//,
        private String patientid = "无";//15673212",
        private String patname = "无";//官华丽",
        private String patisex = "无";//女",
        private String patiage = "无";//57岁",
        private String nurselevel = "无";//1",
        private String dietlevel = "无";//
        private String roomname = "无";//: null,
        private String enterdate = "无";//: "2014-11-17T17:03:00",
        private String nursecolor = "无";// : "#FF3300"
        private String insurancetype = "";
        private JSONObject jsonObject = null;

    public MPatient(JSONObject patJson) {
        jsonObject = patJson;
        wardid = getValueForKey("wardid");
        wardname = getValueForKey("wardname");
        deptId = getValueForKey("deptId");
        deptName = getValueForKey("deptName");
        bedname = getValueForKey("bedname");
        bedno = getValueForKey("bedno");
        patiage = getValueForKey("patiage");
        patientid = getValueForKey("patientid");
        patisex = getValueForKey("patisex");
        patname = getValueForKey("patname");
        nurselevel = getValueForKey("nurselevel");
        dietlevel = getValueForKey("dietlevel");
        roomname = getValueForKey("roomname");
        enterdate = getValueForKey("enterdate");
        nursecolor = getValueForKey("nursecolor");
        insurancetype = getValueForKey("insurancetype");

        if (this.enterdate.contains("T")) {
            this.enterdate = this.enterdate.split("T")[0];
        }
    }

    private String getValueForKey(String key) {
        String str = "无";
        if (jsonObject != null) {

            try {
                str = jsonObject.getString(key);
                str = str.length() > 0 ? str : "无";
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return str;
    }

    public String getInsurancetype() {
        return insurancetype;
    }

    public String getEnterdate() {
        return enterdate;
    }

    public String getNursecolor() {
        return nursecolor;
    }

    public String getRoomname() {
        return roomname;
    }

    public String getBedname() {
        return bedname;
    }

    public String getBedno() {
        return bedno;
    }

    public String getDeptId() {
        return deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public String getDietlevel() {
        return dietlevel;
    }

    public String getNurselevel() {
        return nurselevel;
    }

    public String getPatiage() {
        return patiage;
    }

    public String getPatientid() {
        return patientid;
    }

    public String getPatisex() {
        return patisex;
    }

    public String getPatname() {
        return patname;
    }

    public String getWardid() {
        return wardid;
    }

    public String getWardname() {
        return wardname;
    }
}
