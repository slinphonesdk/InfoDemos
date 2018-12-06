package com.bean.common;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class SenderDatagramSocket extends DatagramSocket
{
    private SenderDatagramSocket(SocketAddress bindAddress) throws SocketException {
        super(bindAddress);
    }

    private static int port = 1000;

    public SenderDatagramSocket(final int port) throws SocketException {
        this(new InetSocketAddress(port));
        SenderDatagramSocket.port = port;
        setReuseAddress(true);
    }

    public InetAddress getAddress(String IPStr) {
        InetAddress serverAddress = null;
        try {
            serverAddress = InetAddress.getByName(IPStr);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return serverAddress;
    }

    private InetAddress hostAddress() {
        final String host = "192.168.88.253";
        return getAddress(host);
    }

    private final InetAddress broadcastAddress() {
        final String host = "192.168.88.255";
        return getAddress(host);
    }

    public void clear() {
        disconnect();
        close();
    }

    public void sendMsgToHost(String msg) {
        sendMsgToClient(msg, hostAddress());
    }
    public void sendBroadcast(String msg) {
        sendMsgToClient(msg, broadcastAddress());
    }

    private DatagramPacket sendPacket;
    public synchronized void sendMsgToClient(String msg, InetAddress inetAddress) {
        this.sendMsgToClient(msg,inetAddress,this.port);
    }

    public synchronized void sendMsgToClient(String msg, InetAddress inetAddress, int port) {
        Log.e("ppt","send: ---- "+msg);

        byte data[] = msg.getBytes();

        sendPacket = new DatagramPacket(data, data.length, inetAddress, port);
        new WaitThread().start();
    }

    private class WaitThread extends Thread {
        @Override
        public void run() {
            try {
                send(sendPacket);
            } catch (IOException e) {
                Log.e("SOCKET", "SOCKET ERROR");
            }
        }
    }
}
