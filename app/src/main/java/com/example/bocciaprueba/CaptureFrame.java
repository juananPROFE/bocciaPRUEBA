package com.example.bocciaprueba;

import static org.opencv.android.Utils.matToBitmap;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.getStructuringElement;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.opencv.video.Video;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class CaptureFrame extends CameraActivity implements CvCameraViewListener2, View.OnClickListener {

    private static String fecha;
    private static final File parentDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    private static  File newDir = new File(parentDir,"/bocciaPRUEBA");

    private static File subDir=null;
    private static final String TAG = "OCVSample::Activity";
    private static String filename_MP4 = null;
    private static String filename_AVI = null;

    private static final int STATUS_FINISHED_PLAYBACK = 0;
    private static final int STATUS_PREVIEW = 1;
    private static final int STATUS_RECORDING = 2;
    private static final int STATUS_PLAYING = 3;
    private static final int STATUS_ERROR = 4;

    private String mVideoFilename="";
    private boolean mUseBuiltInMJPG = false;

    private int mStatus = STATUS_FINISHED_PLAYBACK;
    private int mFPS = 30;
    private int mWidth = 0, mHeight = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    private ImageView mImageView;
    private Button mTriggerButton;
    private TextView mStatusTextView;
    Runnable mPlayerThread;

    private VideoWriter mVideoWriter = null;
    private VideoCapture mVideoCapture = null;

    private Mat mVideoFrame_blur;
    private Mat mVideoFrame;

    private Mat mVideoFrameAnt_blur;

    private Mat mVideoFrameAnt;

    private Mat blur_result;
    private Mat mRenderFrame;

    private int count=0;

    private int numBola=1;
    private double res=0;
    private Mat mVideoFrameAnt_gray;

    private Mat mVideoFrame_gray;

    private Mat umbral;
    public CaptureFrame() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.recorder_surface_view);

        mStatusTextView = (TextView) findViewById(R.id.textview1);
        mStatusTextView.bringToFront();

        //Creando carpeta bocciaPrueba
        if (newDir.mkdirs()){
            Log.i(TAG, "Create Folder bocciaPrueba");
        } else {
            Log.i(TAG, "Folder bocciaPrueba already exists");
        }


// Si estas en un fragment utiliza activity en vez de this
// Este código lo puedes colocar el onCreate.

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            mStatus = STATUS_ERROR;
            mStatusTextView.setText("Error: Can't initialize OpenCV");
            return;
        }

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.recorder_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.disableView();

        mImageView = (ImageView) findViewById(R.id.image_view);

        mTriggerButton = (Button) findViewById(R.id.btn1);
        mTriggerButton.setOnClickListener(this);
        mTriggerButton.bringToFront();

    }

    @Override
    public void onPause()
    {
        Log.d(TAG, "Pause");
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mImageView.setVisibility(SurfaceView.GONE);
        if (mVideoWriter != null) {
            mVideoWriter.release();
            mVideoWriter = null;
        }
        if (mVideoCapture != null) {
            mVideoCapture.release();
            mVideoCapture = null;
        }
        mStatus = STATUS_FINISHED_PLAYBACK;
        mStatusTextView.setText("Status: Finished playback");
        mTriggerButton.setText("Start Camera");

        mVideoFrame.release();
        mRenderFrame.release();
    }

    @Override
    public void onResume()
    {
        Log.d(TAG, "onResume");
        super.onResume();

        mVideoFrame = new Mat();
        mRenderFrame = new Mat();
        mVideoFrame_blur=new Mat();
        mVideoFrameAnt_blur=new Mat();
        mVideoFrameAnt_gray=new Mat();
        mVideoFrame_gray=new Mat();
        blur_result=new Mat();
        umbral=new Mat();
        changeStatus();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        Log.d(TAG, "called onDestroy");
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if (mVideoWriter != null)
            mVideoWriter.release();
        if (mVideoCapture != null)
            mVideoCapture.release();
    }

    public void onCameraViewStarted(int width, int height) {
        Log.d(TAG, "Camera view started " + String.valueOf(width) + "x" + String.valueOf(height));
        mWidth = width;
        mHeight = height;
    }

    public void onCameraViewStopped() {
        Log.d(TAG, "Camera view stopped");
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame)
    {
        Log.d(TAG, "Camera frame arrived");

        Mat rgbMat = inputFrame.rgba();

        Log.d(TAG, "Size: " + rgbMat.width() + "x" + rgbMat.height());

        if (mVideoWriter != null && mVideoWriter.isOpened()) {
            Imgproc.cvtColor(rgbMat, mVideoFrame, Imgproc.COLOR_RGBA2BGR);
            mVideoWriter.write(mVideoFrame);
        }

        return rgbMat;
    }

    @Override
    public void onClick(View view) {
        Log.i(TAG,"onClick event");
        changeStatus();
    }

    public void changeStatus() {
        switch(mStatus) {
            case STATUS_ERROR:
                Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
                break;
            case STATUS_FINISHED_PLAYBACK:
                if (!startPreview()) {
                    setErrorStatus();
                    break;
                }
                mStatus = STATUS_PREVIEW;
                mStatusTextView.setText("Status: Camera preview");
                mTriggerButton.setText("Start recording");
                break;
            case STATUS_PREVIEW:
                if (!startRecording()) {
                    setErrorStatus();
                    break;
                }
                mStatus = STATUS_RECORDING;
                mStatusTextView.setText("Status: "+mVideoFilename);
                mTriggerButton.setText(" Stop and play video");
                break;
            case STATUS_RECORDING:
                if (!stopRecording()) {
                    setErrorStatus();
                    break;
                }
                if (!startPlayback()) {
                    setErrorStatus();
                    break;
                }
                mStatus = STATUS_PLAYING;
                mStatusTextView.setText("Status: Playing video");
                mTriggerButton.setText("Stop playback");
                break;
            case STATUS_PLAYING:
                if (!stopPlayback()) {
                    setErrorStatus();
                    break;
                }
                mStatus = STATUS_FINISHED_PLAYBACK;
                mStatusTextView.setText("Status: Finished playback");
                mTriggerButton.setText("Start Camera");
                break;
        }
    }

    public void setErrorStatus() {
        mStatus = STATUS_ERROR;
        mStatusTextView.setText("Status: Error");
    }

    public boolean startPreview() {
        mOpenCvCameraView.enableView();
        mOpenCvCameraView.setVisibility(View.VISIBLE);
        return true;
    }

    public boolean startRecording() {
        count=0;
        numBola=1;
        Log.i(TAG,"Starting recording");
        Mat frame =new Mat();
        Bitmap img=null;
        int counter=1;
        fecha =LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        filename_AVI="video.avi";
        filename_MP4 = "video.mp4";

        subDir = new File(parentDir,"/bocciaPRUEBA/"+fecha);
        if (subDir.mkdirs()){
            Log.i(TAG, "Create Folder bocciaPrueba");
        } else {
            Log.i(TAG, "Folder bocciaPrueba already exists");
        }
        if (mUseBuiltInMJPG)
            mVideoFilename = subDir + "/" + filename_AVI;
        else
            mVideoFilename = subDir + "/" + filename_MP4;

        mVideoWriter = new VideoWriter();
        if (!mUseBuiltInMJPG) {
            mVideoWriter.open(mVideoFilename, Videoio.CAP_ANDROID, VideoWriter.fourcc('H', '2', '6', '4'), mFPS, new Size(mWidth, mHeight));
            if (!mVideoWriter.isOpened()) {
                Log.i(TAG,"Can't record H264. Switching to MJPG");
                mUseBuiltInMJPG = true;
            }
            else{
                mVideoWriter.write(frame);
                while (true) {
                    if (counter%500==0) {
                        ;

                        if (frame.empty()) {
                            break;
                        }
                        mVideoWriter.write(frame);
                    }else{
                        if (frame.empty()) {
                            break;
                        }
                        mVideoWriter.write(frame);
                    }
                    counter++;
                }

            }
        }

        if (mUseBuiltInMJPG) {
            mVideoWriter.open(mVideoFilename, VideoWriter.fourcc('M', 'J', 'P', 'G'), mFPS, new Size(mWidth, mHeight));
        }

        Log.d(TAG, "Size: " + String.valueOf(mWidth) + "x" + String.valueOf(mHeight));
        Log.d(TAG, "File: " + mVideoFilename);

        if (mVideoWriter.isOpened()) {
            Toast.makeText(this, "Record started to file " + mVideoFilename, Toast.LENGTH_LONG).show();
            return true;
        } else {
            Toast.makeText(this, "Failed to start a record", Toast.LENGTH_LONG).show();
            return false;
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }


    public boolean stopRecording() {
        Log.i(TAG, "Finishing recording");
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setVisibility(SurfaceView.GONE);
        mVideoWriter.release();
        mVideoWriter = null;
        return true;
    }

    public boolean startPlayback() {
        mImageView.setVisibility(SurfaceView.VISIBLE);
        if (!mUseBuiltInMJPG){
            mVideoCapture = new VideoCapture(mVideoFilename, Videoio.CAP_ANDROID);
        } else {
            mVideoCapture = new VideoCapture(mVideoFilename, Videoio.CAP_OPENCV_MJPEG);
        }

        if (!mVideoCapture.isOpened()) {
            Log.e(TAG, "Can't open video");
            Toast.makeText(this, "Can't open file " + mVideoFilename, Toast.LENGTH_SHORT).show();
            return false;
        }



        Toast.makeText(this, "Starting playback from file " + mVideoFilename, Toast.LENGTH_SHORT).show();

        mPlayerThread = new Runnable() {
            @Override
            public void run() {
                File mypath;
                FileOutputStream fos = null;
                Bitmap bmp=null;
                BackgroundSubtractor backSub;
                if (mVideoCapture == null || !mVideoCapture.isOpened()) {
                    return;
                }

                mVideoCapture.read(mVideoFrame);

                if (mVideoFrame.empty()) {
                    if (mStatus == STATUS_PLAYING) {
                        changeStatus();
                    }
                    return;
                }

                //Obteniendo primer frame como referencia y pasandolo a mVideoFrameAnt
                if(!mVideoFrame.empty() && mVideoFrameAnt_blur.empty()) {
                    //desechamos los primeros 20 frames para controlar el cambio de luz.
                    for(int i=0;i<20;i++){
                        mVideoCapture.read(mVideoFrame);
                        if (mVideoFrame.empty()) {
                            if (mStatus == STATUS_PLAYING) {
                                changeStatus();
                            }
                            return;
                        }
                    }


                   backSub= Video.createBackgroundSubtractorMOG2();
                   backSub.apply(mVideoFrame,mVideoFrameAnt_blur);
                    //Limpiando ruido
                    //Imgproc.cvtColor(mVideoFrame, mVideoFrameAnt_gray, Imgproc.COLOR_RGB2GRAY);
                    //Imgproc.GaussianBlur(mVideoFrameAnt_blur, mVideoFrameAnt_blur, new Size(3,3),0);
                    bmp = Bitmap.createBitmap(mVideoFrame.cols(), mVideoFrame.rows(), Bitmap.Config.ARGB_8888);
                    matToBitmap(mVideoFrameAnt_blur, bmp);
                    mypath = new File(subDir, "firstFrame.jpg");
                    try {
                        fos = new FileOutputStream(mypath);
                        // Use the compress method on the BitMap object to write image to the OutputStream
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if(!mVideoFrameAnt_blur.empty()) {
                    Imgproc.cvtColor(mVideoFrame, mVideoFrame_gray, Imgproc.COLOR_RGB2GRAY);
                    Imgproc.GaussianBlur(mVideoFrame_gray, mVideoFrame_blur, new Size(3, 3),0);
                    //Imgproc.blur(mVideoFrame_gray, mVideoFrame_blur, new Size(4, 4));
                   // res = Imgproc.compareHist(mVideoFrame_blur, mVideoFrameAnt_blur, Imgproc.CV_COMP_INTERSECT);

                    Core.absdiff(mVideoFrame_gray, mVideoFrameAnt_blur, blur_result);
                    Imgproc.threshold(blur_result,umbral,127,255,Imgproc.THRESH_BINARY);
                    //Mat element = getStructuringElement( MORPH_ELLIPSE,new Size(4, 4),new Point(-1,-1) );
                    //Imgproc.dilate(umbral,umbral,element);
                    //Siguiente paso, encontrar contornos
                    //Imgproc.findContours(umbral,)
                    //Core.compare(mVideoFrame_blur, mVideoFrameAnt_blur, blur_result, Core.CMP_EQ);
                    //if(Core.countNonZero(umbral)>100000)
                     if(Core.countNonZero(umbral)>10){
                        for(int i=0;i<20;i++) {
                            mVideoCapture.read(mVideoFrame);
                            if (mVideoFrame.empty()) {
                                if (mStatus == STATUS_PLAYING) {
                                    changeStatus();
                                }
                                return;
                            }
                        }
                        bmp = Bitmap.createBitmap(umbral.cols(), umbral.rows(), Bitmap.Config.ARGB_8888);
                        matToBitmap(blur_result, bmp);
                        mypath = new File(subDir,  numBola+"_" + Core.countNonZero(umbral) + ".jpg");
                        numBola++;
                        count++;
                        try {
                            fos = new FileOutputStream(mypath);
                            // Use the compress method on the BitMap object to write image to the OutputStream
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        //  if (Core.countNonZero(umbral)>150000) {
                         backSub= Video.createBackgroundSubtractorMOG2();
                         backSub.apply(mVideoFrame,mVideoFrameAnt_blur);
                        //mVideoFrameAnt_gray=mVideoFrame_gray;
                        //mVideoFrameAnt_blur=mVideoFrame_blur;
                        // }
                        //mVideoFrameAnt_blur=mVideoFrame_blur;

                        bmp = Bitmap.createBitmap(mVideoFrameAnt_blur.cols(), mVideoFrameAnt_blur.rows(), Bitmap.Config.ARGB_8888);
                        matToBitmap(mVideoFrameAnt_blur, bmp);
                        mypath = new File(subDir, "firstFrame"+numBola+".jpg");
                        try {
                            fos = new FileOutputStream(mypath);
                            // Use the compress method on the BitMap object to write image to the OutputStream
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                //Lo siguiente a probar, commparar imágenes.
                //Imgproc.matchTemmplate

               /* if(mVideoFrameAnt!=null) {
                    //Limpiando ruido
                    Imgproc.blur(mVideoFrameAnt, mVideoFrameAnt_blur, new Size(4, 4));
                    Imgproc.blur(mVideoFrame, mVideoFrame_blur, new Size(4, 4));

                    Core.absdiff(mVideoFrame_blur, mVideoFrameAnt_blur, blur_result);

                    // VideoCapture with CAP_ANDROID generates RGB frames instead of BGR
                    // https://github.com/opencv/opencv/issues/24687
                    Imgproc.cvtColor(mVideoFrame, mRenderFrame, mUseBuiltInMJPG ? Imgproc.COLOR_BGR2RGBA : Imgproc.COLOR_RGB2RGBA);
                    Bitmap bmp = Bitmap.createBitmap(mRenderFrame.cols(), mRenderFrame.rows(), Bitmap.Config.ARGB_8888);
                    matToBitmap(mRenderFrame, bmp);
                    mImageView.setImageBitmap(bmp);


                    if (false) {
                        count++;
                        mypath = new File(newDir, fecha + " capture" + count + ".jpg");
                        try {
                            fos = new FileOutputStream(mypath);
                            // Use the compress method on the BitMap object to write image to the OutputStream
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }*/
                Handler h = new Handler();
                h.postDelayed(this, 33);
            }
        };

        mPlayerThread.run();
        return true;
    }

    public boolean stopPlayback() {

        mVideoCapture.release();
        mVideoCapture = null;
        mImageView.setVisibility(SurfaceView.VISIBLE);
        return true;
    }

}
