package com.example.lenovo.test;

import java.util.Timer;
import android.os.Build;
import android.util.Log;
import android.os.IBinder;
import org.json.JSONArray;
import android.app.Service;
import org.json.JSONObject;
import java.util.TimerTask;
import android.os.AsyncTask;
import android.widget.Toast;
import org.json.JSONException;
import android.content.Intent;

import static com.example.lenovo.test.Config.HOST;

public class PendingService extends Service {
  private Timer timer = new Timer();
  private SMSSender ss = new SMSSender();

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        new MainActivity.SyncData().execute();
        new fetchPending().execute();
      }
    }, 0, 6000000);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
  }

  private class fetchPending extends AsyncTask<Void, Void, Void> {
    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      HttpHandler httpHandler = new HttpHandler();
      String request_url = HOST + "/pending.php?device=" + Build.SERIAL;
      String pending_accounts = httpHandler.makeServiceCall(request_url);

      if (!pending_accounts.isEmpty()) {
        if(!pending_accounts.contains("null")) {
          Log.e("Pending", pending_accounts);
          try {
            JSONArray pending_array = new JSONArray(pending_accounts);

            for (int index = 0; index < pending_array.length(); index++) {
              JSONObject c = (JSONObject) pending_array.get(index);

              String id = c.getString("id");
              String code = c.getString("code");
              String phone = c.getString("phone_number");

              String update_url = request_url+"&d="+id;
              httpHandler.makeServiceCall(update_url);

              String message = "Your verification code is " + code;
              ss.sendSMS(getApplicationContext(), phone, message);
            }
          } catch (final JSONException e) { e.printStackTrace(); }
        }
      }
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
    }

  }

}
