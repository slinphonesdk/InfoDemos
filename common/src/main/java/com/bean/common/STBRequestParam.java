package com.bean.common;


import com.bean.common.utils.STBLog;

import java.util.ArrayList;
import java.util.Arrays;

import src.CMC;

public class STBRequestParam
{
    public final static String msgConfig(int command, String sipNumber, int deviceType, String ext) {
        return msgConfig(command, sipNumber, deviceType, ext, null, null);
    }

    public final static String rMsgConfig(int command, String sipNumber, int deviceType, String ext, String sessionID, CMC.CMCRequestParam.Body.Builder bodyBuilder){

        STBLog.out("CMC", "~ : "+command+" "+sipNumber+" "+deviceType+" "+ext);

        src.CMC.CMCRequestParam.Builder cmcRequestParamBuilder = src.CMC.CMCRequestParam.newBuilder();
        src.CMC.CMCRequestParam.Header cmcHeader = src.CMC.CMCRequestParam.Header.newBuilder()
                .setCommand(command)
                .setSip(sipNumber)
                .setType(deviceType)
                .setUuid(ext)
                .build();
        if (sessionID != null) {
            bodyBuilder.setSessionID(sessionID);
        }
        src.CMC.CMCRequestParam params = cmcRequestParamBuilder
                .setHeader(cmcHeader) // 添加头
                .setBody(bodyBuilder)// 添加体
                .build();

        byte[] paramsByte = params.toByteArray();
        final String msg = Arrays.toString(paramsByte).replace(" ", "");// 去除空格
        return msg;
    }


    public final static String msgConfig(int command, String sipNumber, int deviceType, String ext, String sessionID, ArrayList<String> lists) {
        STBLog.out("CMC", "~ : "+command+" "+sipNumber+" "+deviceType+" "+ext);
        if (sessionID != null) STBLog.out("CMC", "static msgConfig: sessionID:"+sessionID);
        if (lists != null) STBLog.out("CMC", "static msgConfig: lists:"+lists.toString());

        src.CMC.CMCRequestParam.Builder cmcRequestParamBuilder = src.CMC.CMCRequestParam.newBuilder();
        src.CMC.CMCRequestParam.Header cmcHeader = src.CMC.CMCRequestParam.Header.newBuilder()
                .setCommand(command)
                .setSip(sipNumber)
                .setType(deviceType)
                .setUuid(ext)
                .build();

        CMC.CMCRequestParam.Body.Builder cmcBodyBuilder = CMC.CMCRequestParam.Body.newBuilder();
        if (sessionID != null) {
            cmcBodyBuilder.setSessionID(sessionID);
        }
        if (lists != null) {
            if (lists.size() > 0) {
                for (String str: lists) {
                    cmcBodyBuilder.addLists(str);
                }
            }
        }

        src.CMC.CMCRequestParam params = cmcRequestParamBuilder
                .setHeader(cmcHeader) // 添加头
                .setBody(cmcBodyBuilder)// 添加体
                .build();

        byte[] paramsByte = params.toByteArray();
        final String msg = Arrays.toString(paramsByte).replace(" ", "");// 去除空格
        return msg;
    }
}
