package org.linphone.telpovoip;

public interface ExPhoneListener
{
    void registrationState(String s, String s1);
    void callState(String s, int i, String s1);
}
