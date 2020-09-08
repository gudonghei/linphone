package com.gudonghei.phone.phone;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.view.View;


import com.gudonghei.phone.R;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCall.State;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCore.EcCalibratorStatus;
import org.linphone.core.LinphoneCore.GlobalState;
import org.linphone.core.LinphoneCore.RegistrationState;
import org.linphone.core.LinphoneCore.RemoteProvisioningState;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneFriendList;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.SubscriptionState;
import org.linphone.mediastream.Log;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration;
import org.linphone.mediastream.video.capture.hwconf.AndroidCameraConfiguration.AndroidCamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;


public class LinphoneMiniManager implements LinphoneCoreListener {
    private static LinphoneMiniManager mInstance;
    private Context mContext;
    private LinphoneCore mLinphoneCore;
    private Timer mTimer;
    private Handler handler = new Handler();
    private Intent broadcast;


    private PhoneListener phoneListener;



    public void setPhoneListener(PhoneListener phoneListener) {
        this.phoneListener = phoneListener;
    }

    public interface PhoneListener {
        void phoneReturn(int x, String code);
    }


    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public LinphoneCore getmLinphoneCore() {
        return mLinphoneCore;
    }

    public LinphoneMiniManager(Context c) {
        mContext = c;
        LinphoneCoreFactory.instance().setDebugMode(true, "Linphone Mini");
        try {
            String basePath = mContext.getFilesDir().getAbsolutePath();
            copyAssetsFromPackage(basePath);
            mLinphoneCore = LinphoneCoreFactory.instance().createLinphoneCore(this, basePath + "/.linphonerc", basePath + "/linphonerc", null, mContext);
            initLinphoneCoreValues(basePath);

            setUserAgent();
            setFrontCamAsDefault();
            startIterate();
            mInstance = this;
            mLinphoneCore.setNetworkReachable(true); // Let's assume it's true
        } catch (LinphoneCoreException e) {
        } catch (IOException e) {
        }
    }

    public static LinphoneMiniManager getInstance() {
        return mInstance;
    }

    public void destroy() {
        try {
            mTimer.cancel();
            mLinphoneCore.destroy();
        } catch (RuntimeException e) {
        } finally {
            mLinphoneCore = null;
            mInstance = null;
        }
    }

    private void startIterate() {
        TimerTask lTask = new TimerTask() {
            @Override
            public void run() {
                mLinphoneCore.iterate();
            }
        };

        /*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
        mTimer = new Timer("LinphoneMini scheduler");
        mTimer.schedule(lTask, 0, 20);
    }

    private void setUserAgent() {
        try {
            String versionName = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
            if (versionName == null) {
                versionName = String.valueOf(mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionCode);
            }
            mLinphoneCore.setUserAgent("LinphoneMiniAndroid", versionName);
        } catch (NameNotFoundException e) {
        }
    }

    private void setFrontCamAsDefault() {
        int camId = 0;
        AndroidCamera[] cameras = AndroidCameraConfiguration.retrieveCameras();
        for (AndroidCamera androidCamera : cameras) {
            if (androidCamera.frontFacing)
                camId = androidCamera.id;
        }
        mLinphoneCore.setVideoDevice(camId);
    }

    private void copyAssetsFromPackage(String basePath) throws IOException {
        copyIfNotExist(mContext, R.raw.oldphone_mono, basePath + "/oldphone_mono.wav");
        copyIfNotExist(mContext, R.raw.ringback, basePath + "/ringback.wav");
        copyIfNotExist(mContext, R.raw.toy_mono, basePath + "/toy_mono.wav");
        copyIfNotExist(mContext, R.raw.linphonerc_default, basePath + "/.linphonerc");
        copyFromPackage(mContext, R.raw.linphonerc_factory, new File(basePath + "/linphonerc").getName());
        copyIfNotExist(mContext, R.raw.lpconfig, basePath + "/lpconfig.xsd");
        copyIfNotExist(mContext, R.raw.rootca, basePath + "/rootca.pem");
    }
    
    
    

    private void initLinphoneCoreValues(String basePath) {
        mLinphoneCore.setContext(mContext);
        mLinphoneCore.setRing(null);
        mLinphoneCore.setRootCA(basePath + "/rootca.pem");
        mLinphoneCore.setPlayFile(basePath + "/toy_mono.wav");
        mLinphoneCore.setChatDatabasePath(basePath + "/linphone-history.db");
        int availableCores = Runtime.getRuntime().availableProcessors();
        mLinphoneCore.setCpuCount(availableCores);
    }



    public static void copyIfNotExist(Context context, int ressourceId, String target) throws IOException {
        File lFileToCopy = new File(target);
        if (!lFileToCopy.exists()) {
            copyFromPackage(context, ressourceId, lFileToCopy.getName());
        }
    }

    public static void copyFromPackage(Context context, int ressourceId, String target) throws IOException {
        FileOutputStream lOutputStream = context.openFileOutput (target, 0);
        InputStream lInputStream = context.getResources().openRawResource(ressourceId);
        int readByte;
        byte[] buff = new byte[8048];
        while (( readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff,0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }


    @Override
    public void globalState(LinphoneCore lc, GlobalState state, String message) {
        Log.d("Global state: " + state + "(" + message + ")");

        android.util.Log.i("99999999", "globalState: ");
    }


    @Override
    public void callState(LinphoneCore lc, LinphoneCall call, State cstate,
                          String message) {


        if (broadcast == null) {
            broadcast = new Intent("com.hundsun.om.Activity.RECEIVER");
        }
//        String text ="";
        if (cstate.toString().equals("OutgoingInit")) {
//            text="拨号中";
//            PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_ALERTING);
//            ChangeFloatUI(Default.CallStatus.CALL_STATE_ALERTING);
            broadcast.putExtra("info", message);
        } else if (cstate.toString().equals("IncomingReceived")) {
//            text="有电话";
//            PowerManagerUtils.creatPowerManager(mContext);
//            PowerManagerUtils.light();
//            Intent intent = new Intent(mContext, CallPageActivity.class);
//            PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_INCOMING);
//            mContext.startActivity(intent);
//            broadcast.putExtra("info", message);
//            ChangeFloatUI(Default.CallStatus.CALL_STATE_INCOMING);
//            CallPage(true);
        } else if (cstate.toString().equals("CallEnd")) {
//            text="电话被挂断了";
            if (message.equals("Call declined.")) {
//                PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_NOT_CONNECTED);
//                ChangeFloatUI(Default.CallStatus.CALL_STATE_NOT_CONNECTED);
//                PowerManagerUtils.dark();
            } else {
//                PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_DISCONNECTED);
//                ChangeFloatUI(Default.CallStatus.CALL_STATE_DISCONNECTED);
            }
            broadcast.putExtra("info", message);
//            android.util.Log.i("6465", "callState: " + message);
//            CallPage(false);

        } else if (cstate.toString().equals("Released")) {
//            text="可以继续拨打电话";
//            PowerManagerUtils.dark();
        } else if (cstate.toString().equals("Connected")) {
//            text="通话中";
//            PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_ACTIVE);
//            ChangeFloatUI(Default.CallStatus.CALL_STATE_ACTIVE);
            broadcast.putExtra("info", message);
        }

   switch (message){
       case "Call released":
           phoneListener.phoneReturn(1, "");
           phoneListener.phoneReturn(2, "待机");
           break;
       case "Starting outgoing call":
           phoneListener.phoneReturn(2, "呼叫中");
           break;
       case "Incoming call":
           phoneListener.phoneReturn(2, "来电");
           break;
       case "Call ended":
           phoneListener.phoneReturn(2, "来电结束");
           break;
       case "Streams running":
       case "Connected (streams running)":
           phoneListener.phoneReturn(2, "接听");
           break;
       case "Call terminated":
           phoneListener.phoneReturn(1, "");
           phoneListener.phoneReturn(2, "已挂断");
           break;


   }

        android.util.Log.i("99999999", "callState: " + message +
                "\n" + cstate.toString() +
                "\n" + call.toString());
    }

    @Override
    public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {
        android.util.Log.i("99999999", "authInfoRequested: ");
    }

    @Override
    public void authenticationRequested(LinphoneCore linphoneCore, LinphoneAuthInfo linphoneAuthInfo, LinphoneCore.AuthMethod authMethod) {
        android.util.Log.i("99999999", "authenticationRequested: ");
    }

    @Override
    public void callStatsUpdated(LinphoneCore lc, LinphoneCall call,
                                 LinphoneCallStats stats) {
        android.util.Log.i("99999999", "callStatsUpdated: ");
    }

    @Override
    public void callEncryptionChanged(LinphoneCore lc, LinphoneCall call,
                                      boolean encrypted, String authenticationToken) {
        android.util.Log.i("99999999", "callEncryptionChanged: ");
    }

    @Override
    public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg,
                                  RegistrationState cstate, String smessage) {
        android.util.Log.i("99999999", "registrationState: ");
        Log.i("lilin Registration state: " + cstate + "(" + smessage + ")");

        Intent broadcast = new Intent("com.hundsun.om.fragment.RECEIVER");
        String s = smessage;

        if (smessage.equals("Registration successful")) {
            s = "登入成功";
//            PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_IDLE);
//            PhoneGlobal.setMyNumber(getMyNumber());
        } else if (smessage.equals("Registration in progress")) {
            s = "正在登入";
//            PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_LOGINING);
        } else if (smessage.equals("Forbidden")) {
//            if (PhoneGlobal.getCallStatus() != Default.CallStatus.CALL_STATE_DISABLED) {
//                PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_LOGIN_FAILED);
//                ChangeFloatUI(Default.CallStatus.CALL_STATE_LOGIN_FAILED);
//                s = "登入失败";
//            } else {
//                s = "账号被禁用";
//            }
        } else if (smessage.equals("Unregistration done")) {
            s = "已注销";
//            PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_LOGOUT);
//            ChangeFloatUI(Default.CallStatus.CALL_STATE_LOGOUT);
        } else if (smessage.equals("Registration disabled")) {
            s = "账号被禁用";
//            PhoneGlobal.setCallStatus(Default.CallStatus.CALL_STATE_DISABLED);
//            ChangeFloatUI(Default.CallStatus.CALL_STATE_DISABLED);
        }
        android.util.Log.i("99999", "registrationState: " + s);
        broadcast.putExtra("login_info", s);
        phoneListener.phoneReturn(0, s);
        mContext.sendBroadcast(broadcast);
//        LocalBroadcastManager.getInstance(mContext).sendBroadcast()
    }


    @Override
    public void newSubscriptionRequest(LinphoneCore lc, LinphoneFriend lf,
                                       String url) {
        android.util.Log.i("99999999", "newSubscriptionRequest: ");
    }

    @Override
    public void notifyPresenceReceived(LinphoneCore lc, LinphoneFriend lf) {
        android.util.Log.i("99999999", "notifyPresenceReceived: ");
    }


    @Override
    public void messageReceived(LinphoneCore lc, LinphoneChatRoom cr,
                                LinphoneChatMessage message) {
        android.util.Log.i("99999999", "messageReceived: ");
        Log.d("Message received from " + cr.getPeerAddress().asString() + " : " + message.getText() + "(" + message.getExternalBodyUrl() + ")");
    }

    @Override
    public void isComposingReceived(LinphoneCore lc, LinphoneChatRoom cr) {
        Log.d("Composing received from " + cr.getPeerAddress().asString());
        android.util.Log.i("99999999", "isComposingReceived: ");
    }

    @Override
    public void dtmfReceived(LinphoneCore lc, LinphoneCall call, int dtmf) {
        android.util.Log.i("99999999", "dtmfReceived: ");
    }

    @Override
    public void ecCalibrationStatus(LinphoneCore lc, EcCalibratorStatus status,
                                    int delay_ms, Object data) {
        android.util.Log.i("99999999", "ecCalibrationStatus: ");
    }

    @Override
    public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {
        android.util.Log.i("99999999", "uploadProgressIndication: ");
    }

    @Override
    public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {
        android.util.Log.i("99999999", "uploadStateChanged: ");
    }

    @Override
    public void friendListCreated(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {
        android.util.Log.i("99999999", "friendListCreated: ");
    }

    @Override
    public void friendListRemoved(LinphoneCore linphoneCore, LinphoneFriendList linphoneFriendList) {
        android.util.Log.i("99999999", "friendListRemoved: ");
    }

    @Override
    public void notifyReceived(LinphoneCore lc, LinphoneCall call,
                               LinphoneAddress from, byte[] event) {
        android.util.Log.i("99999999", "notifyReceived: ");
    }

    @Override
    public void transferState(LinphoneCore lc, LinphoneCall call,
                              State new_call_state) {
        android.util.Log.i("99999999", "transferState: ");
    }

    @Override
    public void infoReceived(LinphoneCore lc, LinphoneCall call,
                             LinphoneInfoMessage info) {
        android.util.Log.i("99999999", "infoReceived: ");
    }

    @Override
    public void subscriptionStateChanged(LinphoneCore lc, LinphoneEvent ev,
                                         SubscriptionState state) {
        android.util.Log.i("99999999", "subscriptionStateChanged: ");
    }

    @Override
    public void notifyReceived(LinphoneCore lc, LinphoneEvent ev,
                               String eventName, LinphoneContent content) {
        android.util.Log.i("99999999", "notifyReceived: ");
        Log.d("Notify received: " + eventName + " -> " + content.getDataAsString());
    }

    @Override
    public void publishStateChanged(LinphoneCore lc, LinphoneEvent ev,
                                    PublishState state) {
        android.util.Log.i("99999999", "publishStateChanged: ");
    }

    @Override
    public void configuringStatus(LinphoneCore lc,
                                  RemoteProvisioningState state, String message) {
        android.util.Log.i("99999999", "configuringStatus: ");
        Log.d("Configuration state: " + state + "(" + message + ")");
    }

    @Override
    public void show(LinphoneCore lc) {
        android.util.Log.i("99999999", "show: ");
    }

    @Override
    public void displayStatus(LinphoneCore lc, String message) {
        android.util.Log.i("99999999", "displayStatus: " + message);
        if (!message.contains("Refreshing")){
            String x = subString(message, "sip:", "@");
            android.util.Log.i("99999999显示号码:", x);
            phoneListener.phoneReturn(1, x);
        }


        if (broadcast == null) {
            broadcast = new Intent("com.hundsun.om.Activity.RECEIVER");
        }
        if (message.trim().equals("Not Found Q.850;cause=3;text=\"NO_ROUTE_DESTINATION\"")) {
            phoneListener.phoneReturn(2, "您所拨打的电话是空号");
        } else if (message.trim().equals("User is temporarily unavailable.")) {
            phoneListener.phoneReturn(2, "对方不在线");
        } else if (message.trim().equals("Request timeout.")) {
            phoneListener.phoneReturn(2, "无法接通，稍后再拨");
        } else if (message.trim().equals("User is busy.")) {
            phoneListener.phoneReturn(2, "您拨打的电话正在通话中，请稍后再拨");
        }


    }

    public static String subString(String str, String strStart, String strEnd) {

        /* 找出指定的2个字符在 该字符串里面的 位置 */
        int strStartIndex = str.indexOf(strStart);
        int strEndIndex = str.indexOf(strEnd);

        /* 开始截取 */
        String result = str.substring(strStartIndex, strEndIndex).substring(strStart.length());
        return result;
    }

    @Override
    public void displayMessage(LinphoneCore lc, String message) {
        android.util.Log.i("99999999", "displayMessage: ");
    }

    @Override
    public void displayWarning(LinphoneCore lc, String message) {
        android.util.Log.i("99999999", "displayWarning: ");
    }

    @Override
    public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {
        android.util.Log.i("99999999", "fileTransferProgressIndication: ");
    }

    @Override
    public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {
        android.util.Log.i("99999999", "fileTransferRecv: ");
    }

    @Override
    public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {
        android.util.Log.i("99999999", "fileTransferSend: ");
        return 0;
    }

    private static String MyNumber = "";

    public String getMyNumber() {
        return MyNumber;
    }

    public static void setMyNumber(String myNumber) {
        MyNumber = myNumber;
    }

    public void lilin_reg(String sipAddress, String password, String port) throws LinphoneCoreException {

        LinphoneAddress address = LinphoneCoreFactory.instance().createLinphoneAddress(sipAddress);
        String username = address.getUserName();
        String domain = address.getDomain();

        LinphoneProxyConfig[] proxyConfigList = mLinphoneCore.getProxyConfigList();
        for (LinphoneProxyConfig linphoneProxyConfig : proxyConfigList) {
            mLinphoneCore.removeProxyConfig(linphoneProxyConfig);
        }//删除原来的

        mLinphoneCore.addAuthInfo(LinphoneCoreFactory.instance().createAuthInfo(username, password, null, domain + ":" + port));
        LinphoneCore.Transports transports = mLinphoneCore.getSignalingTransportPorts();
        transports.tcp = -1;
        transports.udp = -1;
        transports.tls = -1;
        mLinphoneCore.setSignalingTransportPorts(transports);

// create proxy config
        LinphoneProxyConfig proxyCfg = mLinphoneCore.createProxyConfig(sipAddress, domain + ":" + port, null, true);
        proxyCfg.enablePublish(true);
        proxyCfg.setExpires(120);
        mLinphoneCore.addProxyConfig(proxyCfg); // add it to linphone
        mLinphoneCore.setDefaultProxyConfig(proxyCfg);//注册一次就好了  下次启动就不用注册
    }
//    private void ChangeFloatCallNumber(String nubmer){
//        MyAppliaction.getInstance().handler.post(new Runnable() {
//            @Override
//            public void run() {
//                PhoneFloatWindow.instance(MyAppliaction.getInstance()).ChangeCallNumber(nubmer);
//            }
//        });
}
//    private void ChangeFloatUI(int x) {
//        MyAppliaction.getInstance().handler.post(new Runnable() {
//            @Override
//            public void run() {
//                PhoneFloatWindow.instance(MyAppliaction.getInstance()).ChangeUI(x);
//            }
//        });
//    }
//
//    private void CallPage(boolean isShow) {
//        if (isShow) {
//            MyAppliaction.getInstance().handler.post(new Runnable() {
//                @Override
//                public void run() {
//                    PhoneFloatWindow.instance(MyAppliaction.getInstance()).showFloatWindow();
//                }
//            });
//        } else {
//            MyAppliaction.getInstance().handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    PhoneFloatWindow.instance(MyAppliaction.getInstance()).hideFloatWindow();
//                }
//            }, 1500);
//
//        }
//    }
//}
