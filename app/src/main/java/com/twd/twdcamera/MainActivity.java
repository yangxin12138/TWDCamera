package com.twd.twdcamera;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
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
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
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
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener, View.OnFocusChangeListener, View.OnClickListener {
    private static final String TAG = "MAIN_ACTIVITY";

    private DrawerLayout drawerLayout;
    private LinearLayout LLScreen;
    private LinearLayout LLAudio;

    private TextureView mTextureView;
    private Context context = this;


    private LinearLayout content_frame;

    private CaptureRequest.Builder builder;

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
        mTextureView.setSurfaceTextureListener(this);
        //获取LinearLayout的实例
        content_frame = findViewById(R.id.content_frame);

        //TODO:初始化预览画面大小
        SharedPreferences sharedPreferences = getSharedPreferences("ScreenSizePreferences", Context.MODE_PRIVATE);
        int index = sharedPreferences.getInt("index", 0);

        //获取屏幕的宽度和高度
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        su = new ScreenUtils(mTextureView, screenWidth, screenHeight);
        su.updateSize(ScreenIndex[index]);
        initView();
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

        frameLayout = findViewById(R.id.fragment_container_view);
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
        drawerLayout.openDrawer(GravityCompat.END);
    }

    public void closeMenu() {
        drawerLayout.closeDrawer(GravityCompat.END);
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            v.setBackgroundColor(getResources().getColor(R.color.gray));
        } else {
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
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            manager.openCamera("0", new CameraDevice.StateCallback() {

                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    try {
                        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                        surfaceTexture.setDefaultBufferSize(1920, 1080);
                        Surface surface = new Surface(surfaceTexture);
                        builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.addTarget(surface);

                        camera.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                try {
                                    session.setRepeatingRequest(builder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {

                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }
            }, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

    }
}