package com.udp.master2.model;

import java.net.InetAddress;

public class SessionIDModel
{
    public class BedBean {
        public String ward_id;
        public String room_no;
        public String bed_no;

        public BedBean(String sipNumber) {
            if (sipNumber.length() == 8) {
                ward_id = sipNumber.substring(0,2);
                room_no = sipNumber.substring(2,5);
                bed_no  = sipNumber.substring(5,8);
            }
        }
    }

    private InetAddress toIPAddress;
    private InetAddress fromIPAddress;
    private String sessionID = "";
    private String callsip;
    private String tosip;
    private String generatingDate;// 生成时间
    private String forwardDate;// 转发时间
    private String cancelDate;// 收到分机取消时间
    private String sureDate;// 收到分机确定接听时间
    private String callEndDate; // 挂断时间
    private String refuseData;//拒绝接听
    private SessionIDState sessionIDState;
    private BedBean bedBean;

    public void setCallsip(String callsip) {

        this.bedBean = new BedBean(callsip);
        this.callsip = callsip;
    }

    public InetAddress getFromIPAddress() {
        return fromIPAddress;
    }

    public void setFromIPAddress(InetAddress fromIPAddress) {
        this.fromIPAddress = fromIPAddress;
    }

    public void setToIPAddress(InetAddress toIPAddress) {
        this.toIPAddress = toIPAddress;
    }

    public void setTosip(String tosip) {
        this.tosip = tosip;
    }

    public InetAddress getToIPAddress() {
        return toIPAddress;
    }

    public String getCallsip() {
        return callsip;
    }

    public String getTosip() {
        return tosip;
    }

    public BedBean getBedBean() {
        return bedBean;
    }

    public void setRefuseData(String refuseData) {
        this.refuseData = refuseData;
    }

    public void setSessionIDState(SessionIDState sessionIDState) {
        this.sessionIDState = sessionIDState;
    }

    public void setCallEndDate(String callEndDate) {
        this.callEndDate = callEndDate;
    }

    public void setCancelDate(String cancelDate) {
        this.cancelDate = cancelDate;
    }

    public void setForwardDate(String forwardDate) {
        this.forwardDate = forwardDate;
    }

    public void setGeneratingDate(String generatingDate) {
        this.generatingDate = generatingDate;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public void setSureDate(String sureDate) {
        this.sureDate = sureDate;
    }

    public String getSessionID() {
        return sessionID;
    }

    public String getCallEndDate() {
        return callEndDate;
    }

    public String getCancelDate() {
        return cancelDate;
    }

    public String getForwardDate() {
        return forwardDate;
    }

    public String getGeneratingDate() {
        return generatingDate;
    }

    public String getSureDate() {
        return sureDate;
    }

    public String getRefuseData() {
        return refuseData;
    }

    public SessionIDState getSessionIDState() {
        return sessionIDState;
    }


    public String toString() {
        return "sessionID: "+sessionID+" sip: "+callsip + "time -> " + generatingDate + " " + sureDate + " " + callEndDate;
    }
}

