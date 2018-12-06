package com.udp.extension;

import com.bean.common.MsgCommand;
import com.bean.common.ParamsHeader;

import src.CMC;

public interface ExPhoneListener
{
    void msgCallBack(ParamsHeader paramsHeader, MsgCommand msgCommand, CMC.CMCRequestParam.Body sessionModel);
    void sessionStatus(String sessionID);
    void registrationState(String s, String s1);
    void sipCallIncomingReceived();
    void sipCallEnd();
    void sipCallState(String s, int i, String s1);
    void keepLineState(PhoneLineState state, String sip);

}
