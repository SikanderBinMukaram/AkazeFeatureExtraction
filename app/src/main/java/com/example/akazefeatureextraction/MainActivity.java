package com.example.akazefeatureextraction;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

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

import static org.opencv.imgproc.Imgproc.resize;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1;
    public boolean firstframe = true;

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

        mOpenCvCameraView = findViewById(R.id.main_surface);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

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
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame frame) {
        // get current camera frame as OpenCV Mat object
        Mat frame1 = frame.rgba();
        Mat Org = new Mat();
        frame1.copyTo(Org);
//        final Mat first = new Mat();



//        final Mat frame2 = new Mat();
//        final Mat frame1 = new Mat();
//

        //for only first frame
        Mat desc1 = new Mat();
        MatOfKeyPoint kpts1 = new MatOfKeyPoint();

        int scale = 4;
        Size scaleSize = new Size(frame1.width()/scale,frame1.height()/scale);

        resize(frame1, frame1, scaleSize , 0, 0, Imgproc.INTER_AREA);
        Size scaleSize2 = new Size(frame1.width()*scale,frame1.height()*scale);
        //  Detect keypoints and compute descriptors using AKAZE
        AKAZE akaze = AKAZE.create();
        akaze.detectAndCompute(frame1, new Mat(), kpts1, desc1);

//
//        if(firstframe && kpts1.rows()>10) {
//            frame1.copyTo(first);
//            firstframe = false;
//        }

        //
        if(!kpts1.empty()) {
            KeyPoint[] keys = kpts1.toArray();
            for(int x = 0; x<kpts1.rows();x++){
                System.out.println(String.format("Checking Mat = " + keys[x].pt.x));
                keys[x].pt.x = keys[x].pt.x *scale;
                keys[x].pt.y = keys[x].pt.y *scale;
                keys[x].angle = keys[x].angle;
//                keys[x].size = keys[x].size*scale;



            }

            kpts1.fromArray(keys);
        }


//        Mat desc2 = new Mat();
//        MatOfKeyPoint kpts2 = new MatOfKeyPoint();
//
//        if(i==0){
//
//            akaze.detectAndCompute(frame1, new Mat(), kpts1, desc1);
//            frame.copyTo(frame1);
//            akaze.detectAndCompute(frame2, new Mat(), kpts2, desc2);
//
//        }
//        else{
//            akaze.detectAndCompute(frame2, new Mat(), kpts2, desc2);
//        }

//  Drawing feature points
        Scalar color = new Scalar(255, 0, 0); // BGR
        int flags = Features2d.DrawMatchesFlags_DEFAULT; // For each keypoint, the circle around keypoint with keypoint size and orientation will be drawn.
//        multiply(kpts1.col(0),Scalar.all(scale),kpts1.col(0));
//        multiply(kpts1.col(1),Scalar.all(scale),kpts1.col(1));
//        Features2d.drawKeypoints(frame.rgba(), kpts1, out, color, flags);
        Features2d.drawKeypoints(Org, kpts1, Org, color, flags);
//        Features2d.drawKeypoints(frame1, kpts1, frame1, color, flags);

//        // Use brute-force matcher to find 2-nn matches
//        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
//        List<MatOfDMatch> KnnMatches = new ArrayList<>();
//        matcher.knnMatch(desc1,desc2,KnnMatches,2);
//
//        //Use 2-nn matches and ratio criterion to find correct keypoint matches
//        float MatchRatiothreshold = 0.8f;
//        List<KeyPoint> listOfMatched1 = new ArrayList<>();
//        List<KeyPoint> listOfMatched2 = new ArrayList<>();
//        List<KeyPoint> listOfKeypoints1 = kpts1.toList();
//        List<KeyPoint> listOfKeypoints2 = kpts2.toList();
//        List<DMatch> listOfGoodMatches = new ArrayList<>();
//
//        for(int j=0; j<KnnMatches.size();j++){
//            DMatch[] matches = KnnMatches.get(j).toArray();
//            float dist1 = matches[0].distance;
//            float dist2 = matches[1].distance;
//            if(dist1< MatchRatiothreshold*dist2){
//                listOfGoodMatches.add(new DMatch(listOfMatched1.size(), listOfMatched2.size(), 0));
//                listOfMatched1.add(listOfKeypoints1.get(matches[0].queryIdx));
//                listOfMatched2.add(listOfKeypoints2.get(matches[0].trainIdx));
//
//            }
//
//        }

//
//        final Mat res = new Mat();
//        MatOfKeyPoint inliers1 = new MatOfKeyPoint(listOfMatched1.toArray(new KeyPoint[listOfMatched1.size()]));
//        MatOfKeyPoint inliers2 = new MatOfKeyPoint(listOfMatched2.toArray(new KeyPoint[listOfMatched2.size()]));
//        MatOfDMatch goodMatches = new MatOfDMatch(listOfGoodMatches.toArray(new DMatch[listOfGoodMatches.size()]));
//
//        Features2d.drawMatches(frame1, inliers1, frame, inliers2,goodMatches, res);
////                               frame.copyTo(res);

        // native call to process current camera frame
//        adaptiveThresholdFromJNI(mat.getNativeObjAddr());

        // return processed frame for live preview
//        resize(frame1, frame1, scaleSize2 , 0, 0, Imgproc.INTER_CUBIC);

//        return frame1;
        return Org;

//        if(firstframe)
//            return frame1;
//        else
//            return frame1;
    }

}