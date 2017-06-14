package com.example.lenovo.test;

import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import org.json.JSONArray;
import java.net.URLEncoder;
import android.os.AsyncTask;
import android.content.Intent;
import org.json.JSONException;
import android.content.Context;
import android.widget.TextView;
import android.net.NetworkInfo;
import android.widget.ProgressBar;
import android.net.ConnectivityManager;
import java.io.UnsupportedEncodingException;
import android.support.v7.app.AppCompatActivity;

public class SendActivity extends AppCompatActivity {
  private Handler handler = new Handler();
  private SMSSender ss = new SMSSender();
  private String log_url = "";

  private ProgressBar sendingBar;
  private TextView sendProgressText, processText, contentText;
  private int sendProgress = 0;

  private static boolean DEBUG = false;
  private static boolean CONNECTION_OK = false;
  private static boolean wifiConnected = false;
  private static boolean mobileConnected = false;

  private String message = "", keyword = "", id ="";
  private Boolean demo = false, TASK_COMPLETE = false;
  private String code = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_send);

    sendingBar = (ProgressBar) findViewById(R.id.sendingBar);
    processText = (TextView) findViewById(R.id.sendingTitle);
    contentText = (TextView) findViewById(R.id.contents);
    sendProgressText = (TextView) findViewById(R.id.sendProgressText);

    Intent intent = getIntent();
    Bundle bd = intent.getExtras();

    if(bd != null) {
      message = intent.getStringExtra("message");
      keyword = intent.getStringExtra("keyword");
      id = intent.getStringExtra("id");
      contentText.setText(keyword + " students");
      processText.setText("Connecting to " + Config.HOST);
      if(message.contains("<space>")) {
        code = message.substring(message.lastIndexOf("<space>") - 4, message.lastIndexOf("<space>"));
      }
    }

    checkNetworkConnection();
    processText.setText("Fetching");
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(2000);
        }catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();
    new fetchData().execute();
    if(TASK_COMPLETE) {
      Log.e("TAG", "ASDASD");
    }
  }


  private void checkNetworkConnection() {
    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeInfo = connMgr.getActiveNetworkInfo();
    if (activeInfo != null && activeInfo.isConnected()) {
      wifiConnected = activeInfo.getType() == ConnectivityManager.TYPE_WIFI;
      mobileConnected = activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
      if(wifiConnected) {
        CONNECTION_OK = true;
      } else if (mobileConnected){
        CONNECTION_OK = false;
      }
    } else {
      CONNECTION_OK = false;
    }
  }

  private class fetchData extends AsyncTask<Void, Void, Void> {

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
      SampleData.DEMO_TARGETS = null;
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      String params = null;
      HttpHandler httpHandler = new HttpHandler();

      if(keyword.length() == 3 && !keyword.equals("ALL")) {
        String course = keyword.substring(0,2);
        String year = keyword.substring(2);

        if(keyword.contains("YR")) {
          params = "/get.php?t=student&y="+year;
        }else {
          params = "/get.php?t=student&c="+course;
          if(!year.contains("A")) params += "&y="+year;
        }
      }else if(keyword.length() == 5) {
        params = "/get.php?t=student&i="+keyword;
      }else if(keyword.length() == 6) {
        params = "/get.php?t=member&g="+keyword;
      }else if(keyword.equals("ALL")){
        params = "/get.php?t=student";
      }

      String request_url = Config.HOST + params;
      String students_json = httpHandler.makeServiceCall(request_url);

      if (!students_json.isEmpty()) {
        try {
          JSONArray students_array = new JSONArray(students_json);
          SampleData.DEMO_TARGETS = new String[students_array.length()];

          if(SampleData.DEMO_TARGETS != null) {
            Log.e("SendActivity", "Size:"+SampleData.DEMO_TARGETS.length);
            if(message.contains("<space>")) {
              String count_result = httpHandler.makeServiceCall(Config.HOST + "/count.php?poll=" + code +"&val=" + SampleData.DEMO_TARGETS.length);
              Log.e("TargetUpdate", count_result);
            }

            for(int index = 0; index < students_array.length(); index++) {
              SampleData.DEMO_TARGETS[index] = students_array.get(index).toString();
            }
          }
        } catch (JSONException e) { e.printStackTrace(); }
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      sendSMS();
    }
  }

  private void sendSMS() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(3000);
        }catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();

    if(SampleData.DEMO_TARGETS != null) {
      processText.setText("Sending");
      contentText.setText('"'+message+'"');
      sendingBar.setMax(SampleData.DEMO_TARGETS.length);
      sendingBar.setIndeterminate(false);
      sendProgress = 0;
    }

    new Thread(new Runnable() {
      @Override
      public void run() {
        while (sendProgress < SampleData.DEMO_TARGETS.length) {
          try{
            handler.post(new Runnable() {
              @Override
              public void run() {
                sendingBar.setProgress(sendProgress);
                sendProgressText.setText("Sent " + sendProgress + " of " + SampleData.DEMO_TARGETS.length);
              }
            });
            ss.sendSMS(getBaseContext(), SampleData.DEMO_TARGETS[sendProgress], message);
            String phone = "";

            try {
              phone = URLEncoder.encode(SampleData.DEMO_TARGETS[sendProgress], "utf-8");
            }catch (UnsupportedEncodingException e) {
              e.printStackTrace();
            }

            if(id != null) {
              log_url = Config.HOST + "/sent.php?phone=" + phone + "&id=" + id;
              new log().execute();
              Log.e("SEND LOG", log_url);
            }

            sendProgress++;
            Thread.sleep(3000);
          }catch(InterruptedException e) { e.printStackTrace(); }
        }
        finish();
        TASK_COMPLETE = true;
      }
    }).start();
  }

  private class log extends AsyncTask<Void, Void, Void> {
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      HttpHandler httpHandler = new HttpHandler();
      httpHandler.makeServiceCall(log_url);
      log_url = "";
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
    }
  }
}
