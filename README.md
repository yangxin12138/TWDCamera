# TWDCamera


## 主界面
### 根节点：
###     implementation 'androidx.drawerlayout:drawerlayout:1.1.1'
### 相机调用：Camera2 API
···  MainActivity.java

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
··· 


## 侧边菜单
### 侧边菜单使用FrameLayout来加载fragment显示菜单
