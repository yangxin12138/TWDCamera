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
import com.twd.twdcamera.utils.SystemPropertiesUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


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

    private SurfaceHolder holder;
    private Camera camera;

    private FrameLayout frameLayout;
    private ImageView error_tip;
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
    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: -------启动-----");
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //初始化SurfaceView
        mSurfaceView = findViewById(R.id.cameraView);
        mSurfaceView.setBackgroundColor(Color.TRANSPARENT);
        holder = mSurfaceView.getHolder();
        holder.addCallback(this);
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
        error_tip = findViewById(R.id.error_tip);


        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                previousResolution = ReadHdmiInfo(HDMI_FILE_PATH);
                try {
                    Thread.sleep(5000); // 等待1秒
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                while (true){
                    String currentResolution = ReadHdmiInfo(HDMI_FILE_PATH);
                    Log.i(TAG, "run: 外部currentResolution:"+currentResolution+",previous:"+previousResolution);
                    if (currentResolution != null && ! currentResolution.equals(previousResolution) && ! currentResolution.equals("9") ){
                        Log.i(TAG, "run: currentResolution = "+ currentResolution + ", previousResolution = "+previousResolution);
                        previousResolution = currentResolution;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                //关闭相机服务
                                SystemPropertiesUtils.setProperties("ctl.stop","media");
                                //开启相机服务
                                SystemPropertiesUtils.setProperties("ctl.start","media");
                                try {
                                    Thread.sleep(3000);
                                }catch (Exception e){e.printStackTrace();}
                                finish();
                                /*Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);*/
                                Intent intent = getPackageManager().getLaunchIntentForPackage("com.twd.hdmi_loading");
                                if (intent != null){
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                }
                                error_tip.setVisibility(View.GONE);
                                mSurfaceView.setVisibility(View.VISIBLE);
                            }
                        });
                        Log.i(TAG, "run: 修改后的分辨率 = " + previousResolution);
                    } else if (currentResolution.equals("9")) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                error_tip.setVisibility(View.VISIBLE);
                                mSurfaceView.setVisibility(View.GONE);
                            }
                        });

                    }
                    try {
                        Thread.sleep(1000); // 等待1秒
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }).start();
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
                /*case KeyEvent.KEYCODE_ENTER:
                    //关闭相机服务
                    SystemPropertiesUtils.setProperties("ctl.stop","media");
                    //开启相机服务
                    SystemPropertiesUtils.setProperties("ctl.start","media");
                    try {
                        Thread.sleep(3000);
                    }catch (Exception e){e.printStackTrace();}
                    finish();
                    try {
                        Thread.sleep(3000);
                    }catch (Exception e){e.printStackTrace();}
                    Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    break;*/
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
            Thread.sleep(3000);
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

    private void RestartHDMI(){
        // 关闭相机服务
        SystemPropertiesUtils.setProperties("ctl.stop","media");
        Log.i(TAG, "RestartHDMI: 关闭相机服务");
        // 开启相机服务
        SystemPropertiesUtils.setProperties("ctl.start","media");
        Log.i(TAG, "RestartHDMI: 开启相机服务");

        // 重启应用程序
        Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        previousResolution = ReadHdmiInfo(HDMI_FILE_PATH);
        if (camera == null) {
            Log.i(TAG, "surfaceCreated: previousResolution = " + previousResolution);
                try {
                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    camera.setPreviewDisplay(holder);
                    Camera.Parameters parameters = camera.getParameters();
                    ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
                    Log.i(TAG, "surfaceCreated: size" + parameters.getPreviewSize().width + "," + parameters.getPreviewSize().height);


                    /*camera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] data, Camera camera) {
                            String currentResolution = ReadHdmiInfo(HDMI_FILE_PATH);
                            if (currentResolution != null && ! currentResolution.equals(previousResolution)){
                                camera.stopPreview();
                                // 关闭相机服务
                                SystemPropertiesUtils.setProperties("ctl.stop","media");
                                Log.i(TAG, "RestartHDMI: 关闭相机服务");
                                try {
                                    Thread.sleep(2000); // 延时1秒
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                // 开启相机服务
                                SystemPropertiesUtils.setProperties("ctl.start","media");
                                Log.i(TAG, "RestartHDMI: 开启相机服务");

                                // 重启应用程序
                                Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                Log.i(TAG, "onPreviewFrame: currentResolution = "+ currentResolution + ", previousResolution = "+previousResolution);
                                previousResolution = currentResolution;
                                Log.i(TAG, "onPreviewFrame: 修改后的分辨率 = " + previousResolution);
                            }
                        }
                    });*/
                    Thread.sleep(3000);
                    camera.startPreview();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }

    public void simulateKeyPress(int keyCode) {
        // 创建一个新的KeyEvent实例表示按下按键事件
        KeyEvent keyDownEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);

        // 调用dispatchKeyEvent()方法，传递按下按键事件
        dispatchKeyEvent(keyDownEvent);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder mHolder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged: 调用画面改变");

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed: 调用画面被回收");
    }
}