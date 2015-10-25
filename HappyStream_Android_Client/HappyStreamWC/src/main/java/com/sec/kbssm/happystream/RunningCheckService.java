package com.sec.kbssm.happystream;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class RunningCheckService extends Service {
    private static final String TAG = RunningCheckService.class.getSimpleName();
    private static final int CHECK_DELAY_MS = 5000;

    private Handler mHandler;
    private Runnable mRunnable;

    public RunningCheckService() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() invoked");

        mRunnable = new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "run() invoked!");
                //check ps squid
                String result = ExecuteShell.checkServerRunning(Common.applicationPath);
                mHandler.postDelayed(mRunnable, CHECK_DELAY_MS);
            }
        };
        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, CHECK_DELAY_MS);


        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() invoked");


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "onDestroy() invoked");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
