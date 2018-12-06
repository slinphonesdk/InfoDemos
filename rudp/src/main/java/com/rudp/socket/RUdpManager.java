package com.rudp.socket;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.rudp.socket.model.RUdpSessionModel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class RUdpManager
{

    private String TAG = "RUdpManager";

    // 消息列表
    private Map<String,RUdpSessionModel> sessionMap = Collections.synchronizedMap(new HashMap<String, RUdpSessionModel>());

    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;
    private DatagramSocket responseSocket;
    private RUdpSessionModel.TimeOutCallBack timeOutCallBack;
    private byte[] receiverData = new byte[1024];
    private byte[] responseData = new byte[1024];
    private static int responsePort = 60989;//接收响应端口
    private static int receivePort = 60123;
    private ArrayList<HandlerThread> handlerThreadList = new ArrayList<>();
    private ArrayList<Handler> handlerArrayList = new ArrayList<>();

    private HandlerThread sendHandlerThread = new HandlerThread("SendHandlerThread");
    private Handler sendHandler;

    private RUdpListener rUdpListener;

    public RUdpManager(int receivePort) {

        this.receivePort = receivePort;
        try {
            receiveSocket = new DatagramSocket(receivePort);
            responseSocket = new DatagramSocket(responsePort);
            sendSocket = new DatagramSocket(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        timeOutCallBack = new RUdpSessionModel.TimeOutCallBack() {
            @Override
            public void requestTimeOutFlag(String flag) {
                sessionMap.remove(flag);
            }
        };

        threadStart();
    }

    public RUdpManager() {
        this(receivePort);
    }

    private boolean isRunning = true;
    // TODO: ThreadStart
    private void threadStart() {

        // TODO: 接收响应
        Runnable responseRunnable = new Runnable() {
            @Override
            public void run() {

                Log.e(TAG,"响应接收 ing");
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
                while (isRunning) {

                    try {
                        responseSocket.receive(responsePacket);
                    } catch (IOException receive) {
                        receive.printStackTrace();
                    }

                    String result = new String(responsePacket.getData(), responsePacket.getOffset(), responsePacket.getLength());
                    Log.e(TAG, result);
                    if (result.contains("&")) {

                        String[] split = result.split("&");
                        if (split.length == 2) {
                            String key = split[0];
                            String value = split[1];
                            if (TextUtils.equals(key,"Heart") && rUdpListener != null) {
                                rUdpListener.dataResponse(key,value,responsePacket.getAddress());
                            } else {
                                response(key, value);
                            }
                        }
                    } else {
                        response(result, null);
                    }

                    try {
                        sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            private void response(String key, String value) {
                if (sessionMap.containsKey(key)) {

                    Log.e(TAG, "value: " + value);
                    Log.e(TAG, "sessionMap Size: " + sessionMap.size());

                    RUdpSessionModel rUdpSessionModel = sessionMap.get(key);
                    rUdpSessionModel.successCallBack(value);
                    sessionMap.remove(key);
                    Log.e(TAG, "msg: " + sessionMap.size());
                }
            }
        };

        // TODO: 普通接收数据
        Runnable receiveRunnable = new Runnable() {
            @Override
            public void run() {

                String lastCallBackKey = null;
                DatagramPacket receivePacket = new DatagramPacket(receiverData, receiverData.length);
                Log.e(TAG,"普通接收 ing");

                while (isRunning) {

                    try {
                        receiveSocket.receive(receivePacket);
                    } catch (IOException receive) {
                        receive.printStackTrace();
                    }

                    //接收到的byte[]
                    String result = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());

                    String[] split = result.split("&");

                    if (split.length == 2) {
                        String key = split[0];
                        String value = split[1];

                        Log.e(TAG, "key: "+key+" value:"+value);
                        if (!TextUtils.equals(lastCallBackKey, key)) {
                            lastCallBackKey = key;
                            if (rUdpListener != null)
                                rUdpListener.dataResponse(key,value,receivePacket.getAddress());
                            else
                                sendResponseMsg(key,receivePacket.getAddress());
                        }
                    }

                    try {
                        sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        HandlerThread receiveHandlerThread = new HandlerThread("ReceiveHandlerThread");
        handlerThreadList.add(receiveHandlerThread);
        receiveHandlerThread.start();
        Handler receiveHandler = new Handler(receiveHandlerThread.getLooper());
        handlerArrayList.add(receiveHandler);
        receiveHandler.post(receiveRunnable);

        sendHandlerThread.start();
        handlerThreadList.add(sendHandlerThread);
        sendHandler = new Handler(sendHandlerThread.getLooper());
        handlerArrayList.add(sendHandler);

        HandlerThread responseHandlerThread = new HandlerThread("ResponseHandlerThread");
        handlerThreadList.add(responseHandlerThread);
        responseHandlerThread.start();
        Handler responseHandler = new Handler(responseHandlerThread.getLooper());
        handlerArrayList.add(responseHandler);
        responseHandler.post(responseRunnable);

    }

    // TODO: 普通数据接收监听
    public void addListener(RUdpListener rUdpListener) {
        this.rUdpListener = rUdpListener;
    }

    // TODO: 发送响应
    public void sendResponseMsg(String msgFlag, InetAddress inetAddress) {
        this.sendResponseMsg(msgFlag, null, inetAddress);
    }

    // TODO: 发送响应
    public void sendResponseMsg(String msgFlag, String extMsg, InetAddress inetAddress) {

        String sendMsg = msgFlag;
        if (extMsg != null || !TextUtils.equals(extMsg,"")) {
            sendMsg = msgFlag + "&" + extMsg;
        }
        byte[] bytes = sendMsg.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(bytes,bytes.length,inetAddress,responsePort);
        sendHandler.post(new SendRunnable(datagramPacket));
    }

    // TODO: 发送
    public void sendMsg(String msg, InetAddress inetAddress, RUdpCallBack rUdpCallBack) {

        Log.e(TAG,inetAddress.getHostAddress());
        // 1.add msg bind response call back
        RUdpSessionModel rUdpSessionModel = new RUdpSessionModel(msg, rUdpCallBack, inetAddress, receivePort);
        sendHandler.post(new SendRunnable(rUdpSessionModel.getDatagramPacket()));

        sessionMap.put(rUdpSessionModel.getMsgFlag(), rUdpSessionModel);
        rUdpSessionModel.addCallBack(timeOutCallBack);
    }

    // TODO: 发送心跳
    public void sendHeartMsg(String msg, InetAddress inetAddress) {

        byte[] bytes = ("Heart&"+msg).getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(bytes,bytes.length,inetAddress,responsePort);
        sendHandler.post(new SendRunnable(datagramPacket, true));
    }

    private class SendRunnable implements Runnable {

        private DatagramPacket datagramPacket;
        private int sendCount = 3;

        SendRunnable(DatagramPacket datagramPacket) {
            this.datagramPacket = datagramPacket;
        }

        SendRunnable(DatagramPacket datagramPacket, boolean isSendOneTimes) {
            this.datagramPacket = datagramPacket;
            if (isSendOneTimes) {
                sendCount = 1;
            }
        }

        @Override
        public void run() {
            for (int i = 0; i < sendCount; i++) {

                try {
                    sendSocket.send(this.datagramPacket);
                    sleep(300);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // TODO: 销毁
    public void destroy() {

        isRunning = false;

        responseSocket.close();
        responseSocket = null;

        sendSocket.close();
        sendSocket = null;

        receiveSocket.close();
        receiveSocket = null;

        if (sessionMap.size() > 0) {
            for (RUdpSessionModel rUdpSessionModel : sessionMap.values()) {
                rUdpSessionModel.destroy();
            }
        }
        sessionMap.clear();

        for (HandlerThread handlerThread: handlerThreadList) {
            handlerThread.quit();
        }

        for (Handler handler:handlerArrayList) {
            handler.removeCallbacks(null);
        }
    }


}
