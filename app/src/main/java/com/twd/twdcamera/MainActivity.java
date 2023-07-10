package com.twd.twdcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.common.util.concurrent.ListenableFuture;
import com.twd.twdcamera.utils.ScreenUtils;

import java.io.IOException;
import java.util.List;



public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnFocusChangeListener , View.OnClickListener {

    private DrawerLayout drawerLayout;
    private LinearLayout LLScreen;
    private LinearLayout LLAudio;

    private TextureView mTextureView;
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private Context context = this;


    private LinearLayout content_frame;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFutures;

    private FrameLayout frameLayout;
    private boolean flag = true;
    private boolean isIndex = true;
    private ScreenUtils su;
    private String[] ScreenIndex = {
            "自动", // 16:9
            "1280*720",// 4:3
            "1920*1080"// 16:9
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //初始化SurfaceView
        mTextureView = findViewById(R.id.cameraView);
        mTextureView.setSurfaceTextureListener(surfaceTextureListener);
        //获取LinearLayout的实例
        content_frame = findViewById(R.id.content_frame);

        //TODO:初始化预览画面大小
        SharedPreferences sharedPreferences = getSharedPreferences("ScreenSizePreferences", Context.MODE_PRIVATE);
        int index = sharedPreferences.getInt("index",0);

        //获取屏幕的宽度和高度
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        su = new ScreenUtils(mTextureView,screenWidth,screenHeight);
        su.updateSize(ScreenIndex[index]);
        initView();
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            int number = Camera.getNumberOfCameras();
            if (allPermissionsGranted()){
                if ( number != 0){
                    mCamera = Camera.open(); //打开相机
                    try {
                        mCamera.setPreviewTexture(surface);
                        mCamera.startPreview();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }else { Log.i("yang","onSurfaceTextureAvailable:没有可用相机");}
            }else {
                //如果没有，则请求相机权限
                ActivityCompat.requestPermissions((Activity) context,new String[]{Manifest.permission.CAMERA},100);
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            Log.i("yang","onSurfaceTextureSizeChanged:纹理大小发生变化"+width+","+height);
            // 纹理大小发生变化时的处理
            try {
                mCamera.stopPreview();
            }catch (Exception e){
                e.printStackTrace();
            }
            try {
                //检查是否获取相机权限
                if (allPermissionsGranted()){
                    //获取相机的可用预览尺寸列表
                    Camera.Parameters parameters = mCamera.getParameters();
                    List<Camera.Size> supportedSize = parameters.getSupportedPreviewSizes();
                    Camera.Size bestSize = null;
                    float targetRatio = 16f / 9;
                    float minDiff = Float.MAX_VALUE;
                    for (Camera.Size size : supportedSize){
                        float ratio = (float) size.width / size.height;
                        if (Math.abs(ratio - targetRatio) < minDiff){
                            bestSize = size;
                            minDiff = Math.abs(ratio - targetRatio);
                        }
                    }
                    //设置预览尺寸
                    parameters.setPreviewSize(bestSize.width,bestSize.height);
                    mCamera.setParameters(parameters);
                    mCamera.startPreview();
                }else {
                    //如果没有，则请求相机权限
                    ActivityCompat.requestPermissions((Activity) context,new String[]{Manifest.permission.CAMERA},100);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            mCamera.stopPreview(); //停止预览
            mCamera.release(); //释放资源
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            // 纹理数据更新时的处理，可以在这里进行图像处理等操作
        }
    };

    /* 判断相机权限*/
    private boolean allPermissionsGranted(){
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

        frameLayout = findViewById(R.id.fragment_container_view);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN){
            switch (event.getKeyCode()){
                case KeyEvent.KEYCODE_MENU:
                    if (flag) {
                        openMenu();
                        flag = false;
                    }else {
                        closeMenu();
                        flag = true;
                    }
                    break;
                case KeyEvent.KEYCODE_BACK:
                    if (!flag){
                        closeMenu();
                        flag = true;
                        return true;
                    }
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void openMenu(){
        drawerLayout.openDrawer(GravityCompat.END);
    }

    public void closeMenu(){
        drawerLayout.closeDrawer(GravityCompat.END);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus){
            v.setBackgroundColor(getResources().getColor(R.color.gray));
        }else {
            v.setBackgroundColor(getResources().getColor(R.color.white));
        }
    }

    /* 加载菜单fragment到FrameLayout*/
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.LL_screen) {
            ScreenFragment fragment = new ScreenFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container_view,fragment);
            fragmentTransaction.commit();
        } else if (v.getId()==R.id.LL_audio) {
            AudioFragment fragment = new AudioFragment();
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container_view,fragment);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        if (allPermissionsGranted()){
            int number = Camera.getNumberOfCameras();
            if (number != 0){
                //打开相机并设置预览
//                mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                mCamera = Camera.open();
                Log.i("yang","相机可用");
                try {
                    mCamera.setPreviewDisplay(holder);
                    mCamera.startPreview();
                    Log.i("yang","startPreview");
                }catch (IOException e){
                    e.printStackTrace();
                }
            }else {
                Log.i("yang","没有可用相机设备！");
            }
        }else {
            //如果没有，则请求相机权限
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},100);
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
/*
        ViewGroup.LayoutParams  layoutParams = su.getLayoutParams();
        int viewWidth = layoutParams.width;
        int viewHeight = layoutParams.height;*/
        //处理SurfaceView尺寸变化
        if (mSurfaceHolder.getSurface() == null){
            return;
        }
        try {
            mCamera.stopPreview();
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            //检查是否获得相机权限
            if (allPermissionsGranted()){
                //获取相机的可用预览尺寸列表
                Camera.Parameters parameters = mCamera.getParameters();
                List<Camera.Size> supportedSize = parameters.getSupportedPreviewSizes();
                Camera.Size bestSize =  null;
                float targetRatio = 16f / 9;
                float minDiff = Float.MAX_VALUE;
                for (Camera.Size size : supportedSize){
                    float ratio = (float) size.width / size.height;
                    if (Math.abs(ratio - targetRatio)<minDiff){
                        bestSize = size;
                        minDiff = Math.abs(ratio-targetRatio);
                    }
                }
                //设置预览尺寸
                parameters.setPreviewSize(bestSize.width,bestSize.height);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
            }else {
                //如果没有，则请求相机权限
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},100);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        //释放相机资源
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }
}