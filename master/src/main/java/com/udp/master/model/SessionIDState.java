package com.udp.master.model;

import java.util.Vector;

public class SessionIDState
{
    @SuppressWarnings("unchecked")
    private static Vector<SessionIDState> values = new Vector();
    private final int mValue;
    private final String mStringValue;
    public static final SessionIDState waitSend = new SessionIDState(90002, "Wait to send or broadCast");//等待转发
    public static final SessionIDState response = new SessionIDState(90004, "response from treatAndNurse");//已经被响应
    public static final SessionIDState accept = new SessionIDState(90011, "Will Send To Server");//即将发送给服务器，即将移除
    public static final SessionIDState refuse = new SessionIDState(90022, "Will Send To Server");//即将发送给服务器，即将移除
    public static final SessionIDState cancel = new SessionIDState(90033, "Will Send To Server");//即将发送给服务器，即将移除
    public static final SessionIDState callEnd = new SessionIDState(90055, "Will Send To Server");//即将发送给服务器，即将移除

    public final int value() {
        return this.mValue;
    }

    private SessionIDState(int value, String stringValue) {
        this.mValue = value;
        values.addElement(this);
        this.mStringValue = stringValue;
    }

    public static SessionIDState fromInt(int value) {
        for(int i = 0; i < values.size(); ++i) {
            SessionIDState state = values.elementAt(i);
            if (state.mValue == value) {
                return state;
            }
        }

        throw new RuntimeException("state not found [" + value + "]");
    }

    public String toString() {
        return this.mStringValue;
    }
}
