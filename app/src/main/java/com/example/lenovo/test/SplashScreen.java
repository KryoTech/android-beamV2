package com.example.lenovo.test;

import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import org.json.JSONArray;
import android.os.AsyncTask;
import android.app.Activity;
import android.content.Intent;
import org.json.JSONException;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;

public class SplashScreen extends Activity {
  private String error = "";
  private boolean server = false, sms = false, network = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    if(isOnline()) {
      error = "You are offline";
      new PrefetchData().onPostExecute(null);
    }else {
      new PrefetchData().execute();
    }
  }

  public boolean isOnline() {
    ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    return (networkInfo != null && networkInfo.isConnected());
  }

  private class PrefetchData extends AsyncTask<Void, Void, Void> {
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      HttpHandler httpHandler = new HttpHandler();

      String teachers_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=teacher");
      String students_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=student");
      String keywords_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=keyword");
      String admins_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=admin");
      String groups_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=group");
      String events_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=event");
      Log.e("Teachers", teachers_json);
      Log.e("Students", students_json);
      Log.e("Keywords", keywords_json);
      Log.e("Admins", admins_json);
      Log.e("Groups", groups_json);
      Log.e("Events", events_json);

      try {
        JSONArray code_array = new JSONArray(groups_json);
        JSONArray event_array = new JSONArray(events_json);
        JSONArray ajson_array = new JSONArray(admins_json);
        JSONArray sjson_array = new JSONArray(students_json);
        JSONArray tjson_array = new JSONArray(teachers_json);
        JSONArray keyword_array = new JSONArray(keywords_json);

        SampleData.KEYWORDS = new String[keyword_array.length()];
        SampleData.GROUP_CODES = new String[code_array.length()];
        SampleData.EVENT_CODES = new String[event_array.length()];
        SampleData.ADMIN_NUMBERS = new String[ajson_array.length()];
        SampleData.TEACHER_NUMBERS = new String[tjson_array.length()];
        SampleData.STUDENT_NUMBERS = new String[sjson_array.length()];

        for (int index = 0; index < keyword_array.length(); index++)
          SampleData.KEYWORDS[index] = keyword_array.get(index).toString();
        for (int index = 0; index < code_array.length(); index++)
          SampleData.GROUP_CODES[index] = code_array.get(index).toString();
        for (int index = 0; index < event_array.length(); index++)
          SampleData.EVENT_CODES[index] = event_array.get(index).toString();
        for (int index = 0; index < ajson_array.length(); index++)
          SampleData.ADMIN_NUMBERS[index] = ajson_array.get(index).toString();
        for (int index = 0; index < tjson_array.length(); index++)
          SampleData.TEACHER_NUMBERS[index] = tjson_array.get(index).toString();
        for (int index = 0; index < sjson_array.length(); index++)
          SampleData.STUDENT_NUMBERS[index] = sjson_array.get(index).toString();
      }catch (JSONException e) {
        e.printStackTrace();
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          Intent i = new Intent(SplashScreen.this, MainActivity.class);
          i.putExtra("network", network);
          i.putExtra("server", server);
          i.putExtra("error", error);
          i.putExtra("sms", sms);
          if(!error.contains("offline")) {
            startService(new Intent(SplashScreen.this, PendingService.class));
          }
          startActivity(i);
          finish();
        }
      }, 2000);
    }
  }
}