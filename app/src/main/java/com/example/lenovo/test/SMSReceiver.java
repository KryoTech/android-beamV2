package com.example.lenovo.test;

import android.util.Log;
import android.os.Build;
import android.os.Bundle;
import org.json.JSONObject;
import java.net.URLEncoder;
import android.os.AsyncTask;
import android.content.Intent;
import org.json.JSONException;
import android.content.Context;
import android.telephony.SmsMessage;
import android.content.BroadcastReceiver;
import java.io.UnsupportedEncodingException;

public class SMSReceiver extends BroadcastReceiver {
  private static final String TAG = SMSReceiver.class.getSimpleName();
  HttpHandler httpHandler = new HttpHandler();
  SMSSender ss = new SMSSender();
  private Context context;
  int approved = 2;

  String clean_phone = "";
  String sender = "";
  String event = "", response = "";
  String event_url = "", event_message = "", event_keyword = "", message2 = "";
  String log_url = "", save_url = "", req_url = "", reqKeyword = "", reqMessage ="", reqID = "", reqTeacher = "";

  @Override
  public void onReceive(Context context, Intent intent) {
    this.context = context;
    Bundle bundle = intent.getExtras();
    SmsMessage[] messages = null;
    String phone = "", content = "";

    if (bundle != null) {
      Object[] pdus = (Object[]) bundle.get("pdus");

      messages = new SmsMessage[pdus.length];

      for (int i = 0; i < messages.length; i++) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          String format = bundle.getString("format");
          messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
        } else {
          messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
        }
        phone = messages[i].getOriginatingAddress();
        try {
          if(phone.charAt(0) != '0')
            clean_phone = URLEncoder.encode(phone, "utf-8");
        }catch (UnsupportedEncodingException e) { e.printStackTrace(); }
        content = messages[i].getMessageBody();
      }

      sender = phone;
      String keyword = "", message = "";

      if(content.contains(" ")) {
        keyword = content.substring(0, content.indexOf(" "));
        message = content.substring(content.indexOf(" ") + 1);
      }

      if(isStudent(phone)) {
        Log.e("Student", phone);
        if(keyword.equals("POLL")) {
          Log.e("Student", "Valid Keyword");
          event = message.substring(0, content.indexOf(" "));
          if(studentKeys(event)) {
            Log.e("Student", "RSVP Received");
            response = message.substring(content.indexOf(" ") + 1);
            if(response.length() == 1) {
              // For beam_polls
              save_url = Config.HOST + "/rsvp.php?e=" + event + "&r=" + response;
              // For poll_replies
              log_url = Config.HOST + "/log.php?i=" + clean_phone + "&e=" + event + "&r=" +response;
              new dbAction().execute();
            }
          }else {
            ss.sendSMS(context, phone, "Sorry, I do not recognize that event code");
          }
        }else {
          ss.sendSMS(context, phone, "Sorry, I do not recognize that keyword");
        }
      }else if(isValid(phone)) {
        String TAG =  this.TAG + ":teacher";
        Log.e(TAG, "Teacher phone");
        if(teacherKeys(keyword) || keywordCheck(keyword)) {
          String clean_message = "", utf_phone = "";
          try {
            utf_phone = URLEncoder.encode(phone, "utf-8");
            clean_message = URLEncoder.encode(message, "utf-8");
          }catch (UnsupportedEncodingException e) { e.printStackTrace(); }
          int code = Config.gen();
          save_url = Config.HOST + "/request.php?t=" + utf_phone + "&k=" + keyword + "&m=" + clean_message + "&c=" + code;
          new dbAction().execute();
          ss.sendSMS(context, phone, "Your request has been queued. You will be notified once your request has been processed.");
          ss.sendSMS(context, SampleData.DEMO_ADMIN, "Request from:" + phone + "\n\n" + message + "\n\nReply with:\n" + code + " APPROVE or\n" + code + " REJECT");
        }else {
          ss.sendSMS(context, phone, "Sorry, I do not recognize that keyword");
        }
      }else if(isAdmin(phone)) {
        String TAG =  this.TAG + ":Admin";
        Log.e(TAG, "Admin phone");
        if(message.equals("APPROVE")) {
          approved = 1;
          req_url = Config.HOST + "/getreq.php?code=" + keyword + "&action=approved";
          new dbAction().execute();
        }else if(message.equals("REJECT")) {
          req_url = Config.HOST + "/getreq.php?code=" + keyword + "&action=denied";
          approved = 0;
          new dbAction().execute();
        }else if(keyword.equals("EVENT")) {
          String event_code = message.substring(0, content.indexOf(" ") - 1);
          String group_code = message.substring(content.indexOf(" "), message.length());
          sender = phone;
          event_keyword = group_code;
          event_url = Config.HOST + "/event.php?code=" + event_code;
          new dbAction().execute();
          if(!event_message.isEmpty()) {
            ss.sendSMS(context, phone, event_message);
            ss.sendSMS(context, phone, message2);
          }
        }else {
          ss.sendSMS(context, phone, "Sorry, I don't recognize that keyword");
        }
      }else {
        if(keyword.equals("INFO")) {
          event = message.substring(0, content.indexOf(" ") + 1);
          response = message.substring(content.indexOf(" ") + 2);
          switch (event) {
            case "PHONE":
              if (response.length() == 3) {
                switch (response) {
                  case "MIN":
                    ss.sendSMS(context, phone, "Mintal Campus Contact Numbers:\n\nRegistrar: XXX-XXXX\nCashier: XXX-XXXX");
                    break;
                  case "JAC":
                    ss.sendSMS(context, phone, "Jacinto Campus Contact Numbers:\n\nRegistrar: XXX-XXXX\nCashier: XXX-XXXX");
                    break;
                  case "CAB":
                    ss.sendSMS(context, phone, "Cabantian Campus Contact Numbers:\n\nRegistrar: XXX-XXXX\nCashier: XXX-XXXX");
                    break;
                  default:
                    ss.sendSMS(context, phone, "Sorry, I don't recognize the campus code: " + response + "\n\nValid campus codes: MIN, JAC, CAB");
                    break;
                }
              } else {
                ss.sendSMS(context, phone, "Sorry, I don't recognize the campus code: " + response + "\n\nValid campus codes: MIN, JAC, CAB");
              }
              break;
            default:
              ss.sendSMS(context, phone, "Holy Child College of Davao\n\nEmail: info@holychild.edu.ph\nWebsite: holychild.edu.ph\nFacebook: facebook.com/holychild\nTwitter: @holychilddvo");
              break;
          }
        } else {
          Log.e(TAG+":default", "Unrecognized Keyword");
        }
      }
    }
  }

  private boolean studentKeys(String keyword) {
    if(SampleData.EVENT_CODES != null) {
      for (String current_keyword:SampleData.EVENT_CODES) {
        if(current_keyword.equals(keyword)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean keywordCheck(String keyword) {
    if(SampleData.KEYWORDS != null) {
      for (String tmp_keyword : SampleData.KEYWORDS) {
        if(tmp_keyword.equals(keyword)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean teacherKeys(String keyword) {
    if(SampleData.GROUP_CODES != null) {
      for (String tmp_keyword : SampleData.GROUP_CODES) {
        if(tmp_keyword.equals(keyword)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isAdmin(String phone) {
    if(SampleData.ADMIN_NUMBERS != null) {
      for (String current_phone : SampleData.ADMIN_NUMBERS) {
        if(current_phone.equals(phone)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isValid(String phone) {
    if(SampleData.TEACHER_NUMBERS != null) {
      for (String current_phone : SampleData.TEACHER_NUMBERS) {
        if(current_phone.equals(phone)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isStudent(String phone) {
    if(SampleData.STUDENT_NUMBERS != null) {
      for (String current_phone : SampleData.STUDENT_NUMBERS) {
        if(current_phone.equals(phone)) {
          return true;
        }
      }
    }
    return false;
  }

  private void showSendProgress(String keyword, String message, String id) {
    Intent sendingActivityIntent = new Intent();
    sendingActivityIntent.putExtra("keyword", keyword);
    sendingActivityIntent.putExtra("message", message);
    sendingActivityIntent.putExtra("id", id);
    sendingActivityIntent.setClassName("com.example.lenovo.test", "com.example.lenovo.test.SendActivity");
    sendingActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    this.context.startActivity(sendingActivityIntent);
  }

  private void showSendProgress(String keyword, String message) {
    Intent sendingActivityIntent = new Intent();
    sendingActivityIntent.putExtra("keyword", keyword);
    sendingActivityIntent.putExtra("message", message);
    sendingActivityIntent.setClassName("com.example.lenovo.test", "com.example.lenovo.test.SendActivity");
    sendingActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    this.context.startActivity(sendingActivityIntent);
  }

  private class dbAction extends AsyncTask<Void, Void, Void> {

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... arg0) {
      String log_result = "";
      if(!event_url.isEmpty()) {
        String json = httpHandler.makeServiceCall(event_url);
        try {
          JSONObject event_broadcast = new JSONObject(json);
          String name = event_broadcast.getString("name");
          String code = event_broadcast.getString("code");
          String details = event_broadcast.getString("details");
          String schedule = event_broadcast.getString("schedule");
          String location = event_broadcast.getString("location");
          event_message = "Event Reminder\n\nWhat: " + name + "\nWhen: " + schedule + "\nWhere: " + location + "\nDetails: " + details + "\nTo RSVP, reply with:\nPOLL<space>" + code + "<space>A if attending,\nPOLL<space>" + code + "<space>N if not attending and\nPOLL<space>" + code + "<space>U if undecided";
          Log.e(TAG+":smsEvent", event_message);
        }catch (JSONException e) { e.printStackTrace(); }
      }

      if(!req_url.isEmpty()) {
        String result = httpHandler.makeServiceCall(req_url);
        Log.e(TAG+":process", result);
        try {
          JSONObject broadcast = new JSONObject(result);
          reqKeyword = broadcast.getString("keyword");
          Log.e(TAG, reqKeyword);
          reqMessage = broadcast.getString("message");
          Log.e(TAG, reqMessage);
          reqTeacher = broadcast.getString("teacher");
          Log.e(TAG, reqTeacher);
          reqID = broadcast.getString("id");
          Log.e(TAG, reqID);
          if(approved == 1) {
            // Admin approved broadcast
            ss.sendSMS(context, reqTeacher, "Your request has been approved");
            showSendProgress(reqKeyword, reqMessage, reqID);
          }else {
            // Admin denied broadcast
            ss.sendSMS(context, reqTeacher, "Sorry, your request has been denied");
          }
        }catch (JSONException e) { e.printStackTrace(); }
      }

      String error = "";

      if(!log_url.isEmpty()) {
        // Request that saves action to database
        log_result = httpHandler.makeServiceCall(log_url);
        try {
          JSONObject broadcast = new JSONObject(log_result);
          error = broadcast.getString("error");
          Log.e(TAG + ":send", "Sending to " + sender);
          ss.sendSMS(context, sender, error);
        }catch (Exception e) {
          e.printStackTrace();
        }
      }

      if(!save_url.isEmpty()) {
        // Request that saves to database
        if(log_result.contains("error")) {
          Log.e(TAG+":RSVP", log_result);
        }else {
          String result = httpHandler.makeServiceCall(save_url);
          try {
            JSONObject broadcast = new JSONObject(result);
            error = broadcast.getString("error");
            Log.e(TAG, error);
            Log.e(TAG + ":send", "Sending to " + sender);
            ss.sendSMS(context, sender, error);
          }catch (JSONException e) { e.printStackTrace(); }
          Log.e(TAG+":save", result);
          Log.e(TAG, ">" + sender + "<");
        }
      }
      save_url = "";
      log_url = "";
      req_url = "";
      return null;
    }

    @Override
    protected void onPostExecute(Void result) {
      super.onPostExecute(result);
      if(!event_url.isEmpty()) {
        event_url = "";
        showSendProgress(event_keyword, event_message);
      }
    }
  }
}