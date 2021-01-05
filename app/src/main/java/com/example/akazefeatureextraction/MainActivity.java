package com.example.akazefeatureextraction;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.view.GestureDetectorCompat;

import org.jetbrains.annotations.NotNull;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.AKAZE;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.FileReader;

import static org.opencv.imgproc.Imgproc.resize;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1;


    public boolean startMatching = false;
    private GestureDetectorCompat mDetector;

    //Initialization

    public Mat Currframe, Firstframe;
    public AKAZE akaze;
    public Mat desc;
    public MatOfKeyPoint kpts;
    public Scalar color;

    public int scale = 8;
    public Size scaleSize;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");

                // Load native library after(!) OpenCV initialization
//                System.loadLibrary("native-lib");

                mOpenCvCameraView.enableView();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Permissions for Android 6+
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_REQUEST
        );

        setContentView(R.layout.activity_main);
        mDetector = new GestureDetectorCompat(this, new MyGestureListener());

        mOpenCvCameraView = findViewById(R.id.main_surface);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

    }


    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(DEBUG_TAG,"onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
            startMatching = true;
            return true;
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mOpenCvCameraView.setCameraPermissionGranted();
            } else {
                String message = "Camera permission was not granted";
                Log.e(TAG, message);
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.e(TAG, "Unexpected permission request");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        Currframe.release();
        Firstframe.release();
        desc.release();
        kpts.release();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Currframe = new Mat();
        Firstframe = new Mat();
        desc = new Mat();
        kpts = new MatOfKeyPoint();
        akaze = AKAZE.create();

    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame frame) {
        // get current camera frame as OpenCV Mat object
        Currframe = frame.rgba();
        Firstframe = new Mat();
        Currframe.copyTo(Firstframe);

        scaleSize = new Size(Currframe.width()/scale,Currframe.height()/scale);
        resize(Currframe, Currframe, scaleSize , 0, 0, Imgproc.INTER_AREA);

        //  Detect keypoints and compute descriptors using AKAZE
        akaze.detectAndCompute(Currframe, new Mat(), kpts, desc);


//        for scaling keypoints to size
        // comment below if you don't want to scale keypoints and uncomment for Currframe feature drawing
//        if(!kpts.empty()) {
//            KeyPoint[] keys = kpts.toArray();
//            for(int x = 0; x<kpts.rows();x++){
////                System.out.println(String.format("Checking Mat = " + keys[x].pt.x));
//                keys[x].pt.x = keys[x].pt.x *scale;
//                keys[x].pt.y = keys[x].pt.y *scale;
//
//
//            }
//
//            kpts.fromArray(keys);
//        }


//  Drawing feature points
        if(startMatching) {
            color = new Scalar(0, 255, 0); // BGR
        }
        else{
            color = new Scalar(255, 0, 0); // BGR
        }
        int flags = Features2d.DrawMatchesFlags_DEFAULT; // For each keypoint, the circle around keypoint with keypoint size and orientation will be drawn.

        //uncomment this if not scaling keypoints
        Features2d.drawKeypoints(Currframe, kpts, Currframe, color, flags);
        scaleSize = new Size(Currframe.width()*scale,Currframe.height()*scale);
        resize(Currframe, Currframe, scaleSize , 0, 0, Imgproc.INTER_CUBIC);
        return Currframe;

        //uncomment this if you scaling keypoints
//        Features2d.drawKeypoints(Firstframe, kpts, Firstframe, color, flags);
//        return Firstframe;

    }

}