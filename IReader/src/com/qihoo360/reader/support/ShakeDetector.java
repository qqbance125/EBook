
package com.qihoo360.reader.support;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
/**
 *	抖动检测器
 */
public class ShakeDetector implements SensorEventListener {
	//力阈值
    private static final int FORCE_THRESHOLD = 1000;
    //时间阈值
    private static final long TIME_THRESHOLD = 100000000l;
    private static final long SHAKE_TIMEOUT = 500000000l;
    private static final long SHAKE_DURATION = 1000000000l;
    private static final int SHAKE_COUNT = 3;

    //重力感应器
    private SensorManager mSensorMgr;
    private Sensor mAccelerometer;
    private float mLastX = -1.0f, mLastY = -1.0f, mLastZ = -1.0f;
    private long mLastTime;
    private OnShakeListener mShakeListener;
    private Context mContext;
    private int mShakeCount = 0;
    private long mLastShake;
    private long mLastForce;

    public interface OnShakeListener {
        public void onShake();
    }

    public ShakeDetector(Context context) {
        mContext = context;
        resume();
    }

    public void setOnShakeListener(OnShakeListener listener) {
        mShakeListener = listener;
    }

    public void resume() {
        mSensorMgr = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }
        boolean supported = mSensorMgr.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_GAME);
        if (!supported) {
            mSensorMgr.unregisterListener(this, mAccelerometer);
            throw new UnsupportedOperationException("Accelerometer not supported");
        }


    }

    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this, mAccelerometer);
            mSensorMgr = null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        if (mAccelerometer != event.sensor)
            return;

        long time = event.timestamp;
        if ((time - mLastForce) > SHAKE_TIMEOUT) {
            mShakeCount = 0;
        }

        if ((time - mLastTime) > TIME_THRESHOLD) {
            long diff = time - mLastTime;
            float speed = Math.abs(values[0] + values[1]
                    + values[2] - mLastX - mLastY - mLastZ)
                    / diff * 10000000000l;

            if (speed > FORCE_THRESHOLD) {
                if ((++mShakeCount >= SHAKE_COUNT) && (time - mLastShake > SHAKE_DURATION)) {
                    mLastShake = time;
                    mShakeCount = 0;
                    if (mShakeListener != null) {
                        mShakeListener.onShake();
                    }
                }
                mLastForce = time;
            }
            mLastTime = time;
            mLastX = values[0];
            mLastY = values[1];
            mLastZ = values[2];
        }
    }
}
