package in.ssmediaos.smsgateway;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;

public class MainActivity extends Activity {
  EditText url, hospital, key;
  TextView status;
  SharedPreferences prefs;
  int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+0.5f);}
  @Override public void onCreate(Bundle b){
    super.onCreate(b); prefs=getSharedPreferences("gateway",MODE_PRIVATE);
    LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(dp(20),dp(28),dp(20),dp(20)); root.setGravity(Gravity.CENTER_HORIZONTAL);
    root.setBackgroundColor(0xFFF6F8FC);
    TextView title=new TextView(this); title.setText("SS MediaOS\nSIM SMS Gateway"); title.setTextSize(24); title.setTextColor(0xFF12345A); title.setGravity(Gravity.CENTER); root.addView(title,new LinearLayout.LayoutParams(-1,dp(100)));
    url=field(root,"HTTPS API Base URL (ending /api)",prefs.getString("url","https://yourdomain.in/smsgateway/api"));
    hospital=field(root,"Hospital ID",prefs.getString("hospital","hospital01"));
    key=field(root,"Device API Key",prefs.getString("key","")); key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
    status=new TextView(this); status.setText("Ready. Use HTTPS and your own device key."); status.setTextSize(14); status.setPadding(0,dp(16),0,dp(16)); root.addView(status);
    Button start=new Button(this); start.setText("SAVE & START GATEWAY"); root.addView(start,new LinearLayout.LayoutParams(-1,dp(56)));
    Button stop=new Button(this); stop.setText("STOP GATEWAY"); root.addView(stop,new LinearLayout.LayoutParams(-1,dp(52)));
    TextView note=new TextView(this); note.setText("Keep this phone charged and connected. Set the correct default SMS SIM. SMS charges may apply."); note.setPadding(0,dp(18),0,0); root.addView(note);
    setContentView(root);
    start.setOnClickListener(v->startGateway()); stop.setOnClickListener(v->{stopService(new Intent(this,GatewayService.class)); status.setText("Gateway stopped.");});
  }
  EditText field(LinearLayout root,String label,String value){
    TextView l=new TextView(this); l.setText(label); l.setTextColor(0xFF263238); l.setPadding(0,dp(10),0,dp(4)); root.addView(l);
    EditText e=new EditText(this); e.setSingleLine(true); e.setText(value); e.setPadding(dp(10),0,dp(10),0); root.addView(e,new LinearLayout.LayoutParams(-1,dp(50))); return e;
  }
  void startGateway(){
    String u=url.getText().toString().trim().replaceAll("/+$",""), h=hospital.getText().toString().trim(), k=key.getText().toString().trim();
    if(!u.startsWith("https://")||!u.endsWith("/api")||h.isEmpty()||k.isEmpty()){status.setText("HTTPS URL ending in /api, Hospital ID and Device Key are required.");return;}
    prefs.edit().putString("url",u).putString("hospital",h).putString("key",k).apply();
    if(checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.SEND_SMS},10);return;}
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);return;}
    Intent i=new Intent(this,GatewayService.class);
    if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
    status.setText("Gateway started. Keep the foreground notification visible.");
  }
  @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g); if((r==10||r==11)&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED) startGateway(); else status.setText("Permission required to send SMS / show service notification.");}
}
