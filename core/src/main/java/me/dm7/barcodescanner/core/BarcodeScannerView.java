package me.dm7.barcodescanner.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public abstract class BarcodeScannerView extends FrameLayout implements Camera.PreviewCallback  {
    private static final String TAG = "barcodescanner.core.BarcodeScannerView";
    private Camera mCamera;
    private CameraPreview mPreview;
    private ViewFinderView mViewFinderView;
    private Rect mFramingRectInPreview;

    public BarcodeScannerView(Context context) {
        super(context);
        setupLayout();
    }

    public BarcodeScannerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setupLayout();
    }

    public void setupLayout() {
        mPreview = new CameraPreview(getContext());
        mViewFinderView = new ViewFinderView(getContext());

        RelativeLayout relativeLayout = new RelativeLayout(getContext());
        relativeLayout.setGravity(Gravity.CENTER);
        relativeLayout.setBackgroundColor(Color.BLACK);
        relativeLayout.addView(mPreview);
        addView(relativeLayout);

        addView(mViewFinderView);
    }

    public Camera getCameraInstance() {
        return mCamera;
    }

    public void startCamera() {
        AsyncTask<Void, Void, Camera> getCameraInstance = new AsyncTask<Void, Void, Camera>() {
            @Override
            protected Camera doInBackground(Void... params) {
                if(mCamera != null) {
                    stopCamera();
                }
                mCamera = CameraUtils.getCameraInstance();
                return mCamera;
            }

            @Override
            protected void onPostExecute(Camera camera) {
                if(mCamera != null) {
                    mViewFinderView.setupViewFinder();
                    mPreview.setCamera(mCamera, BarcodeScannerView.this);
                    mPreview.initCameraPreview();
                    Log.i(TAG, "Started camera");
                }
            }
        };

        getCameraInstance.execute();
    }

    public void stopCamera() {
        if(mCamera != null) {
            mPreview.stopCameraPreview();
            mPreview.setCamera(null, null);
            Log.i(TAG, "Stopped camera");
            mCamera.release();
            Log.i(TAG, "Released camera");
            mCamera = null;
        } else {
            Log.i(TAG, "Camera already stopped and released");
        }
    }

    public synchronized Rect getFramingRectInPreview(int width, int height) {
        if (mFramingRectInPreview == null) {
            Rect framingRect = mViewFinderView.getFramingRect();
            if (framingRect == null) {
                return null;
            }
            Rect rect = new Rect(framingRect);
            Point screenResolution = DisplayUtils.getScreenResolution(getContext());
            Point cameraResolution = new Point(width, height);

            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }

            rect.left = rect.left * cameraResolution.x / screenResolution.x;
            rect.right = rect.right * cameraResolution.x / screenResolution.x;
            rect.top = rect.top * cameraResolution.y / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;

            mFramingRectInPreview = rect;
        }
        return mFramingRectInPreview;
    }

    public void setFlash(boolean flag) {
        if(mCamera != null && CameraUtils.isFlashSupported(mCamera)) {
            Camera.Parameters parameters = mCamera.getParameters();
            if(flag) {
                if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                    return;
                }
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            try {
              mCamera.setParameters(parameters);
            } catch(RuntimeException e) {
              // TODO: Actually fix this issue on Sony devices.
            }
        }
    }

    public boolean getFlash() {
        if(mCamera != null && CameraUtils.isFlashSupported(mCamera)) {
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public void toggleFlash() {
        if(mCamera != null && CameraUtils.isFlashSupported(mCamera)) {
            Camera.Parameters parameters = mCamera.getParameters();
            if(parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            mCamera.setParameters(parameters);
        }
    }

    public void setAutoFocus(boolean state) {
        if(mPreview != null) {
            mPreview.setAutoFocus(state);
        }
    }
}
