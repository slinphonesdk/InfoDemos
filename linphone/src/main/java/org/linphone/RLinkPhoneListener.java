package org.linphone;


public interface RLinkPhoneListener
{
    void callState(String from, int state, String s);
    void registrationState(String state, String s);

}
