package com.gudonghei.phone.phone;

import android.app.Activity;
import android.content.Context;
import android.util.Log;


import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCoreException;

public class PhoneUtils {

    private static String TAG = "PhoneBusiness";

    /**
     * phoneLogin
     * 在平台上注册该sip账号
     * sip账号：username
     * sip密码：password
     * sip网域：domain
     * sip端口：port
     */
    public static void phoneLogin(LinphoneMiniManager mManager, String username, String password, String domain, String port) {
        Log.i(TAG, "phoneLogin: "+username+"密码："+password);
        LinphoneMiniManager.setMyNumber(username);
        String sipAddress = "sip:" + username + "@" + domain;
        try {
            mManager.lilin_reg(sipAddress, password, port);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }


    /**
     * CallSomebody
     * 拨号
     * 拨打账号phone_number
     * sip网域domain
     */
    public static void CallSomebody(LinphoneMiniManager mManager,  String phone_number, String domain) {
        try {
            mManager.getmLinphoneCore().invite(phone_number + "@" + domain);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

    /**
     * useSpeaker
     * 使用扬声器
     */
    public static void useSpeaker(Context context) {
        LinphoneMiniManager mManager =PhoneGlobal.getMiniManager(context);
        mManager.getmLinphoneCore().enableSpeaker(true);
    }

    /**
     * useReceiver
     * 使用听筒
     */
    public static void useReceiver(Context context) {
        LinphoneMiniManager mManager =PhoneGlobal.getMiniManager(context);
        mManager.getmLinphoneCore().enableSpeaker(false);
    }


    /**
     * Answer
     * 接电话
     */
    public static int Answer(LinphoneMiniManager mManager) {
        LinphoneCall call = mManager.getmLinphoneCore().getCurrentCall();
        try {
            mManager.getmLinphoneCore().acceptCall(call);
            mManager.getmLinphoneCore().enableSpeaker(true);
            mManager.getmLinphoneCore().enableEchoCancellation(true);
        } catch (LinphoneCoreException exception) {
            Log.d(TAG, exception.toString());
            return -1;
        }

        return 0;
    }

    /**
     * HangUp
     * 挂机
     */
    public static void HangUp(LinphoneMiniManager mManager) {
        try {
            LinphoneCall call = mManager.getmLinphoneCore().getCurrentCall();
            mManager.getmLinphoneCore().terminateCall(call);
        } catch (Exception e) {
        }
    }

    /**
     * GetCallInfo
     * 获取电话信息
     */
    public static void GetCallInfo(LinphoneMiniManager mManager) {

    }

}
