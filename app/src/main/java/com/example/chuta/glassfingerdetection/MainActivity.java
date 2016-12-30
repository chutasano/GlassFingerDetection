package com.example.chuta.glassfingerdetection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.contrib.FaceRecognizer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.params.Face;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chuta.glassfingerdetection.R;

public class MainActivity extends Activity implements CvCameraViewListener2, GestureDetector.OnGestureListener {
    private static final String TAG = "com.example.c";
    private static final Scalar FACE_RECT_COLOR = new Scalar(255, 255, 255, 255); // white

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private CameraView mOpenCvCameraView;
    private Rect[] mFacesArray;

    private float mRelativeFingerMin = 0.1f;
    private int mAbsoluteFaceMin = 0;
    private int mAbsoluteFaceMax = 0;
    private float mRelativeFingerMax = 0.4f;
    private float mScaleFactor = 1.15f; //approach 1 for more accuracy w/ more time

    private GestureDetector mGestureDetector;

    /**
     * Constructor
     */
    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setContentView(R.layout.face_detect_surface_view);
        int height = 360;
        if (Math.round(height * mRelativeFingerMax) > 0) {
            mAbsoluteFaceMax = Math.round(height * mRelativeFingerMax);
            mAbsoluteFaceMin = Math.round(height * mRelativeFingerMin);
        }
        // Important for Google Glass
        mGestureDetector = new GestureDetector(this, this);
        mOpenCvCameraView = (CameraView) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        // Load default libopencv_java.so
        if (OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "Libraries Loaded!", Toast.LENGTH_SHORT).show();

            if (mLoaderCallback != null) {
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        } else {
            Toast.makeText(getApplicationContext(), "failed to load libraries", Toast.LENGTH_SHORT).show();
        }

        // Usual OpenCV Loader
        // OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    /**
     * Create callback that is loaded if the OpenCV libraries load correctly
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        /*
         * (non-Javadoc)
         * @see org.opencv.android.BaseLoaderCallback#onManagerConnected(int)
         */
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.cascade_finger_tip);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);

                        // cascade algorithms
                        // local binary patterns
                        // faster - less accurate
                        // mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");

                        // haar-like faces
                        // Viola and Jones[2] adapted the idea of using Haar wavelets and developed the so-called Haar-like feature
                        mCascadeFile = new File(cascadeDir, "cascade_finger_tip.xml");

                        // http://docs.opencv.org/trunk/doc/py_tutorials/py_objdetect/py_face_detection/py_face_detection.html#face-detection
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
                        }

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    /*
     * (non-Javadoc)
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraViewStarted(int, int)
     */
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    /*
     * (non-Javadoc)
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraViewStopped()
     */
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    /*
     * (non-Javadoc)
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null) {
            mJavaDetector.detectMultiScale(mGray, faces, mScaleFactor, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(mAbsoluteFaceMin, mAbsoluteFaceMin), new Size(mAbsoluteFaceMax, mAbsoluteFaceMax));
        }

        // Draw rectangles
        mFacesArray = faces.toArray();
        for (int i = 0; i < mFacesArray.length; i++) {
            Imgproc.rectangle(mRgba, mFacesArray[i].tl(), mFacesArray[i].br(), FACE_RECT_COLOR, 3);
        }
        return mRgba;
    }

    /**
     * Set the relative face size and toast to the screen
     *
     * @param faceSize Face size
     */
    private void setMinFaceSize(float faceSize) {
        mRelativeFingerMin = faceSize;
        mAbsoluteFaceMin = 0;
        int height = mGray.rows();
        if (Math.round(height * mRelativeFingerMax) > 0) {
            mAbsoluteFaceMax = Math.round(height * mRelativeFingerMax);
            mAbsoluteFaceMin = Math.round(height * mRelativeFingerMin);
        }
        Toast.makeText(this, String.format("Face size: %.0f%%", mRelativeFingerMin * 100.0f), Toast.LENGTH_SHORT).show();
    }

    /**
     * Turn on touch events for Google Glass
     */
    /*
	 * (non-Javadoc)
	 * @see android.app.Activity#onGenericMotionEvent(android.view.MotionEvent)
	 */
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.OnGestureListener#onDown(android.view.MotionEvent)
     */
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.OnGestureListener#onShowPress(android.view.MotionEvent)
     */
    @Override
    public void onShowPress(MotionEvent e) {
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.OnGestureListener#onSingleTapUp(android.view.MotionEvent)
     */
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.OnGestureListener#onScroll(android.view.MotionEvent, android.view.MotionEvent, float, float)
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see android.view.GestureDetector.OnGestureListener#onLongPress(android.view.MotionEvent)
     */
    @Override
    public void onLongPress(MotionEvent e) {
    }

    /**
     * Special Thanks:
     * https://github.com/space150/google-glass-playground/blob/master/OpenCVFaceDetection/src/com/space150/android/glass/opencvfacedetection/MainActivity.java
     */
	/*
	 * (non-Javadoc)
	 * @see android.view.GestureDetector.OnGestureListener#onFling(android.view.MotionEvent, android.view.MotionEvent, float, float)
	 */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float minSize = mRelativeFingerMin;


        if (velocityX < 0.0f) // swipe forward
        {
            minSize -= 0.02f;
            if (minSize < 0.04f) {
                minSize = 0.04f;
            }
        } else if (velocityX > 0.0f) // swipe backward
        {
            minSize += 0.02f;
            if (minSize > 0.8f) {
                minSize = 0.8f;
            }
        }

        setMinFaceSize(minSize);

        return false;
    }


}