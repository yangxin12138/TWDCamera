package com.twd.twdcamera.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.twd.twdcamera.MainActivity;

/**
 * @Author:Yangxin
 * @Description:
 * @time: Create in 上午10:34 14/3/2025
 */
public class CameraBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "CameraBroadcastReceiver";
    public static final String ACTION_OPEN_CAMERA = "com.twd.ACTION_OPEN_CAMERA";
    public static final String ACTION_CLOSE_CAMERA = "com.twd.ACTION_CLOSE_CAMERA";

    private Handler handler;

    public CameraBroadcastReceiver(Handler handler) {
        this.handler = handler;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_OPEN_CAMERA.equals(action)){
            // 发送打开摄像头的消息给 MainActivity
            Log.d(TAG, "Received open camera broadcast");
            Message msg = handler.obtainMessage(MainActivity.MSG_OPEN_CAMERA);
            handler.sendMessage(msg);
        } else if (ACTION_CLOSE_CAMERA.equals(action)) {
            // 发送打开摄像头的消息给 MainActivity
            Log.d(TAG, "Received close camera broadcast");
            Message msg = handler.obtainMessage(MainActivity.MSG_CLOSE_CAMERA);
            handler.sendMessage(msg);
        }
    }
}
