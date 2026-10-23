package in.ssmediaos.smsgateway;

import android.app.*;
import android.content.*;
import android.os.*;
import android.telephony.SmsManager;
import android.content.pm.PackageManager;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class GatewayService extends Service {
  Handler handler; Runnable loop; String base, hospital, key; boolean busy=false;
  @Override public void onCreate(){super.onCreate(); handler=new Handler(Looper.getMainLooper()); createNotification();}
  void createNotification(){
    String ch="sms_gateway";
    if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(ch,"SMS Gateway",NotificationManager.IMPORTANCE_LOW); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}
    Notification n=new Notification.Builder(this,ch).setContentTitle("SS MediaOS SMS Gateway").setContentText("Gateway is running; SMS may be sent from this phone's default SIM.").setSmallIcon(android.R.drawable.stat_notify_sync).build();
    startForeground(77,n);
  }
  @Override public int onStartCommand(Intent i,int flags,int id){
    SharedPreferences p=getSharedPreferences("gateway",MODE_PRIVATE); base=p.getString("url",""); hospital=p.getString("hospital",""); key=p.getString("key","");
    if(loop==null){loop=()->{ if(!busy){busy=true; new Thread(()->{try{pollAndSend();}finally{busy=false;}}).start();} handler.postDelayed(loop,30000);}; handler.post(loop);}
    return START_STICKY;
  }
  void pollAndSend(){
    try{
      JSONObject auth=new JSONObject(); auth.put("hospital_id",hospital); auth.put("device_key",key);
      JSONObject response=post(base+"/poll.php",auth);
      if(!response.optBoolean("ok")||response.isNull("sms")) return;
      JSONObject sms=response.getJSONObject("sms"); long id=sms.getLong("id"); String to=sms.getString("recipient"), msg=sms.getString("message");
      String result="sent";
      try{
        if(checkSelfPermission(android.Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED) throw new Exception("SEND_SMS permission missing");
        SmsManager sm=SmsManager.getDefault();
        java.util.ArrayList<String> parts=sm.divideMessage(msg);
        if(parts.size()>1) sm.sendMultipartTextMessage(to,null,parts,null,null); else sm.sendTextMessage(to,null,msg,null,null);
      }catch(Exception e){result="failed";}
      JSONObject report=new JSONObject(); report.put("hospital_id",hospital); report.put("device_key",key); report.put("sms_id",id); report.put("status",result); report.put("detail",result.equals("sent")?"send_submitted":"send_failed");
      post(base+"/status.php",report);
    }catch(Exception ignored){}
  }
  JSONObject post(String address,JSONObject data)throws Exception{
    URL url=new URL(address); HttpURLConnection c=(HttpURLConnection)url.openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(10000); c.setReadTimeout(10000); c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json; charset=utf-8");
    byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8); try(OutputStream o=c.getOutputStream()){o.write(bytes);}
    InputStream in=(c.getResponseCode()<400)?c.getInputStream():c.getErrorStream(); ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buf=new byte[2048]; int n; while((n=in.read(buf))>0)out.write(buf,0,n); c.disconnect(); return new JSONObject(out.toString("UTF-8"));
  }
  @Override public void onDestroy(){if(handler!=null&&loop!=null)handler.removeCallbacks(loop); loop=null; stopForeground(true); super.onDestroy();}
  @Override public IBinder onBind(Intent i){return null;}
}
