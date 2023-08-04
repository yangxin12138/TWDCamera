package com.twd.twdcamera.utils;

import android.content.Context;
import android.hardware.Camera;
import android.os.FileObserver;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MyFileObserver extends FileObserver {

    private Context context;
    private Camera camera;
    private  String HDMI_FILE_PATH;
    String DefaultResolution;

    private SurfaceHolder holder;

    private static final String TAG = "MyFileObserver";
    public MyFileObserver(String path, Camera camera, SurfaceHolder holder) {
        super(path,FileObserver.ALL_EVENTS);
        this.camera = camera;
        HDMI_FILE_PATH = path;
        this.holder = holder;
        Log.i("MyFileObserver", "MyFileObserver: 开始监听 path = "+ path);
        DefaultResolution = readResolution();
        Log.i(TAG, "MyFileObserver: 初始分辨率是："+DefaultResolution);
    }

    private void restartCamera(String resolution){
        camera.stopPreview();
        camera.setPreviewCallback(null);
        camera.release();
        Log.i(TAG, "restartCamera: 执行重新启动");

        if (camera == null){
            try {
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                camera.setPreviewDisplay(holder);
                camera.startPreview();
//                MyFileObserver myFileObserver = new MyFileObserver(HDMI_FILE_PATH,camera,holder);
//                myFileObserver.startWatching();
                Camera.Parameters parameters = camera.getParameters();
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                if(resolution == "8"){
                    parameters.setPreviewSize(1920,1080);
                } else if (resolution == "6") {
                    parameters.setPreviewSize(1280,720);
                }
                camera.setParameters(parameters);
                camera.setDisplayOrientation(0);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private String readResolution(){
        String hdmi_resolution = null;
        try {
            //创建文件对象
            File file = new File(HDMI_FILE_PATH);
            //创建文件读取对象
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String lines;
            StringBuilder content = new StringBuilder();
            //逐行读取文件内容
            while ((lines = reader.readLine()) != null){
                content.append(lines);
            }
            hdmi_resolution = content.toString();
            reader.close();
        }catch (IOException e){
            Log.i(TAG, "onEvent: 读取失败");
            e.printStackTrace();
        }

        return hdmi_resolution;
    }

    @Override
    public void onEvent(int event, @Nullable String path) {
        int ev = event & FileObserver.ALL_EVENTS;
        String hdmi_resolution = readResolution();
        Log.i(TAG, "onEvent: even"+ev);
        if (! hdmi_resolution.equals(DefaultResolution)){
            Log.i(TAG, "onEvent: 现在的分辨率是："+hdmi_resolution);
            Log.i(TAG, "onEvent: 触发修改事件");
            DefaultResolution = hdmi_resolution;
            restartCamera(hdmi_resolution);
        }
    }
}
