package com.udp.master.model;

import android.text.TextUtils;

import com.bean.common.DeviceType;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;

public class ExPhoneModel
{
    private String sip;
    private DeviceType deviceType;
    private String IPAddress;
    private State state;
    private long theLastHeartTime;
    private int offLineTimes = 0;

    public InetAddress IPAddress()
    {
        try
        {
            InetAddress localInetAddress = InetAddress.getByName(getIPAddress());
            return localInetAddress;
        }
        catch (UnknownHostException localUnknownHostException)
        {
            localUnknownHostException.printStackTrace();
        }
        return null;
    }

    public JSONObject jsonObject()
            throws JSONException
    {
        JSONObject localJSONObject = new JSONObject();
        localJSONObject.put("sip", this.sip);
        localJSONObject.put("deviceType", this.deviceType.value());
        localJSONObject.put("IPAddress", this.IPAddress);
        localJSONObject.put("state", this.state.value());
        localJSONObject.put("theLastHeartTime", this.theLastHeartTime);
        localJSONObject.put("offLineTime", this.offLineTimes);
        return localJSONObject;
    }

    // TODO: 类型正确返回true
    public boolean isPass() {

        if (getSip().length() != 7) return false;

        String bed = getSip().substring(4,7);
        String room = getSip().substring(2,4);
        String area = getSip().substring(0,2);
        if (deviceType == DeviceType.bedHeader) {
            return Integer.valueOf(bed) != 0 && Integer.valueOf(room) != 0 && Integer.valueOf(area) != 0;
        }
        else if (deviceType == DeviceType.doorSide) {
            return Integer.valueOf(room) != 0 && Integer.valueOf(bed) == 0 && Integer.valueOf(area) != 0;
        }
        else if (deviceType == DeviceType.treatAndNurse) {
            return Integer.valueOf(room) == 0 && Integer.valueOf(bed) == 0 && Integer.valueOf(area) != 0;
        }
        return false;
    }

    public void setState(State state) {
        this.state = state;
    }

    public void setOffLineTimes(int offLineTimes) {
        this.offLineTimes = offLineTimes;
    }

    public int getOffLineTimes() {
        return offLineTimes;
    }

    public void setTheLastHeartTime() {
        this.theLastHeartTime = System.currentTimeMillis();
    }

    public void setTheLastHeartTime(long theLastHeartTime) {
        this.theLastHeartTime = theLastHeartTime;
    }

    public long getTheLastHeartTime() {
        return theLastHeartTime;
    }

    public ExPhoneModel(String sip, DeviceType deviceType, String IPAddress, State state) {
        this.sip = sip;
        this.deviceType = deviceType;
        this.IPAddress = IPAddress;
        this.state = state;
        theLastHeartTime = System.currentTimeMillis();
    }

    public String getSip() {
        return sip;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public State getState() {
        return state;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public Boolean isEqual(ExPhoneModel exPhoneModel) {
        return TextUtils.equals(exPhoneModel.IPAddress, this.IPAddress);
    }

    public static class State {
        @SuppressWarnings("unchecked")
        private static Vector<State> values = new Vector();
        private final int mValue;
        private final String mStringValue;
        public static final State online = new State(1, "On-line");
        public static final State offline = new State(0, "Off-line");
        public final int value() {
            return this.mValue;
        }

        private State(int value, String stringValue) {
            this.mValue = value;
            values.addElement(this);
            this.mStringValue = stringValue;
        }

        public static State fromInt(int value) {
            for(int i = 0; i < values.size(); ++i) {
                State state = values.elementAt(i);
                if (state.mValue == value) {
                    return state;
                }
            }

            throw new RuntimeException("type not found [" + value + "]");
        }

        public String toString() {
            return this.mStringValue;
        }
    }
}
