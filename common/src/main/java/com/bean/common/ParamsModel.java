package com.bean.common;

import android.text.TextUtils;

import com.google.protobuf.InvalidProtocolBufferException;

public class ParamsModel
{
    public static ParamsModel getParamsHeader(String response) throws InvalidProtocolBufferException {
        String firstChar = response.substring(0, 1);
        String lastChar = response.substring(response.length() - 1, response.length());
        String insideFirstChar = "[";
        String insideLastChar = "]";
        if (TextUtils.equals(firstChar, insideFirstChar) && TextUtils.equals(lastChar, insideLastChar)) {
            byte[] bytes = STBUntils.stringToByte(response);

            src.CMC.CMCRequestParam cmcRequestParam = src.CMC.CMCRequestParam.parseFrom(bytes);
            src.CMC.CMCRequestParam.Header header = cmcRequestParam.getHeader();
            src.CMC.CMCRequestParam.Body body = cmcRequestParam.getBody();
            ParamsHeader paramsHeader = new ParamsHeader(DeviceType.fromInt(header.getType()), header.getUuid(), header.getSip(), MsgCommand.fromInt(header.getCommand()));
            ParamsModel paramsModel = new ParamsModel(paramsHeader, body);
            return paramsModel;
        }
        return null;
    }

    public src.CMC.CMCRequestParam.Body body;
    public ParamsHeader paramsHeader;
    public ParamsModel(ParamsHeader paramsHeader, src.CMC.CMCRequestParam.Body body) {
        this.body = body;
        this.paramsHeader = paramsHeader;
    }
}
