package com.bean.common;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ParamsHeader extends JSONObject
{

    public ParamsHeader(DeviceType deviceType, String uuid, String sip, MsgCommand msgCommand) {
        this.deviceType = deviceType;
        this.uuid = uuid;
        this.sip = sip;
        this.msgCommand = msgCommand;
        this.date = SimpleDate.getCurrentDate();
    }

    public DeviceType deviceType;
    public String uuid;//: “dcb123”,
    public String sip;//: “1109”,
    public MsgCommand msgCommand;//: 100001
    public String date;

}
