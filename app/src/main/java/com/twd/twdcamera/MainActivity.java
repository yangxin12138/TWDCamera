package com.twd.twdcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
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

import com.twd.twdcamera.utils.ScreenUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


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
    static String previousResolution;
    static String currentResolution;
    private Handler mHandler = new Handler();

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
//        if (mThread != null) {
//            mThread.stop();
//            mThread = null;
//        }
        super.onDestroy();
    }

    /* 判断相机权限*/
    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
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

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: +++");

                openCamera();

//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.i(TAG, "run: handler openCamera");
//                        openCamera();
//                    }
//                });

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                previousResolution = ReadHdmiInfo(HDMI_FILE_PATH);
                currentResolution = ReadHdmiInfo(HDMI_FILE_PATH);
                Log.i(TAG, "run: 111 外部currentResolution:"+currentResolution+",previous:"+previousResolution);
                if (! currentResolution.equals("10") ) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.i(TAG, "run: mSurfaceView set View.VISIBLE");
                            loading_tip.setVisibility(View.GONE);
                            mSurfaceView.setVisibility(View.VISIBLE);
                        }
                    });
                }

                while (mRunningFlag){
                    String currentResolution = ReadHdmiInfo(HDMI_FILE_PATH);
                    Log.i(TAG, "run: 222 外部currentResolution:"+currentResolution+",previous:"+previousResolution);
                    if (! currentResolution.equals(previousResolution) && ! currentResolution.equals("10") ){
                        Log.i(TAG, "run: currentResolution = "+ currentResolution + ", previousResolution = "+previousResolution);
                        previousResolution = currentResolution;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //reopenCamera();
                                refreshCamera();
                            }
                        });
                        Log.i(TAG, "run: 修改后的分辨率 = " + previousResolution);
                    } else if (currentResolution.equals("10")) {
                        Log.i(TAG, "run: currentResolution = 10");
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                loading_tip.setVisibility(View.VISIBLE);
                                mSurfaceView.setVisibility(View.GONE);
                            }
                        });
                    }
                    
                    previousResolution = currentResolution;

                    try {
                        Thread.sleep(1000); // 等待1秒
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        });

        if ((mThread != null) && (mRunningFlag == false)) {
            mRunningFlag = true;
            mThread.start();
        }

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
    }

    private void openCamera() {
        if (mCamera == null) {
            Log.i(TAG, "openCamera: ");
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(1920,1080);
            mCamera.setParameters(parameters);
            ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
            Log.i(TAG, "openCamera: size = " + parameters.getPreviewSize().width + "," + parameters.getPreviewSize().height);
            mCamera.startPreview();
        }
    }

//    private void reopenCamera() {
//        Log.i(TAG, "reopenCamera: ");
//        if (mCamera != null) {
//            mCamera.stopPreview();
//            mCamera.setPreviewCallback(null);
//            mCamera.release();
//            mCamera = null;
//        }
//        if (mCamera == null) {
//
//            try {
//                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//                mCamera.setPreviewDisplay(holder);
//                Camera.Parameters parameters = mCamera.getParameters();
//                ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
//                Log.i(TAG, "surfaceCreated: size" + parameters.getPreviewSize().width + "," + parameters.getPreviewSize().height);
//
//                parameters.setPreviewSize(1920,1080);
//                mCamera.setParameters(parameters);
//                error_tip.setVisibility(View.GONE);
//                mSurfaceView.setVisibility(View.VISIBLE);
//                mCamera.startPreview();
//                if ((mThread != null) && (mRunningFlag == false)) {
//                    mRunningFlag = true;
//                    mThread.start();
//                }
//                //Thread.sleep(1000); // 等待3秒
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }


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

    private String ReadHdmiInfo(String filePath){
        try {
            //创建文件对象
            File file = new File(filePath);
            //创建文件读取对象
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String lines;
            StringBuilder content = new StringBuilder();
            //逐行读取文件内容
            while ((lines = reader.readLine()) != null){
                content.append(lines);
            }
            String hdmi_resolution = content.toString();
            reader.close();
            return hdmi_resolution;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
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
}