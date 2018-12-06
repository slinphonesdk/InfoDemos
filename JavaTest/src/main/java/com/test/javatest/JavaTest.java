package com.test.javatest;

public class JavaTest
{
    public static void main(String[] args) {

        String pwd = UuitUtils.GetSSIDPassword(9,10);
        System.out.print(pwd);

        String newLine = System.getProperty("line.separator");
        System.out.print("---"+newLine+"1");
    }

}
