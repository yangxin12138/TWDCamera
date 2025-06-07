package com.twd.twdcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;

import android.util.Log;
import android.view.KeyEvent;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;


import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.twd.twdcamera.receiver.CameraBroadcastReceiver;
import com.twd.twdcamera.utils.ScreenUtils;
import com.twd.twdcamera.utils.SystemPropertiesUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnFocusChangeListener, View.OnClickListener {
    private static final String TAG = "MAIN_ACTIVITY";

    private DrawerLayout drawerLayout;
    private LinearLayout LLScreen;
    private TextView tv_screen;
    private TextView tv_title_screen;
    private LinearLayout LLAudio;

    private SurfaceView mSurfaceView;
    private Context context = this;


    private LinearLayout content_frame;

    private SurfaceHolder mSurfaceholder;
    private Camera mCamera;

    private Thread mThread;
    private boolean mRunningFlag=false;

    private FrameLayout frameLayout;
    private ImageView loading_tip;
    private boolean flag = true;
    private boolean isIndex = true;
    private ScreenUtils su;
    private String[] ScreenIndex = {
            "自动", // 16:9
            "4:3",// 4:3
            "16:9"// 16:9
    };
    private static final String HDMI_FILE_PATH = "/sys/hdmi/resolution";
    private static final String HDMI_ACTIVITY = "persist.ty.hdmiin";
    static String previousResolution;
    static String currentResolution;
    private CameraBroadcastReceiver cameraReceiver;
    public static final int MSG_OPEN_CAMERA = 1;
    public static final int MSG_CLOSE_CAMERA = 2;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MSG_CLOSE_CAMERA:
                    closeCamera();
                    break;
                case MSG_OPEN_CAMERA:
                    reOpenCamera();
                    break;
            }
            return true;
        }
    });
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 检查Intent的来源，判断是否是从其他应用返回
        if (intent.getFlags() == Intent.FLAG_ACTIVITY_RETAIN_IN_RECENTS) {
            // 在这里可以进行重新初始化等操作，相当于重新打开应用
            Log.i(TAG, "onNewIntent: -----------检测到从其他地方返回");
            initTask();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: -------onResume-----返回");
        initTask();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: -------activity---开始");
        initTask();
    }
    
    private void initTask(){
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: -------启动-----");
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        SystemPropertiesUtils.setProperty(HDMI_ACTIVITY,"1");
        //初始化SurfaceView
        mSurfaceView = findViewById(R.id.cameraView);
        mSurfaceView.setBackgroundColor(Color.TRANSPARENT);
        mSurfaceholder = mSurfaceView.getHolder();
        mSurfaceholder.addCallback(this);
        //获取LinearLayout的实例
        content_frame = findViewById(R.id.content_frame);

        //TODO:初始化预览画面大小
        SharedPreferences sharedPreferences = getSharedPreferences("ScreenSizePreferences", Context.MODE_PRIVATE);
        int index = sharedPreferences.getInt("index", 0);

        //获取屏幕的宽度和高度
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        su = new ScreenUtils(mSurfaceView, screenWidth, screenHeight);
        su.updateSize(ScreenIndex[index]);
        initView();

        // 创建广播接收器实例，并传入 Handler
        cameraReceiver = new CameraBroadcastReceiver(mHandler);

        IntentFilter filter = new IntentFilter();
        filter.addAction(CameraBroadcastReceiver.ACTION_OPEN_CAMERA);
        filter.addAction(CameraBroadcastReceiver.ACTION_CLOSE_CAMERA);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(cameraReceiver, filter, AppCompatActivity.RECEIVER_EXPORTED);
        } else {
            registerReceiver(cameraReceiver, filter);
        }

    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: mCamera set null !!!");
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        mRunningFlag = false;
        SystemPropertiesUtils.setProperty(HDMI_ACTIVITY,"0");
        unregisterReceiver(cameraReceiver);
        super.onDestroy();
    }

    private void initView() {
        drawerLayout = findViewById(R.id.drawer_layout);
        LLScreen = findViewById(R.id.LL_screen);
        LLAudio = findViewById(R.id.LL_audio);

        LLScreen.setFocusable(true);
        LLScreen.setFocusableInTouchMode(true);

        LLAudio.setFocusable(true);
        LLAudio.setFocusableInTouchMode(true);

        LLScreen.setOnFocusChangeListener(this);
        LLAudio.setOnFocusChangeListener(this);

        LLScreen.setOnClickListener(this);
        LLAudio.setOnClickListener(this);

        tv_screen = findViewById(R.id.screen_text);
        tv_title_screen = findViewById(R.id.tv_title_screen);

        frameLayout = findViewById(R.id.fragment_container_view);
        loading_tip = findViewById(R.id.loading_tip);

        loading_tip.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.GONE);

        Handler handler = new Handler(Looper.getMainLooper());
        int status = readFile("/sys/it6616/hdmi_status");
        if (status == 1){
            SystemPropertiesUtils.setProperty(HDMI_ACTIVITY,"1");
            openCamera();
            Log.i(TAG, "run: ----初始化打开相机");
            loading_tip.setVisibility(View.GONE);
            mSurfaceView.setVisibility(View.VISIBLE);
            printAllAudioSources(getApplicationContext(),true);
        } else {
            Log.i(TAG, "run: --初始化未打开相机");
            loading_tip.setVisibility(View.VISIBLE);
            mSurfaceView.setVisibility(View.GONE);
        }

        SystemPropertiesUtils.setProperty(HDMI_ACTIVITY,"1");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        } else {
            openCamera(); // 已经有权限，直接打开相机
        }
        //openCamera();
        Log.i(TAG, "run: ----初始化打开相机");
        loading_tip.setVisibility(View.GONE);
        mSurfaceView.setVisibility(View.VISIBLE);
        printAllAudioSources(getApplicationContext(),true);

    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause: ---暂停了------");
        super.onPause();
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        mRunningFlag = false;
        printAllAudioSources(getApplicationContext(),false);
        SystemPropertiesUtils.setProperty(HDMI_ACTIVITY,"0");//hdmi是否处于前台活动，1为活动，0为未活动
    }

    private void closeCamera(){
        Log.i("CameraBroadcastReceiver","----执行closeCamera----");
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        mRunningFlag = false;
        SystemPropertiesUtils.setProperty(HDMI_ACTIVITY,"0");//hdmi是否处于前台活动，1为活动，0为未活动
        loading_tip.setVisibility(View.VISIBLE);
        mSurfaceView.setVisibility(View.GONE);
    }

    private void openCamera() {
        if (mCamera == null) {
            Log.i(TAG, "openCamera: 打印所有可用的摄像头信息");
            // 打印所有可用的摄像头信息
            printAvailableCameras();

            // 获取所有摄像头 ID
            Log.i(TAG, "openCamera: 获取所有摄像头 ID");
            List<Integer> cameraIds = getAvailableCameraIds();
            Log.i(TAG, "Available Camera IDs: " + cameraIds);

            // 获取后置摄像头 ID
            Log.i(TAG, "openCamera: 获取所有摄像头 ID");
            int backCameraId = getBackCameraId();
            if (backCameraId != -1) {
                Log.i(TAG, "Back Camera ID: " + backCameraId);
            } else {
                Log.e(TAG, "No back camera found!");
            }
            mCamera = Camera.open(0);
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
            for (Camera.Size size : supportedSizes) {
                Log.i(TAG, "Supported preview size: " + size.width + "x" + size.height);
            }
            parameters.setPreviewSize(640,480);
            mCamera.setParameters(parameters);
            Log.i(TAG, "openCamera: size = " + parameters.getPreviewSize().width + "," + parameters.getPreviewSize().height);
            mCamera.startPreview();
        }
    }

    private void printAvailableCameras() {
        int numberOfCameras = Camera.getNumberOfCameras(); // 获取摄像头数量
        Log.i(TAG, "Number of cameras: " + numberOfCameras);

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo); // 获取摄像头信息
            String facing = (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) ? "Back" : "Front";
            Log.i(TAG, "Camera ID: " + i + ", Facing: " + facing + ", Orientation: " + cameraInfo.orientation);
        }
    }

    private List<Integer> getAvailableCameraIds() {
        List<Integer> cameraIds = new ArrayList<>();
        int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; i++) {
            cameraIds.add(i);
        }

        return cameraIds;
    }

    private int getBackCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i; // 返回后置摄像头的 ID
            }
        }

        return -1; // 没有找到后置摄像头
    }

    public static void printAllAudioSources(Context context,boolean able) {
        Log.i(TAG, "printAllAudioSources: able = "+able);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setParameters(able ? "SET_LOOPBACK_TYPE=5,3" : "SET_LOOPBACK_TYPE=0");
    }
    private void refreshCamera() {
        Log.i(TAG, "refreshCamera: ");
        try {
            if (mCamera != null) {
                Log.i(TAG, "refreshCamera: camera != null");
                mCamera.stopPreview();
                Camera.Parameters parameters = mCamera.getParameters();
                ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
                Log.i(TAG, "surfaceCreated: size" + parameters.getPreviewSize().width + "," + parameters.getPreviewSize().height);

                parameters.setPreviewSize(1920, 1080);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(mSurfaceholder);
                mCamera.startPreview();
                loading_tip.setVisibility(View.GONE);
                mSurfaceView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void reOpenCamera() {
        Log.i(TAG, "reOpenCamera: ");
        try {
            if (mCamera == null) {
                Log.i(TAG, "reOpenCamera: camera == null");
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                Camera.Parameters parameters = mCamera.getParameters();
                Log.i(TAG, "surfaceCreated: size" + parameters.getPreviewSize().width + "," + parameters.getPreviewSize().height);

                parameters.setPreviewSize(1920, 1080);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(mSurfaceholder);
                mCamera.startPreview();
                Log.i(TAG, "reOpenCamera: -------SurfaceView refresh");
                loading_tip.setVisibility(View.GONE);
                mSurfaceView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MENU:
                    if (flag) {
                        openMenu();
                        flag = false;
                    } else {
                        closeMenu();
                        flag = true;
                    }
                    break;
                case KeyEvent.KEYCODE_BACK:
                    if (!flag) {
                        closeMenu();
                        flag = true;
                        return true;
                    }
                    closeCamera();
                    printAllAudioSources(getApplicationContext(),false);
                    loading_tip.setVisibility(View.VISIBLE);
                    mSurfaceView.setVisibility(View.GONE);
                    finish();
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void openMenu() {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    public void closeMenu() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            v.setBackgroundColor(getResources().getColor(R.color.gray));
            if (v.getId() == R.id.LL_screen){
                tv_title_screen.setTextColor(getResources().getColor(R.color.black));
            }
        } else {
            v.setBackgroundColor(getResources().getColor(R.color.menuBackground));
            if (v.getId() == R.id.LL_screen){
                tv_title_screen.setTextColor(getResources().getColor(R.color.gray));
            }
        }
    }

    /* 加载菜单fragment到FrameLayout*/
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.LL_screen) {
            ScreenFragment fragment = new ScreenFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container_view, fragment);
            fragmentTransaction.commit();
        } else if (v.getId() == R.id.LL_audio) {
            AudioFragment fragment = new AudioFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container_view, fragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated: ");
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder mHolder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged: 调用画面改变");
        SystemPropertiesUtils.setProperty(HDMI_ACTIVITY,"1");
        printAllAudioSources(getApplicationContext(),true);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: 调用画面被回收");
//        if (camera != null){
//            Log.i(TAG, "surfaceDestroyed: camera release");
//            camera.stopPreview();
//            camera.setPreviewCallback(null);
//            camera.release();
//            camera = null;
//        }

    }

    private int readFile(String filePath){
        File file = new File(filePath);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            if (line != null) {
                return Integer.parseInt(line.trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return -1; // 若读取失败，返回 -1
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_F10 && event.getAction() == KeyEvent.ACTION_DOWN){
            int status = readFile("/sys/it6616/hdmi_status");
            if (status == 1){
                Log.i(TAG, "onKeyDown: -------打开相机");
                SystemPropertiesUtils.setProperty(HDMI_ACTIVITY,"1");
                openCamera();
                loading_tip.setVisibility(View.GONE);
                mSurfaceView.setVisibility(View.VISIBLE);
                printAllAudioSources(getApplicationContext(),true);
            } else if (status == 0) {
                Log.i(TAG, "onKeyDown: -------关闭相机");
                closeCamera();
                printAllAudioSources(getApplicationContext(),false);
                loading_tip.setVisibility(View.VISIBLE);
                mSurfaceView.setVisibility(View.GONE);
            }else {

            }
        }
        return false;
    }
}