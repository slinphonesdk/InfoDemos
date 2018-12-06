package com.bean.common;


import java.util.Vector;

public class FunCode
{

    public final static String host = "10.9.0.5";
    private final static int hostPort = 9600;
    private final static String acc = "/api/WiseMedical";
    public final static String urlString = "http://"+host+":"+hostPort+acc;
    public final static String urlString(String host, int port) {
        return "http://"+host+":"+port+acc;
    }

    private static Vector<FunCode> values = new Vector();
    private final int mValue;
    private final String mStringValue;
    public static final FunCode table = new FunCode(5011, "Update table.");
    public static final FunCode updateRecode = new FunCode(5012, "Update call record.");
    public static final FunCode register = new FunCode(5014, "Register to server.");
    public static final FunCode updateDateTime = new FunCode(3002, "Update Date Time from server.");
    public static final FunCode heartList = new FunCode(5017, "heart List to server.");

    public final int value() {
        return this.mValue;
    }

    private FunCode(int value, String stringValue) {
        this.mValue = value;
        values.addElement(this);
        this.mStringValue = stringValue;
    }

    public static FunCode fromInt(int value) {
        for(int i = 0; i < values.size(); ++i) {
            FunCode state = values.elementAt(i);
            if (state.mValue == value) {
                return state;
            }
        }

        throw new RuntimeException("FunCode not found [" + value + "]");
    }

    public String toString() {
        return this.mStringValue;
    }

}
