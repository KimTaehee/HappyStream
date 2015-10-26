package com.sec.kbssm.happystream;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RunningCheckService extends Service {
    private static final String TAG = RunningCheckService.class.getSimpleName();
    private static final int CHECK_DELAY_MS = 3000;


    SharedPreferences pref;

    private Handler mHandler;
    private Runnable mRunnable;

    public RunningCheckService() {
    }

    /**
     * true is running, or false
     * @return
     */
    public static boolean checkRunningServer() {
        //Log.v(TAG, "run() invoked!");
        //check ps squid
        try {
            String result = serviceRun("ps squid");
            if(result.contains("squid")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() invoked");

        mRunnable = new Runnable() {
            @Override
            public void run() {
                Common.isSquidRunning = checkRunningServer();
                Log.i(TAG, "isSquidRunning: " + Common.isSquidRunning);
                Intent intent = new Intent(Common.ACTION_TO_REFRESH_UI);
                sendBroadcast(intent);

                pref = getSharedPreferences("pref", MODE_PRIVATE);
                if(pref.getBoolean(Common.ACTION_USER_WANT_SERVER_ON, false) == true && !Common.isSquidRunning) { //꺼졌는데 사용자가 이걸 켜두길 원하면
                    Log.i(TAG, "attempt to restart squid");
                    ExecuteShell.runServer(Common.FILES_PATH); //TODO: 정상동작하는지 확인하라!
                } else if(pref.getBoolean(Common.ACTION_USER_WANT_SERVER_ON, false) == false && Common.isSquidRunning) { //사용자는 끄려고 했는데 서버가 켜져있는 경우
                    Log.i(TAG, "attempt to kill squid");
                    ExecuteShell.killServer(Common.FILES_PATH); //TODO: 정상동작하는지 확인하라!
                };


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

    public static String serviceRun(String path) throws Exception {
        Process nativeApp = Runtime.getRuntime().exec(path);
        Log.d(TAG, "exec(" + path + ")");


        BufferedReader reader = new BufferedReader(new InputStreamReader(nativeApp.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
            //Log.i(TAG, "output: " + new String(buffer));
            output.append(buffer, 0, read);
        }
        reader.close();

        // Waits for the command to finish.
        nativeApp.waitFor();

        String nativeOutput =  output.toString();

        return nativeOutput;
    }

}
