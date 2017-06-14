package com.example.lenovo.test;

import android.util.Log;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import org.json.JSONArray;
import android.os.AsyncTask;
import android.widget.Button;
import android.widget.Switch;
import android.content.Intent;
import org.json.JSONException;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.CardView;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
  Switch override;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    Intent intent = getIntent();
    final boolean OFFLINE = intent.getStringExtra("error").contains("offline");

    final CardView status_card = (CardView) findViewById(R.id.status_card);
    final CardView help_card = (CardView) findViewById(R.id.help_card);

    TextView status_title = (TextView) findViewById(R.id.status_title);
    TextView status_subtext = (TextView) findViewById(R.id.status_subtext);

    Button help_button = (Button) findViewById(R.id.help_button);

    help_button.setText("Okay");
    help_button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // home press or background run
        // Setup ongoing notifications
      }
    });

    status_card.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(OFFLINE) {
          new SyncData().execute();
        }else {
          status_card.setVisibility(View.GONE);
        }
      }
    });

    if(OFFLINE) {
      status_subtext.setText("Could not connect to: "+Config.HOST.substring(0, Config.HOST.length() - 4)+"\n\nTap this card to try again");
    }else {
      status_title.setText("Sync Complete");
      status_subtext.setText("Waiting for incoming messages\n\nTap to dismiss card");
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    override = (Switch) menu.findItem(R.id.toolbar_switch).getActionView().findViewById(R.id.switch_override);
    return super.onCreateOptionsMenu(menu);
  }

  public static class SyncData extends AsyncTask<Void, Void, Void> {
    @Override
    protected void onPreExecute() {
      Log.e("ASync", "Syncing Data...");
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      HttpHandler httpHandler = new HttpHandler();

      String admins_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=admin");
      String groups_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=group");
      String events_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=event");
      String teachers_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=teacher");
      String students_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=student");
      String keywords_json = httpHandler.makeServiceCall(Config.HOST + "/get.php?t=keyword");

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
    }
  }
}
