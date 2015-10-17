package com.sec.kbssm.happystream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;



public class MainActivity extends Activity implements OnClickListener {
    //private RelativeLayout mLayoutOnoff;
    private TextView mTvAccessLog;
    private TextView mTvSaveRatio;
    private TextView mTvSaveStatus;
    private TextView mTvCacheRatio;
    private TextView mTvCacheStatus;
    private TextView cache_dir_size;
    private ImageView onBtn, offBtn, trashBtn, trashBtn_off;
    private SeekBar seekbar;
    //String result;
    boolean server_switch;
    SharedPreferences pref;
    //	private Scanner mSc = null;
    //private LogMonitoringAsyncTask mLogMonitor;
    DrawGraph drawGraph;
    ExcuteShell shell;

    private NotificationManager nm = null;

    private int backup_size, resize;
    boolean alert;
    String path="";

    private static final String ACTION_KEY_TYPE = "action";
    private static final String ACTION_KEY_VALUE = "value";

    private static final int ACTION_TYPE_SETTEXT_SAVE_RATIO = 200;
    private static final int ACTION_TYPE_SETTEXT_SAVE_STATUS = 201;
    private static final int ACTION_TYPE_SETTEXT_CACHE_RATIO = 300;
    private static final int ACTION_TYPE_SETTEXT_CACHE_STATUS = 301;


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();

            switch(data.getInt(ACTION_KEY_TYPE)) {

                case ACTION_TYPE_SETTEXT_SAVE_RATIO:
                    Log.d("User Logged","ACTION_TYPE_SETTEXT_SAVE_RATIO");
                    mTvSaveRatio.setText(data.getString(ACTION_KEY_VALUE));

                    break;
                case ACTION_TYPE_SETTEXT_SAVE_STATUS:
                    Log.d("User Logged","ACTION_TYPE_SETTEXT_SAVE_STATUS");
                    mTvSaveStatus.setText(data.getString(ACTION_KEY_VALUE));
                    break;
                case ACTION_TYPE_SETTEXT_CACHE_RATIO:
                    Log.d("User Logged","ACTION_TYPE_SETTEXT_CACHE_RATIO");
                    mTvCacheRatio.setText(data.getString(ACTION_KEY_VALUE));
                    break;
                case ACTION_TYPE_SETTEXT_CACHE_STATUS:
                    Log.d("User Logged","ACTION_TYPE_SETTEXT_CACHE_STATUS");
                    mTvCacheStatus.setText(data.getString(ACTION_KEY_VALUE));
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_main);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        LinearLayout layBody = (LinearLayout) inflater.inflate(R.layout.activity_main, null);
        LinearLayout infoview = (LinearLayout) layBody.findViewById(R.id.lay_arc);
        drawGraph = new DrawGraph(this);
        shell = new ExcuteShell(this);
        infoview.addView(drawGraph);
        setContentView(layBody);

        seekbar = (SeekBar) findViewById(R.id.seekbar);
        onBtn = (ImageView) findViewById(R.id.switch_server_on);
        onBtn.setOnClickListener(this);
        offBtn = (ImageView) findViewById(R.id.switch_server_off);
        offBtn.setOnClickListener(this);
        trashBtn = (ImageView) findViewById(R.id.clean_cache);
        trashBtn_off = (ImageView) findViewById(R.id.clean_cache_gray);
        server_switch = false;
        trashBtn_off.setVisibility(View.INVISIBLE);
        cache_dir_size = (TextView) findViewById(R.id.dir_size_text);
        trashBtn.setOnClickListener(this);
        trashBtn_off.setOnClickListener(this);
        cache_dir_size.setOnClickListener(this);

        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        mTvSaveRatio = (TextView) findViewById(R.id.main_tv_save_ratio);
        mTvSaveStatus = (TextView) findViewById(R.id.main_tv_save_status);
        mTvCacheRatio = (TextView) findViewById(R.id.main_tv_cache_ratio);
        mTvCacheStatus = (TextView) findViewById(R.id.main_tv_cache_status);

        pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        if(pref.getString("file", "").equals("on")){

            Log.d("User Logged","dir_size : " + pref.getString("dir_size", ""));
            seekbar.setProgress(Integer.parseInt(pref.getString("dir_size", ""))-50);//-50?
            cache_dir_size.setText(pref.getString("dir_size", ""));
            backup_size = Integer.parseInt(pref.getString("dir_size", ""));

        }else{
            //없는 거기 때문에 conf 파일에서 내용을 읽어와야함... 근데 이건 내가 알고 있는 값이니까 값을 줘도 될 것 같아.
            Log.d("User Logged","dir_size input : " + 500);
            resize = 500;
            backup_size = 500;
            editor.putString("dir_size", "500");
            editor.commit();
            seekbar.setProgress(resize-50);//-50?
            cache_dir_size.setText("500");

        }


        //mLayoutOnoff = (RelativeLayout) findViewById(R.id.main_layout_onoff);
        //mLayoutOnoff.setOnClickListener(this);
        //mTvAccessLog = (TextView) findViewById(R.id.main_tv_access_log);

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d("User Logged","seekbar change start : " + i);
                resize = i+50;
                cache_dir_size.setText(""+resize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d("User Logged","seekbar touch start");

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("User Logged","seekbar touch end");
                openAlert_Progress();
            }
        });
        path = this.getFilesDir().getPath();
        checkFileExist();

    }
    //checkFileExist
    private void checkFileExist(){
        Log.d("User Logged", "preference start");
        //SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        if(!pref.getString("file", "").equals("on")){
            //이미 깔려있지 않으면 파일을 복사하고, 압축을 해제해 준다.
            Log.v("User logged", "test file exist ...File is : "+ pref.getString("file", ""));
            Log.v("User logged", "manual_operation_btn_copy_squid_from_assets");
            Log.v("User logged", "this.getFilesDir().getPath() : " + this.getFilesDir().getPath());
            copyFile("zero", this.getFilesDir().getPath() + "/zero", this);
            copyFile("squid.tar", this.getFilesDir().getPath() + "/squid.tar", this);
            copyFile("setip.sh", this.getFilesDir().getPath() + "/setip.sh", this);
            copyFile("squidkparse.sh", this.getFilesDir().getPath() + "/squidkparse.sh", this);
            copyFile("squidrun.sh", this.getFilesDir().getPath() + "/squidrun.sh", this);
            copyFile("squidz.sh", this.getFilesDir().getPath() + "/squidz.sh", this);
            copyFile("logbackup.sh", this.getFilesDir().getPath() + "/logbackup.sh", this);
            copyFile("resize_squid.sh", this.getFilesDir().getPath() + "/resize_squid.sh", this);
            copyFile("killserver.sh", this.getFilesDir().getPath() + "/killserver.sh", this);
            try {
                Log.i("User Logged",shell.run("chmod -R 775 " + this.getFilesDir().getPath()));

            } catch (Exception e) {
                Log.e("User Logged", e.getMessage());
                e.printStackTrace();
            }

            Log.v("User logged", "manual_operation_btn_untar_squid");
            try {
               //result =
                Log.i("User Logged",shell.run("busybox tar xvf " + this.getFilesDir().getPath() + "/squid.tar -C " + this.getFilesDir().getPath()));
                //result =
                Log.i("User Logged",shell.run("chmod -R 775 " + this.getFilesDir().getPath()));

            } catch (Exception e) {
                Log.e("User Logged", e.getMessage());
                e.printStackTrace();
            }

            editor.putString("file", "on");
            editor.commit();

        }
        Log.d("User Logged","preference end");

    }

    //private static void copyFile(String assetPath, String localPath, Context context) {
    private static void copyFile(String assetPath, String localPath, Context context) {
        try {
            InputStream in = context.getAssets().open(assetPath);
            FileOutputStream out = new FileOutputStream(localPath);
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("User logged", "onResume() invoked. execute thread.");

        ReadAccesslog();
        //mLogMonitor = new LogMonitoringAsyncTask();
        //mLogMonitor.execute(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v("User logged", "onPause() invoked. stop running thread.");
        //boolean result = mLogMonitor.cancel(true);
        //Log.d("User logged", "mLogMonitor.cancel(true) result: " + result);

    }

    @Override
    public void onClick(View v) {
        Log.v("User logged", "onClick() invoked.");
        switch(v.getId()){
            case R.id.switch_server_on :

                shell.killServer(this.getFilesDir().getPath());
                onBtn.setVisibility(View.INVISIBLE);
                offBtn.setVisibility(View.VISIBLE);
                server_switch = false;
                trashBtn.setVisibility(View.VISIBLE);
                trashBtn_off.setVisibility(View.INVISIBLE);
                break;

            case R.id.switch_server_off :

                shell.runServer(this.getFilesDir().getPath());
                offBtn.setVisibility(View.INVISIBLE);
                onBtn.setVisibility(View.VISIBLE);
                server_switch = true;
                trashBtn.setVisibility(View.INVISIBLE);
                trashBtn_off.setVisibility(View.VISIBLE);

                notification();

                break;

            case R.id.clean_cache :
                if(!server_switch) {//서버가 꺼져있을때는 cache를 비울 수 있음.
                    //
                    openAlert_CleanCache();
                    if(alert){
                        shell.cleanCache(this.getFilesDir().getPath());
                        Toast.makeText(getApplicationContext(), "캐쉬를 비웠습니다.", Toast.LENGTH_SHORT).show();
                    }

                }

                break;
            case R.id.clean_cache_gray :
                Log.v("User logged", "onClick() : cache_gray btn Pressed");
                Toast.makeText(getApplicationContext(), "서버가 켜져 있는 동안은 캐쉬를 지울 수 없습니다.", Toast.LENGTH_SHORT).show();
                break;
        }
        //startActivity(new Intent(this, ManualOperationActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        Log.v("User logged", "In onDestroy ..put dir_size -> "+ resize);
        editor.putString("dir_size", ""+resize);
        editor.commit();

        if(server_switch)
            shell.killServer(this.getFilesDir().getPath());
        nm.cancel(100);
    }

    private void sendActionMsg(int action, String value) {
        Message msg = mHandler.obtainMessage();

        Bundle bundle = new Bundle();
        bundle.putInt(ACTION_KEY_TYPE, action);
        bundle.putString(ACTION_KEY_VALUE, value);

        msg.setData(bundle);
        mHandler.sendMessage(msg);


    }

    private void ReadAccesslog() {
        int mHitBytes;
        int mMissBytes;
        int current_mHitBytes = 0;
        int current_mMissBytes = 0;
        int maxCacheBytes = 0;
        int current_currCacheBytes = 0;

        Log.v("User Logged", "in ReadAccesslog()");
        mHitBytes = 0;
        mMissBytes = 0;

        BufferedReader br = null;
        StringBuilder sb = null;
        try {
            //read a log files
            br = new BufferedReader(new FileReader(
                    this.getFilesDir().getPath() + "/var/logs/access.log"));

            sb = new StringBuilder();
            String line;// = br.readLine();
            //Log.v("User Logged",line);

            int uiUpdateLineCnt = 0;
            while ((line = br. readLine()) != null) {
                //Log.v("User Logged", "line read completed: line: " + line);
                sb.append(line);
                sb.append(System.getProperty("line.separator"));

                //sum bytes as state type (hit,miss)

                int idxOfLastQuote = line.lastIndexOf("\""); //TODO: is this right?
                String substring = line.substring(idxOfLastQuote);

                StringTokenizer st = new StringTokenizer(substring);
                //Log.v("User Logged", "substring: " + substring);
                int cnt = 0;
                String currState = null;
                int currBytes = 0;
                while(st.hasMoreTokens()){
                    String token = st.nextToken();

                    if(cnt==2) {
                        currBytes = Integer.parseInt(token);
                        //Log.v("User Logged", "currBytes : " + currBytes);
                    } else if(cnt==3) {
                        currState = token;
                        //Log.v("User Logged", "currState : " + currState);
                    }
                    cnt++;
                }
                if(currState.contains("HIT")) {
                    mHitBytes += currBytes;
                } else {
                    mMissBytes += currBytes;
                }

                //uiUpdateLineCnt++;
                //if(uiUpdateLineCnt % 10 == 0) {
                    //Log.v("User Logged", "uiUpdateLineCnt: " + uiUpdateLineCnt);
                    //print result on ui
                    //String result

                    //uiUpdateLineCnt = 0;
                //}
            }
            String result = ManualOperationActivity.run("busybox du -s " + this.getFilesDir().getPath() + "/var/logs/cache");
            if(current_mHitBytes < mHitBytes || current_mMissBytes < mMissBytes) {
                if(current_mHitBytes < mHitBytes) {
                    current_mHitBytes = mHitBytes;
                }
                if(current_mMissBytes < mMissBytes){
                    current_mMissBytes = mMissBytes;
                }
                //sendActionMsg(ACTION_TYPE_SETTEXT_LOG, sb.toString());
                Log.v("User Logged", "current_mHitBytes : " + current_mHitBytes + "current_mMissBytes : " + current_mMissBytes);
                Log.v("User Logged", "mHitBytes : " + mHitBytes + "mMissBytes : " + mMissBytes);
                int saveRatio = (int) ((double) current_mHitBytes / ((double) current_mHitBytes + (double) current_mMissBytes) * 100.0);
                sendActionMsg(ACTION_TYPE_SETTEXT_SAVE_RATIO, "" + saveRatio + "%");
                sendActionMsg(ACTION_TYPE_SETTEXT_SAVE_STATUS,
                        formatSize(current_mHitBytes) + " / " + formatSize(current_mHitBytes + current_mMissBytes));
                drawGraph.setmSweep(saveRatio);
            }

            int currCacheBytes = Integer.parseInt(result.substring(0, result.indexOf('\t'))) * 1000;
            Log.v("User Logged", "maxCacheBytes : " + maxCacheBytes + "current_currCacheBytes : " + current_currCacheBytes);
            if(maxCacheBytes != backup_size || current_currCacheBytes != currCacheBytes) {

                //Log.i("User Logged", "cache size du: " + currCacheBytes);
                //Log.i("User Logged", "current_maxCacheBytes : " + maxCacheBytes);

                if(maxCacheBytes != backup_size) {
                    maxCacheBytes = backup_size;
                }
                if(current_currCacheBytes != currCacheBytes){
                    current_currCacheBytes = currCacheBytes;
                }

                sendActionMsg(ACTION_TYPE_SETTEXT_CACHE_RATIO, "" + (int) (((double) current_currCacheBytes / (double) maxCacheBytes)) / 10000 + "%");//* 100.0
                sendActionMsg(ACTION_TYPE_SETTEXT_CACHE_STATUS,
                        formatSize(current_currCacheBytes) + "/" + maxCacheBytes+"MB");
                Log.i("User Logged", "Call Sweep2() : " + current_currCacheBytes / (maxCacheBytes * 10000));
                drawGraph.setmSweep2(current_currCacheBytes / (maxCacheBytes * 10000));

            }
            Log.v("User Logged", "while is null? : " + line);

        } catch(Exception e) {
            Log.e("User Logged",e.getMessage());
        } finally {
            try {
                //원래는 여기에 setUI 내용이 있었음.
                br.close();
            } catch (Exception e) {
//						Log.e("User Logged",e.getMessage());
                e.printStackTrace();
            }
        }
        /*
        try {
            //refresh cycle
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e("User Logged",e.getMessage());
            e.printStackTrace();
        }*/


    }
    //reconfigure
    //~
    //cleancache_

    //Task()


    //run

    private String formatSize(int bytes) {
        String result;
        if(bytes<1000) { //B
            result = "" + bytes + "B";
        } else if(bytes<1000000) { //KB
            result = "" + (bytes/1000) + "KB";
        } else if(bytes<1000000000) { //MB
            result = "" + (bytes/1000000) + "MB";
        } else if(bytes<Integer.MAX_VALUE){ //GB
            result = "" + (bytes/1000000000) + "GB";
        } else { //default
            result = "" + bytes + "B";
        }
        return result;
    }
    //RunAsyncTask

    private void openAlert_Progress(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle(this.getTitle());
        alertDialogBuilder.setMessage("캐싱 용량을 바꿀까요?");
        // set positive button: Yes message
        alertDialogBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                //확인 버튼 눌림
                //reconfigure
                backup_size = resize;
                dialog.dismiss();
                if(server_switch) {
                    Log.i("User Logged", "openAlert() : path " + path);
                    shell.reconfigureServer(resize, path);
                }
            }
        });
        alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog,int id) {
                // cancel the alert box and put a Toast to the user
                //취소버튼 눌림.
                seekbar.setProgress(backup_size);
                cache_dir_size.setText(""+backup_size);
                dialog.cancel();
            }

        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void openAlert_CleanCache(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle(this.getTitle());
        alertDialogBuilder.setMessage("캐쉬를 지울까요?");
        // set positive button: Yes message

        alertDialogBuilder.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int id) {
                //확인 버튼 눌림
                alert = true;
            }
        });
        alertDialogBuilder.setNegativeButton("No",new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog,int id) {
                // cancel the alert box and put a Toast to the user
                //취소버튼 눌림.
                alert = false;
                dialog.cancel();
            }

        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void notification() {

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);

        Notification noti = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                        //.setTicker("New Message2")
                .setContentTitle("HappyStream 가동중")
                        //.setContentText("Other Activity")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pIntent)
                .build();
        // 알림 방식 지정
        noti.defaults |= Notification.DEFAULT_ALL;
        noti.flags |= Notification.FLAG_INSISTENT;
        // 알림 시작
        nm.notify(100, noti);//NOTI_ID
    }

}
