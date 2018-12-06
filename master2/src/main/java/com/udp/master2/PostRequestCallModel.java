package com.udp.master2;

public class PostRequestCallModel
{
    private int code;
    private String success;
    private String msg;
    private String data;

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "code:"+code+"\nsuccess:"+success+"\nmsg:"+msg+"\ndata:"+data;
    }
}
