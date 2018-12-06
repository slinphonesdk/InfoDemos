package com.udp.master;

public interface MasterListener
{
    void msgFromServerIsNeedReload();
    void msgFromServerListData(String jsonBody);
    void msgFromServerErr(String err);
}
