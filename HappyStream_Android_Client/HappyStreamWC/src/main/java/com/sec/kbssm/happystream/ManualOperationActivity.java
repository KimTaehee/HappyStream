package com.sec.kbssm.happystream;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class ManualOperationActivity extends Activity implements OnClickListener{
	private Button mBtnCopyFromAssets;
	private Button mBtnSetIptables;
	private Button mBtnUntarSquid;
	private Button mBtnSquidKParse;
	private Button mBtnSquidZ;
	private Button mBtnSquidNCd1;
    private Button mBtnReconfigure;

	private RunAsyncTask mAsyncTask;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manual_operation);
		
		mBtnCopyFromAssets = (Button) findViewById(R.id.manual_operation_btn_copy_squid_from_assets);
		mBtnCopyFromAssets.setOnClickListener(this);
		mBtnSetIptables = (Button) findViewById(R.id.manual_operation_btn_set_iptables);
		mBtnSetIptables.setOnClickListener(this);
		mBtnUntarSquid = (Button) findViewById(R.id.manual_operation_btn_untar_squid);
		mBtnUntarSquid.setOnClickListener(this);
		mBtnSquidKParse = (Button) findViewById(R.id.manual_operation_btn_squid_k_parse);
		mBtnSquidKParse.setOnClickListener(this);
		mBtnSquidZ = (Button) findViewById(R.id.manual_operation_btn_squid_z);
		mBtnSquidZ.setOnClickListener(this);
		mBtnSquidNCd1 = (Button) findViewById(R.id.manual_operation_btn_squid);
        mBtnSquidNCd1.setOnClickListener(this);
        mBtnReconfigure = (Button) findViewById(R.id.manual_operation_btn_squid_reconfigure);
        mBtnReconfigure.setOnClickListener(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.v("User logged", "onResume() invoked.");
				
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.manual_operation, menu);
		return true;
	}

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
	
	
	/**
	 * 
	 * @param path "/data/data/com.yourdomanual_operation.yourapp/nativeFolder/application"
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static String run(String path) throws Exception {
		Process nativeApp = Runtime.getRuntime().exec(path);
		Log.d("User Logged","exec(" + path + ")");


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

	@Override
	public void onClick(View v) {
		String result;
		
		switch (v.getId()) {
		case R.id.manual_operation_btn_copy_squid_from_assets:
			Log.v("User logged", "manual_operation_btn_copy_squid_from_assets");
			
			copyFile("zero", this.getFilesDir().getPath() + "/zero", this);
			copyFile("squid.tar", this.getFilesDir().getPath() + "/squid.tar", this);
			copyFile("setip.sh", this.getFilesDir().getPath() + "/setip.sh", this);
			copyFile("squidkparse.sh", this.getFilesDir().getPath() + "/squidkparse.sh", this);
			copyFile("squidrun.sh", this.getFilesDir().getPath() + "/squidrun.sh", this);
			copyFile("squidz.sh", this.getFilesDir().getPath() + "/squidz.sh", this);
			copyFile("logbackup.sh", this.getFilesDir().getPath() + "/logbackup.sh", this);
            copyFile("resize_squid.sh", this.getFilesDir().getPath() + "/resize_squid.sh", this);
			try {
				result = run("chmod -R 775 " + this.getFilesDir().getPath());
				Log.i("User Logged",result);
				
			} catch (Exception e) {
				Log.e("User Logged", e.getMessage());
				e.printStackTrace();				
			}
			
			break;

		case R.id.manual_operation_btn_set_iptables:
			Log.v("User logged", "manual_operation_btn_set_iptables");
			
			try {
				result = run("sh " + this.getFilesDir().getPath() + "/setip.sh");
				Log.i("User Logged",result);
				
			} catch (Exception e) {
				Log.e("User Logged", e.getMessage());
				e.printStackTrace();				
			}
			
			break;	
			
		case R.id.manual_operation_btn_untar_squid:
			Log.v("User logged", "manual_operation_btn_untar_squid");
			
			try {
				result = run("busybox tar xvf " + this.getFilesDir().getPath() + "/squid.tar -C " + this.getFilesDir().getPath());
				Log.i("User Logged",result);
				result = run("chmod -R 775 " + this.getFilesDir().getPath());
				Log.i("User Logged",result);
				
			} catch (Exception e) {
				Log.e("User Logged", e.getMessage());
				e.printStackTrace();				
			}
			
			break;		
		
		case R.id.manual_operation_btn_squid_k_parse:
			Log.v("User logged", "manual_operation_btn_squid_k_parse");
			
			try {
				result = run("sh " + this.getFilesDir().getPath() + "/squidkparse.sh");
				Log.i("User Logged",result);
				
			} catch (Exception e) {
				Log.e("User Logged", e.getMessage());
				e.printStackTrace();				
			}
			
			break;
		case R.id.manual_operation_btn_squid_z:
			Log.v("User logged", "manual_operation_btn_squid_z");

			try {
				
				result = run("sh " + this.getFilesDir().getPath() + "/squidz.sh");
				Log.i("User Logged",result);
				
			} catch (Exception e) {
				Log.e("User Logged", e.getMessage());
				e.printStackTrace();				
			}
			
			break;

        case R.id.manual_operation_btn_squid_reconfigure:
        Log.v("User logged", "manual_operation_btn_squid_reconfigure");
        try{

            result = run("sh " + this.getFilesDir().getPath() + "/resize_squid.sh 450");
            Log.i("User Logged",result);

            }catch(Exception e){
                Log.e("User Logged", e.getMessage());
                e.printStackTrace();
            }

            break;
		case R.id.manual_operation_btn_squid: //run squid
			Log.v("User logged", "manual_operation_btn_squid. start new asynctask.");

			
			mAsyncTask = new RunAsyncTask();
			mAsyncTask.execute("sh " + this.getFilesDir().getPath() + "/squidrun.sh");
			
			break;
		}
	}
	
	private class RunAsyncTask extends AsyncTask<String, Void, String> {
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
