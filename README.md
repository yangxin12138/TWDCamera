# TWDCamera


## 主界面
### 根节点：
###     implementation 'androidx.drawerlayout:drawerlayout:1.1.1'
### 相机调用：Camera2 API
···  MainActivity.java

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化SurfaceView
        mTextureView = findViewById(R.id.cameraView);
        mTextureView.setSurfaceTextureListener(this);
        //...
    }
    /* ... */
      @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
        Log.i("yang","this-onSurfaceTextureAvailable");
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            String[] cameraIds = manager.getCameraIdList();
            if (cameraIds.length > 0){
                Log.i("yang","this-203");
                if (manager.getCameraCharacteristics(cameraIds[0]) != null){
                    manager.openCamera("0", new CameraDevice.StateCallback() {

                        @Override
                        public void onOpened(@NonNull CameraDevice camera) {
                            try {
                                iv_error.setVisibility(View.GONE);
                                mTextureView.setVisibility(View.VISIBLE);
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
                            Log.i("yang","this-onDisconnected");
                        }

                        @Override
                        public void onError(@NonNull CameraDevice camera, int error) {
                            Log.i("yang","this-onError");
                        }
                    }, null);
                }
            }else {
                Log.i("yang","this-252");
                iv_error.setVisibility(View.VISIBLE);
                mTextureView.setVisibility(View.GONE);
            }

        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }
··· 


## 侧边菜单
### 侧边菜单使用FrameLayout来加载fragment显示菜单
