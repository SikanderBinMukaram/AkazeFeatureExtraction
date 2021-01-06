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
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.AKAZE;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.resize;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1;


    public boolean startMatching = false, FirstframeAssign=false;
    private GestureDetectorCompat mDetector;

    //Initialization

    public Mat Currframe, Firstframe, Dispframe;
    public AKAZE akaze;
    public Mat Currframe_desc, Firstframe_desc;
    public MatOfKeyPoint Currframe_kpts, Firstframe_kpts, Disp_kpts;
    public Scalar color;
    public int flags; // For each keypoint, the circle around keypoint with keypoint size and orientation will be drawn.

    public Size scaleSize;
    public DescriptorMatcher matcher;
    public List<MatOfDMatch> KnnMatches;


    public int scale = 4;
    public float MatchRatiothreshold = 0.8f;



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
            FirstframeAssign = true;
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
        startMatching = false; FirstframeAssign=false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        Currframe.release();
        Firstframe.release();
        Currframe_desc.release();
        Currframe_kpts.release();
        Firstframe_desc.release();
        Firstframe_kpts.release();
        Dispframe.release();
        Disp_kpts.release();


    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Currframe = new Mat();
        Firstframe = new Mat();
        Dispframe = new Mat();
        Disp_kpts = new MatOfKeyPoint();
        Firstframe_desc = new Mat();
        Firstframe_kpts = new MatOfKeyPoint();
        Currframe_desc = new Mat();
        Currframe_kpts = new MatOfKeyPoint();
        akaze = AKAZE.create();
        Log.i("AKAZE", "getDiffusivity " + akaze.getDiffusivity());
        Log.i("AKAZE", "getDescriptorChannels " + akaze.getDescriptorChannels());
        Log.i("AKAZE", "getDescriptorSize " + akaze.getDescriptorSize());
        Log.i("AKAZE", "getDescriptorType " + akaze.getDescriptorType());
        Log.i("AKAZE", "getNOctaveLayers " + akaze.getNOctaveLayers());
        Log.i("AKAZE", "getNOctaves " + akaze.getNOctaves());
        Log.i("AKAZE", "getThreshold " + akaze.getThreshold());

        akaze.getDiffusivity();
        flags = Features2d.DrawMatchesFlags_DEFAULT;
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        KnnMatches = new ArrayList<>();
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame frame) {
        // Release all unused mats
//        Currframe.release();
//        Firstframe.release();
//        Currframe_desc.release();
//        Currframe_kpts.release();
//        Firstframe_desc.release();
//        Firstframe_kpts.release();
//        Dispframe.release();
//        Disp_kpts.release();

        // get current camera frame as OpenCV Mat object

        Currframe = frame.rgba();
        Currframe.copyTo(Dispframe);

        if(!startMatching) {

            scaleSize = new Size(Currframe.width() / scale, Currframe.height() / scale);
            resize(Currframe, Currframe, scaleSize, 0, 0, Imgproc.INTER_AREA);

            //  Detect keypoints and compute descriptors using AKAZE
            akaze.detectAndCompute(Currframe, new Mat(), Currframe_kpts, Currframe_desc);


//        for scaling keypoints to size
//             comment below if you don't want to scale keypoints and uncomment for Currframe feature drawing
        if(!Currframe_kpts.empty()) {
            KeyPoint[] keys = Currframe_kpts.toArray();
            for(int x = 0; x<Currframe_kpts.rows();x++){
//                System.out.println(String.format("Checking Mat = " + keys[x].pt.x));
                keys[x].pt.x = keys[x].pt.x * scale;
                keys[x].pt.y = keys[x].pt.y * scale;
                keys[x].size = keys[x].size * scale;


            }

            Disp_kpts.fromArray(keys);
        }


//  Drawing feature points
            color = new Scalar(255, 0, 0); // BGR



        //uncomment this if not scaling keypoints
            Features2d.drawKeypoints(Dispframe, Disp_kpts, Dispframe, color, flags);
//            scaleSize = new Size(Currframe.width()*scale,Currframe.height()*scale);
//            resize(Currframe, Currframe, scaleSize , 0, 0, Imgproc.INTER_CUBIC);

        }
        else{

            scaleSize = new Size(Currframe.width() / scale, Currframe.height() / scale);

            if(FirstframeAssign){
                Currframe.copyTo(Firstframe);
                resize(Firstframe, Firstframe, scaleSize, 0, 0, Imgproc.INTER_AREA);
                akaze.detectAndCompute(Firstframe, new Mat(), Firstframe_kpts, Firstframe_desc);

                FirstframeAssign = false;
            }

            resize(Currframe, Currframe, scaleSize, 0, 0, Imgproc.INTER_AREA);

            //  Detect keypoints and compute descriptors using AKAZE
            akaze.detectAndCompute(Currframe, new Mat(), Currframe_kpts, Currframe_desc);
            Log.i("AKAZE", "keypoints " + Currframe_kpts.rows());

            if(!Currframe_kpts.empty()) {
                KeyPoint[] keys = Currframe_kpts.toArray();
                for(int x = 0; x<Currframe_kpts.rows();x++){
//                System.out.println(String.format("Checking Mat = " + keys[x].pt.x));
                    keys[x].pt.x = keys[x].pt.x * scale;
                    keys[x].pt.y = keys[x].pt.y * scale;
                    keys[x].size = keys[x].size * scale;


                }

                Disp_kpts.fromArray(keys);
            }


//  Drawing feature points
//            if (startMatching) {
//                color = new Scalar(0, 255, 0); // BGR
//            } else {
//                color = new Scalar(255, 0, 0); // BGR
//            }
            color = new Scalar(0, 0, 255); // BGR



            //uncomment this if not scaling keypoints
            Features2d.drawKeypoints(Dispframe, Disp_kpts, Dispframe, color, flags);
//            scaleSize = new Size(Currframe.width()*scale,Currframe.height()*scale);
//            resize(Currframe, Currframe, scaleSize , 0, 0, Imgproc.INTER_CUBIC);

//        // Use brute-force matcher to find 2-nn matches
//        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
//        List<MatOfDMatch> KnnMatches = new ArrayList<>();
            System.out.println(String.format("Curr = " + Currframe_kpts.rows() + " First = " + Firstframe_kpts.rows()));

            matcher.knnMatch(Currframe_desc,Firstframe_desc,KnnMatches,2);

//
//        //Use 2-nn matches and ratio criterion to find correct keypoint matches
//        float MatchRatiothreshold = 0.8f;
            List<KeyPoint> listOfMatched_Curr = new ArrayList<>();
            List<KeyPoint> listOfMatched_first = new ArrayList<>();
            List<KeyPoint> listOfKeypoints1 = Currframe_kpts.toList();
            List<KeyPoint> listOfKeypoints2 = Firstframe_kpts.toList();
            List<DMatch> listOfGoodMatches = new ArrayList<>();

            for(int j=0; j<KnnMatches.size();j++){
                DMatch[] matches = KnnMatches.get(j).toArray();
                float dist1 = matches[0].distance;
                float dist2 = matches[1].distance;
                if(dist1< MatchRatiothreshold*dist2){
                    listOfGoodMatches.add(new DMatch(listOfMatched_Curr.size(), listOfMatched_first.size(), 0));
                    listOfMatched_Curr.add(listOfKeypoints1.get(matches[0].queryIdx));
                    listOfMatched_first.add(listOfKeypoints2.get(matches[0].trainIdx));

                }

            }
//
//        final Mat res = new Mat();
            MatOfKeyPoint inliers1 = new MatOfKeyPoint(listOfMatched_Curr.toArray(new KeyPoint[listOfMatched_Curr.size()]));
//        MatOfKeyPoint inliers2 = new MatOfKeyPoint(listOfMatched2.toArray(new KeyPoint[listOfMatched2.size()]));
//        MatOfDMatch goodMatches = new MatOfDMatch(listOfGoodMatches.toArray(new DMatch[listOfGoodMatches.size()]));
//
//        Features2d.drawMatches(frame1, inliers1, frame, inliers2,goodMatches, res);
////                               frame.copyTo(res);
            if(!inliers1.empty()) {
                KeyPoint[] keys1 = inliers1.toArray();
                for(int x = 0; x<inliers1.rows();x++){
//                System.out.println(String.format("Checking Mat = " + keys[x].pt.x));
                    keys1[x].pt.x = keys1[x].pt.x * scale;
                    keys1[x].pt.y = keys1[x].pt.y * scale;
                    keys1[x].size = keys1[x].size * scale;


                }

                Disp_kpts.fromArray(keys1);
            }


//  Drawing feature points
//            if (startMatching) {
//                color = new Scalar(0, 255, 0); // BGR
//            } else {
//                color = new Scalar(255, 0, 0); // BGR
//            }
            color = new Scalar(0, 255, 0); // BGR



            //uncomment this if not scaling keypoints
            Features2d.drawKeypoints(Dispframe, Disp_kpts, Dispframe, color, flags);
//            scaleSize = new Size(Currframe.width()*scale,Currframe.height()*scale);
//            resize(Currframe, Currframe, scaleSize , 0, 0, Imgproc.INTER_CUBIC);

        }


        return Dispframe;

        //uncomment this if you scaling keypoints
//        Features2d.drawKeypoints(Firstframe, kpts, Firstframe, color, flags);
//        return Firstframe;

    }

}