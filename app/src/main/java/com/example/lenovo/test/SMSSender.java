package com.example.lenovo.test;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.ArrayList;

class SMSSender {
  private static SmsManager sms = SmsManager.getDefault();

  void sendSMS(Context context, String phone, String message) {
    boolean DEBUG = false;

    if(DEBUG) {
      if(message.length() > 140) {
        sendMultipartMessage(context, phone, message);
      }else {
        sendTextMessage(context, phone, message);
      }
    }else {
      if(message.length() > 140) {
        Log.e("SMSSender", " TO:" + phone + "&l:" + message);
      }else {
        Log.e("SMSSender", " TO:" + phone + "&s:" + message);
      }
    }
  }

  private void sendTextMessage(Context context, String phone, String message) {
    sms.sendTextMessage(phone, null, message, null, null);
  }

  private void sendMultipartMessage(Context context, String phone, String message) {
    ArrayList<String> messageParts = sms.divideMessage(message);
    sms.sendMultipartTextMessage(phone, null, messageParts, null, null);
  }
}
