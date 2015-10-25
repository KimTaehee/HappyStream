package com.sec.kbssm.happystream;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ExecuteShell { //TODO: activity ...?
    private static final String TAG = ExecuteShell.class.getSimpleName();

    private static RunAsyncTask mAsyncTask;

    public static String reconfigureServer(int resize, String path) {
        String result = null;
        Log.v("User logged", "manual_operation_btn_squid_reconfigure");
        try{
            result = run("sh " + path + "/resize_squid.sh "+resize);
            Log.i("User Logged", result);

        }catch(Exception e){
            Log.e("User Logged", e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public static String runServer(String path){
        String result = null;
        Log.v("User logged", "run Squid Server start!");

        try {
            Log.v("User logged", "manual_operation_btn_set_iptables");
            result = run("sh " + path + "/setip.sh");
            Log.i("User Logged",result);

            Log.v("User logged", "manual_operation_btn_squid_z");
            result = run("sh " + path + "/squidz.sh");
            Log.i("User Logged",result);

            Log.v("User logged", "manual_operation_btn_squid_k_parse");
            result = run("sh " + path + "/squidkparse.sh");
            Log.i("User Logged",result);

            Log.v("User logged", "manual_operation_btn_squid. start new asynctask.");
            mAsyncTask = new RunAsyncTask();
            mAsyncTask.execute("sh " + path + "/squidrun.sh");

        } catch (Exception e) {
            Log.e("User Logged", e.getMessage());
            e.printStackTrace();
        }

        Log.v("User logged", "run Squid Server end!");
        return result;
    }
    public static String killServer(String path){
        String result = null;

        Log.v("User logged", "kill server start");
        try{
            result = run("sh " + path + "/killserver.sh");
            Log.i("User Logged", result);

        }catch(Exception e){
            Log.e("User Logged", e.getMessage());
            e.printStackTrace();
        }
        Log.v("User logged", "kill server end");
        return result;
    }
    public static String cleanCache(String path){
        String result = null;
        Log.v("User logged", "clean cache start");
        try{
            result = run("sh " + path + "/cleancache.sh");
            Log.i("User Logged", result);

        }catch(Exception e){
            Log.e("User Logged", e.getMessage());
            e.printStackTrace();
        }
        Log.v("User logged", "clean cache end");

        return result;
    }

    public static String checkServerRunning(String path){
        String result = null;
        Log.v("User logged", "checkServerRunning() called");
        try{
            result = run("sh " + path + "/pssquid.sh");
            Log.i("User Logged", result);

        }catch(Exception e){
            Log.e("User Logged", e.getMessage());
            e.printStackTrace();
        }
        Log.v("User logged", "checkServerRunning()");

        return result;
    }


    public static String run(String path) throws Exception {
        Process nativeApp = Runtime.getRuntime().exec(path);
        Log.d("User Logged", "exec(" + path + ")");


        BufferedReader reader = new BufferedReader(new InputStreamReader(nativeApp.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
            Log.i("User Logged",new String(buffer));
            output.append(buffer, 0, read);
        }
        reader.close();

        // Waits for the command to finish.
        nativeApp.waitFor();

        String nativeOutput =  output.toString();

        return nativeOutput;
    }

    private static class RunAsyncTask extends AsyncTask<String, Void, String> {
        private String result;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.v("User Logged", "onPreExecute() invoked!");
        }

        @Override
        protected String doInBackground(String... params) {
            Log.v("User Logged", "doInBackground() invoked!");

            try {
                result = run(params[0]);
            } catch (Exception e) {
                Log.e("User Logged", e.getMessage());
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.v("User Logged", "onPostExecute() invoked!");

            if(result != null){
                Log.v("User Logged", "result = " + result);
            }

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.v("User Logged", "onCancelled() invoked!");
        }

    }
}