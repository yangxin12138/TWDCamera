package com.twd.twdcamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;

import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.view.KeyEvent;
import android.view.View;

import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity implements View.OnFocusChangeListener , View.OnClickListener {

    private DrawerLayout drawerLayout;
    private LinearLayout LLScreen;
    private LinearLayout LLAudio;

    private PreviewView previewView;
    private LinearLayout content_frame;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFutures;

    private FrameLayout frameLayout;
    private boolean flag = true;
    private boolean isIndex = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initMainView();
    }

    private void initMainView(){
        //获取previewView和LinearLayout的实例
        previewView = findViewById(R.id.cameraView);
        content_frame = findViewById(R.id.content_frame);
        //检查是否获得相机权限
        if (allPermissionsGranted()){
            //如果以获取相机权限就启动startCamera()方法
            startCamera();
        }else {
            //如果没有，则请求相机权限
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},100);
        }
    }

    private void startCamera(){

        cameraProviderFutures = ProcessCameraProvider.getInstance(this);
        cameraProviderFutures.addListener(()->{
            try {
                //获取ProcessCameraProvider实例
                ProcessCameraProvider cameraProvider = cameraProviderFutures.get();
                //设置一个Preview实例，并指定PreviewView作为预览的输出目标
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

               /* cameraProvider.unbindAll();
                //将预览与相机绑定，并传入适当的CameraSelector以选择相机
                cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,preview);*/

                /*
                * 避免无相机异常
                * */
                CameraSelector cameraSelector  = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                if (cameraProvider.hasCamera(cameraSelector)){
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this,cameraSelector,preview);
                } else {
                    Toast.makeText(this, "No back camera found", Toast.LENGTH_SHORT).show();
                }
            }catch (ExecutionException | InterruptedException | IllegalArgumentException |
                    CameraInfoUnavailableException e){
                e.printStackTrace();
            }
        },ContextCompat.getMainExecutor(this));
    }

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
}