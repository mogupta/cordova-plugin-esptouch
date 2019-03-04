package com.coloz.esptouch;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import java.util.List;

import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.task.__IEsptouchTask;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class esptouch extends CordovaPlugin {
    CallbackContext receivingCallbackContext = null;
    IEsptouchTask mEsptouchTask;
    private static final String TAG = "esptouch";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    public static byte[] strToByteArray(String str) {
        if (str == null) {
            return null;
        }
        byte[] byteArray = str.getBytes();
        return byteArray;
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext)
            throws JSONException {
        receivingCallbackContext = callbackContext; //modified by lianghuiyuan
        if (action.equals("start")) {
            byte[] apSsid = strToByteArray(args.getString(0));
            byte[] apBssid = strToByteArray(args.getString(1);
            byte[] apPassword = strToByteArray(args.getString(2);
            byte[] deviceCountData = strToByteArray(args.getString(3));
            byte[] broadcastData = strToByteArray(args.getString(4));
            int taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
            final Object mLock = new Object();
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    synchronized (mLock) {
                        mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword,cordova.getActivity());
                        mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
                        mEsptouchTask.setEsptouchListener(myListener);
                    }
                    List<IEsptouchResult> resultList = mEsptouchTask.executeForResults(taskResultCount);
                    IEsptouchResult firstResult = resultList.get(0);
                    if (!firstResult.isCancelled()) {
                        int count = 0;
                        final int maxDisplayCount = taskResultCount;
                        if (firstResult.isSuc()) {
                            StringBuilder sb = new StringBuilder();
                            for (IEsptouchResult resultInList : resultList) {
                                sb.append("device" + count + ",bssid=" + resultInList.getBssid() + ",InetAddress="
                                        + resultInList.getInetAddress().getHostAddress() + ".");
                                count++;
                                if (count >= maxDisplayCount) {
                                    break;
                                }
                            }
                            if (count < resultList.size()) {
                                sb.append("\nthere's " + (resultList.size() - count)
                                        + " more resultList(s) without showing\n");
                            }
                            PluginResult result = new PluginResult(PluginResult.Status.OK, "Finished: " + sb);
                            result.setKeepCallback(true); // keep callback after this call
                            receivingCallbackContext.sendPluginResult(result);
                            //receivingCallbackContext.success("finished");
                        } else {
                            PluginResult result = new PluginResult(PluginResult.Status.ERROR, "No Device Found!");
                            result.setKeepCallback(true); // keep callback after this call
                            receivingCallbackContext.sendPluginResult(result);
                        }
                    }
                }
            }//end runnable
            );
            return true;
        } else if (action.equals("stop")) {
            mEsptouchTask.interrupt();
            PluginResult result = new PluginResult(PluginResult.Status.OK, "Cancel Success");
            result.setKeepCallback(true); // keep callback after this call
            receivingCallbackContext.sendPluginResult(result);
            return true;
        } 
        else {
            callbackContext.error("can not find the function " + action);
            return false;
        }
    }

    //listener to get result
    private IEsptouchListener myListener = new IEsptouchListener() {
        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            String text = "bssid=" + result.getBssid() + ",InetAddress=" + result.getInetAddress().getHostAddress();
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, text);
            pluginResult.setKeepCallback(true); // keep callback after this call
            //receivingCallbackContext.sendPluginResult(pluginResult);    //modified by lianghuiyuan
        }
    };
}