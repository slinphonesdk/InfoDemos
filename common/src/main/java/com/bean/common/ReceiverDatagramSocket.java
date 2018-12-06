
package com.bean.common;

import android.text.TextUtils;
import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

import src.CMC;

import static java.lang.Thread.sleep;

public class ReceiverDatagramSocket extends DatagramSocket implements Runnable
{

    private byte receiverData[] = new byte[1024];

    private ReceiverDatagramSocket(SocketAddress bindAddress) throws SocketException {
        super(bindAddress);
        setReuseAddress(true);
        new Thread(this).start();
    }

    public ReceiverDatagramSocket(final int port) throws SocketException {
        this(new InetSocketAddress(port));
    }

    public void clear() {
        mRunning = false;
        disconnect();
        close();
        mListener = null;
    }

    private ReceiverDatagramSocketListener mListener = null;
    public void addListener(ReceiverDatagramSocketListener listener) {
        this.mListener = listener;
    }

    private boolean mRunning = true;
    @Override
    public void run() {
        DatagramPacket receiverPacket = new DatagramPacket(receiverData, receiverData.length);
        Log.e("ppt", "receive thread run: \n ------------- \n");

        while (mRunning) {

            try {
                receive(receiverPacket);
            } catch (IOException receive) {
                receive.printStackTrace();
            }

            //接收到的byte[]
            String result = new String(receiverPacket.getData(), receiverPacket.getOffset(), receiverPacket.getLength());
            Log.e("CMC", result);

            if (mListener != null) {

                String firstChar = result.substring(0,1);
                String lastChar = result.substring(result.length()-1, result.length());
                String insideFirstChar = "[";
                String insideLastChar = "]";

                if (TextUtils.equals(firstChar, insideFirstChar) && TextUtils.equals(lastChar, insideLastChar)) {
                    byte[] bytes = STBUntils.stringToByte(result);

                    try {
                        CMC.CMCRequestParam cmcRequestParam = CMC.CMCRequestParam.parseFrom(bytes);
                        CMC.CMCRequestParam.Header header = cmcRequestParam.getHeader();
                        CMC.CMCRequestParam.Body body = cmcRequestParam.getBody();
                        ParamsHeader paramsHeader = new ParamsHeader(DeviceType.fromInt(header.getType()), header.getUuid(), header.getSip(), MsgCommand.fromInt(header.getCommand()));
                        mListener.msgCallBack(paramsHeader, body, receiverPacket.getAddress());
                    }
                    catch (InvalidProtocolBufferException e) { e.printStackTrace(); }
                }
                else {
                     mListener.msgFromServer(result,receiverPacket.getAddress(),receiverPacket.getPort());
                }
            }

            try { sleep(30); }
            catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
}


