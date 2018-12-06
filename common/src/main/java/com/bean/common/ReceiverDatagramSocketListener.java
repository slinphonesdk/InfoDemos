package com.bean.common;


import java.net.InetAddress;

import src.CMC;

public interface ReceiverDatagramSocketListener
{
    void msgCallBack(ParamsHeader paramsHeader, CMC.CMCRequestParam.Body body, InetAddress fromAddress);
    void msgFromServer(String jsonStr, InetAddress inetAddress, int port);
}
