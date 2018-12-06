package com.bean.common.rudp;

import java.net.InetAddress;

public interface RUdpListener
{

    void dataResponse(String msgFlag,String result, InetAddress fromAddress);
}
