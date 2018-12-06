package com.bean.common.rudp.model;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.bean.common.rudp.RUdpCallBack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class RUdpSessionModel
{

    public interface TimeOutCallBack {
        void requestTimeOutFlag(String flag);
    }

    private String msgFlag;
    private RUdpCallBack rUdpCallBack;
    private DatagramPacket datagramPacket;
    private HandlerThread handlerThread;
    private Handler handler;

    private Runnable timeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (rUdpCallBack != null)
                rUdpCallBack.onFailure("Time Out",new IOException("Request time out."));
            destroy();
        }
    };


    private TimeOutCallBack timeOutCallBack;
    public void addCallBack(TimeOutCallBack timeOutCallBack) {
        this.timeOutCallBack = timeOutCallBack;
    }

    public void successCallBack(String msg) {
        try {
            if (rUdpCallBack != null)
                rUdpCallBack.onResponse("Success",msg == null ? "Ok" : msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        destroy();
    }

    public RUdpSessionModel(String msgFlag, String extMsg, RUdpCallBack rUdpCallBack, InetAddress inetAddress, int port, int timeOut) {

        if (msgFlag == null) {
            this.msgFlag = String.valueOf(Long.toHexString(System.currentTimeMillis()));
        }

        if (rUdpCallBack != null)
            this.rUdpCallBack = rUdpCallBack;

        handlerThread = new HandlerThread(this.msgFlag);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handler.postDelayed(timeOutRunnable, timeOut);

        String msg = this.msgFlag+"&"+extMsg;
        byte[] bytes = msg.getBytes();

        datagramPacket = new DatagramPacket(bytes,bytes.length,inetAddress,port);
    }


    public RUdpSessionModel(String msgFlag, String extMsg, RUdpCallBack rUdpCallBack, InetAddress inetAddress, int port) {

        this(msgFlag,extMsg,rUdpCallBack,inetAddress,port,3000);
    }


    public RUdpSessionModel(String extMsg, RUdpCallBack rUdpCallBack, InetAddress inetAddress, int port) {
        this(null,extMsg, rUdpCallBack,inetAddress,port);
    }

    public void destroy() {
        handlerThread.quit();
        handler.removeCallbacks(timeOutRunnable);
        if (timeOutCallBack != null) {
            timeOutCallBack.requestTimeOutFlag(msgFlag);
        }
    }

    public String getMsgFlag() {
        return msgFlag;
    }

    public DatagramPacket getDatagramPacket() {
        return datagramPacket;
    }

    public boolean isEqual(RUdpSessionModel model) {
        return TextUtils.equals(model.msgFlag, this.msgFlag);
    }
}
