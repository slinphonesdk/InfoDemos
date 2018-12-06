package com.bean.common;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.bean.common.utils.STBLog;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;

import static java.lang.Thread.sleep;

public class AutoWifiManager
{

    private WifiUtils mWifiUtils;
    private Listener mListener;
    private String realSsID;
    private String pwd;
    private int times = 20;

    public void setResetTimes(int times) {
        this.times = times;
    }

    public AutoWifiManager(Context mContext, int area, int room, final Listener mListener) {
        this.mListener = mListener;

        realSsID = "SYC-"+area+"-"+room;
        realSsID = realSsID.toUpperCase();
        mWifiUtils = new WifiUtils(mContext);
        String currentSSID = mWifiUtils.getCurrentWifiInfo().getSSID().replace("\"","");
        STBLog.out("WIFI", String.format("Current SsID: %s, realSsID: %s",currentSSID, realSsID));

        if (!TextUtils.equals(currentSSID, realSsID)) {
            pwd = SSIDUtils.getSSIDPassword(area, room);
            HandlerThread thread = new HandlerThread("ConnectWifi");
            thread.start();
            mHandler = new android.os.Handler(thread.getLooper());
            mHandler.post(runnable);
        }
        else {
            mListener.connectState(true, String.format("The Wifi %s is already connected.", realSsID));
        }

    }

    public interface Listener {
        void connectState(Boolean state, String err);
        void connectEnd(String err);
    }

    private android.os.Handler mHandler;
    private boolean mRunning = true;
    private int count = 0;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            while (mRunning) {

                if (count >= times) {
                    mListener.connectEnd("连接失败，请重新设置.");
                    count = 0;
                    mRunning = false;
                    mHandler.removeCallbacks(runnable);
                    mHandler = null;
                    return;
                }
                count++;

                List<String> ssIds = mWifiUtils.getScanWifiResult();
                STBLog.out("WIFI", ssIds.toString());
                if (ssIds.contains(realSsID)) {

                    boolean bol = mWifiUtils.connectWifiTest(realSsID, pwd);
                    mListener.connectState(bol, bol ? "" : "Connect Failed. Try Again.");
                    if (bol) {
                        mRunning = false;
                        mHandler.removeCallbacks(runnable);
                        break;
                    }
                }
                else {
                    mListener.connectState(false, " UnChecked The WIFI SsID: "+ realSsID+" Continue.");
                }
                try{sleep(300);}
                catch (InterruptedException e) {e.printStackTrace();}
            }
        }
    };

}
