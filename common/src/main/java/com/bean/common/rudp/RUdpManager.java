package com.bean.common.rudp;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.bean.common.rudp.model.RUdpSessionModel;
import com.bean.common.utils.STBLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.lang.Thread.sleep;

/***
 * UDP 通讯类
 */
public class RUdpManager
{

    public static class ServiceType {
        public static int client = 0;
        public static int server = 1;
    }

    private String TAG = "RUdpManager";

    // 消息列表
    private Map<String,RUdpSessionModel> sessionMap = Collections.synchronizedMap(new HashMap<String, RUdpSessionModel>());

    private DatagramSocket sendSocket;
    private DatagramSocket receiveSocket;
    private DatagramSocket responseSocket;
    private RUdpSessionModel.TimeOutCallBack timeOutCallBack;
    private byte[] receiverData = new byte[1024];
    private byte[] responseData = new byte[1024];
    private static int responsePort = 60989;//客户接收响应端口
    private static int receivePort = 60123;//客户端接收端口

    private static int serverResponsePort = 50989;//服务端响应端口
    private static int serverReceivePort = 50123;//服务端接收端口

    private int remoteReceivePort = receivePort;//默认接收端口
    private int remoteResponsePort = 0;//默认响应端口

    private ArrayList<HandlerThread> handlerThreadList = new ArrayList<>();
    private ArrayList<Handler> handlerArrayList = new ArrayList<>();

    private HandlerThread sendHandlerThread = new HandlerThread("SendHandlerThread");
    private Handler sendHandler;

    private RUdpListener rUdpListener;

    public RUdpManager(int type) {

        try {
            switch (type) {
                case 0:
                    STBLog.out("Socket", "客户端绑定端口: receiveSocket.getPort(): "+receivePort+" responseSocket: "+responsePort);
                    receiveSocket = new DatagramSocket(receivePort);
                    responseSocket = new DatagramSocket(responsePort);
                    remoteReceivePort = serverReceivePort;
                    remoteResponsePort = serverResponsePort;
                    break;
                case 1:
                    STBLog.out("Socket", "服务端绑定端口: receiveSocket.getPort(): "+serverReceivePort+" responseSocket: "+serverResponsePort);
                    receiveSocket = new DatagramSocket(serverReceivePort);
                    responseSocket = new DatagramSocket(serverResponsePort);
                    remoteReceivePort = receivePort;
                    remoteResponsePort = responsePort;
                    break;
            }

            STBLog.out("Socket","远程端口 "+remoteReceivePort+" :"+ remoteResponsePort);


            sendSocket = new DatagramSocket(0);
            threadStart();

        } catch (SocketException e) {
            e.printStackTrace();
            STBLog.out("Socket","Socket 初始化异常");
        }

        timeOutCallBack = new RUdpSessionModel.TimeOutCallBack() {
            @Override
            public void requestTimeOutFlag(String flag) {
                sessionMap.remove(flag);
            }
        };

    }

    private boolean isRunning = true;

    /***
     * ThreadStart
     */
    private void threadStart() {

        // TODO: 接收响应
        Runnable responseRunnable = new Runnable() {
            @Override
            public void run() {

                STBLog.out(TAG,"响应接收 ing");
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);
                while (isRunning) {

                    try {
                        responseSocket.receive(responsePacket);
                    } catch (IOException receive) {
                        receive.printStackTrace();
                    }

                    String result = new String(responsePacket.getData(), responsePacket.getOffset(), responsePacket.getLength());
                    STBLog.out(TAG, result);
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

                    STBLog.out(TAG, "value: " + value);
                    STBLog.out(TAG, "sessionMap Size: " + sessionMap.size());

                    RUdpSessionModel rUdpSessionModel = sessionMap.get(key);
                    rUdpSessionModel.successCallBack(value);
                sessionMap.remove(key);
                STBLog.out(TAG, "msg: " + sessionMap.size());
            }
            }
        };

        // TODO: 普通接收数据
        Runnable receiveRunnable = new Runnable() {
            @Override
            public void run() {

                String lastCallBackKey = "";
                DatagramPacket receivePacket = new DatagramPacket(receiverData, receiverData.length);
                STBLog.out(TAG,"普通接收 ing");

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

                        STBLog.out(TAG, "key: "+key+" value:"+value);
                        if (!TextUtils.equals(lastCallBackKey, key)) {
                            lastCallBackKey = key;
                            if (rUdpListener != null)
                                rUdpListener.dataResponse(key,value,receivePacket.getAddress());
                            else
                                sendResponseMsg(key,receivePacket.getAddress());
                        }
                    }
                    else {

                        String firstChar = result.substring(0,1);
                        String responseChar = "0";
                        String resultMsg = "";
                        String flagMsg = "";
                        String needRefreshList = "01|1";

                        if (result.length() > 17) {
                            flagMsg = result.substring(1, 17);
                            resultMsg = result.substring(17,result.length());
                        }

                        if (TextUtils.equals(firstChar,responseChar)) {
                            byte[] bytes = flagMsg.getBytes();
                            DatagramPacket datagramPacket = new DatagramPacket(bytes,bytes.length,receivePacket.getAddress(),receivePacket.getPort());
                            try {
                                sendSocket.send(datagramPacket);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        if (TextUtils.equals(resultMsg,needRefreshList)) {
                            if (rUdpListener != null)
                                rUdpListener.dataResponse("refresh","01",receivePacket.getAddress());
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

    /***
     * 发送响应
     * @param msgFlag
     * @param inetAddress
     */
    public void sendResponseMsg(String msgFlag, InetAddress inetAddress) {
        this.sendResponseMsg(msgFlag, null, inetAddress);
    }

    /***
     * 发送响应
     * @param msgFlag
     * @param extMsg
     * @param inetAddress
     */
    public void sendResponseMsg(String msgFlag, String extMsg, InetAddress inetAddress) {

        String sendMsg = msgFlag;
        if (extMsg != null || !TextUtils.equals("",extMsg)) {
            sendMsg = msgFlag + "&" + extMsg;
        }
        byte[] bytes = sendMsg.getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(bytes,bytes.length,inetAddress,remoteResponsePort);
        sendHandler.post(new SendRunnable(datagramPacket));
    }

    /***
     * 发送
      * @param msg
     * @param inetAddress
     * @param rUdpCallBack
     */
    public void sendMsg(String msg, InetAddress inetAddress, RUdpCallBack rUdpCallBack) {

        sendWith(msg,inetAddress,remoteReceivePort,rUdpCallBack);
    }

    /***
     * 发送 -> 服务端
      * @param msg
     * @param inetAddress
     * @param rUdpCallBack
     */
    public void sendMsgToServer(String msg, InetAddress inetAddress, RUdpCallBack rUdpCallBack) {

        sendWith(msg,inetAddress,serverReceivePort,rUdpCallBack);
    }

    /***
     * 发送 -> 服务端 (自定义超时时间)
     * @param msg
     * @param inetAddress
     * @param rUdpCallBack
     * @param timeOut
     */
    public void sendMsgToServer(String msg, InetAddress inetAddress, RUdpCallBack rUdpCallBack, int timeOut) {

        STBLog.out(TAG,inetAddress.getHostAddress());

        RUdpSessionModel rUdpSessionModel = new RUdpSessionModel(null, msg, rUdpCallBack, inetAddress, serverReceivePort, timeOut);
        sendHandler.post(new SendRunnable(rUdpSessionModel.getDatagramPacket()));

        sessionMap.put(rUdpSessionModel.getMsgFlag(), rUdpSessionModel);
        rUdpSessionModel.addCallBack(timeOutCallBack);
    }


    /***
     * 发送 -> 客户端
      * @param msg
     * @param inetAddress
     * @param rUdpCallBack
     */
    public void sendMsgToClient(String msg, InetAddress inetAddress, RUdpCallBack rUdpCallBack) {

        sendWith(msg,inetAddress,receivePort,rUdpCallBack);
    }

    private synchronized void sendWith(String msg, InetAddress inetAddress,int port, RUdpCallBack rUdpCallBack) {

        STBLog.out(TAG,inetAddress.getHostAddress());

        RUdpSessionModel rUdpSessionModel = postModelWith(msg, inetAddress, port, rUdpCallBack);
        sendHandler.post(new SendRunnable(rUdpSessionModel.getDatagramPacket()));

        sessionMap.put(rUdpSessionModel.getMsgFlag(), rUdpSessionModel);
        rUdpSessionModel.addCallBack(timeOutCallBack);
    }

    private RUdpSessionModel postModelWith(String msg, InetAddress inetAddress,int port, RUdpCallBack rUdpCallBack) {
        RUdpSessionModel rUdpSessionModel = new RUdpSessionModel(msg, rUdpCallBack, inetAddress, port);
        return rUdpSessionModel;
    }

    /***
     * 发送心跳信息 -> 服务端
     * @param msg
     * @param inetAddress
     */
    public void sendHeartMsg(String msg, InetAddress inetAddress) {

        byte[] bytes = ("Heart&"+msg).getBytes();
        DatagramPacket datagramPacket = new DatagramPacket(bytes,bytes.length,inetAddress,serverResponsePort);
        sendHandler.post(new SendRunnable(datagramPacket, true));
    }

    private class SendRunnable implements Runnable {

        private DatagramPacket datagramPacket;
        private int sendCount = 3;

        SendRunnable(DatagramPacket datagramPacket) {
            this.datagramPacket = datagramPacket;
        }

        /***
         * @param datagramPacket
         * @param isSendOneTimes
         */
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

//            try {
//                sendSocket.send(this.datagramPacket);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            sendHandler.removeCallbacks(this);
        }
    }

    public void destroy() {

        isRunning = false;

        responseSocket.close();
        responseSocket = null;

        sendSocket.close();
        sendSocket = null;

        receiveSocket.close();
        receiveSocket = null;

        if (sessionMap.size() > 0) {
            Collection<RUdpSessionModel> sessionModelCollections = sessionMap.values();
            Iterator<RUdpSessionModel> iterator = sessionModelCollections.iterator();
            while (iterator.hasNext()) {
                iterator.next().destroy();
            }
        }

        sessionMap.clear();

        for (int i = 0; i < handlerThreadList.size(); i++) {
            handlerThreadList.get(i).quit();
        }

        for (int i = 0; i < handlerArrayList.size(); i++) {
            handlerArrayList.get(i).removeCallbacks(null);
        }
    }


}
