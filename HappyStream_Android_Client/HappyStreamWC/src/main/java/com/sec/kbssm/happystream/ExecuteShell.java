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
        Log.v(TAG, "manual_operation_btn_squid_reconfigure");
        try{
            result = run("sh " + path + "/resize_squid.sh "+resize);
            Log.i(TAG, result);

        }catch(Exception e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public static String runServer(String path){
        String result = null;
        Log.v(TAG, "run Squid Server start!");

        try {
            Log.v(TAG, "manual_operation_btn_set_iptables");
            result = run("sh " + path + "/setip.sh");
            Log.i(TAG,result);

            Log.v(TAG, "manual_operation_btn_squid_z");
            result = run("sh " + path + "/squidz.sh");
            Log.i(TAG,result);

            Log.v(TAG, "manual_operation_btn_squid_k_parse");
            result = run("sh " + path + "/squidkparse.sh");
            Log.i(TAG,result);

            Log.v(TAG, "manual_operation_btn_squid. start new asynctask.");
            mAsyncTask = new RunAsyncTask();
            mAsyncTask.execute("sh " + path + "/squidrun.sh");

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        Log.v(TAG, "run Squid Server end!");
        return result;
    }
    public static String killServer(String path){
        String result = null;

        Log.v(TAG, "kill server start");
        try{
            result = run("sh " + path + "/killserver.sh");
            Log.i(TAG, result);

        }catch(Exception e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        Log.v(TAG, "kill server end");
        return result;
    }
    public static String cleanCache(String path){
        String result = null;
        Log.v(TAG, "clean cache start");
        try{
            result = run("sh " + path + "/cleancache.sh");
            Log.i(TAG, result);

        }catch(Exception e){
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        Log.v(TAG, "clean cache end");

        return result;
    }

    public static String run(String path) throws Exception {
        Process nativeApp = Runtime.getRuntime().exec(path);
        Log.d(TAG, "exec(" + path + ")");


        BufferedReader reader = new BufferedReader(new InputStreamReader(nativeApp.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
            Log.i(TAG, "output: " + new String(buffer));
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
            Log.v(TAG, "onPreExecute() invoked!");
        }

        @Override
        protected String doInBackground(String... params) {
            Log.v(TAG, "doInBackground() invoked!");

            try {
                result = run(params[0]);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.v(TAG, "onPostExecute() invoked!");

            if(result != null){
                Log.v(TAG, "result = " + result);
            }

        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.v(TAG, "onCancelled() invoked!");
        }

    }
}