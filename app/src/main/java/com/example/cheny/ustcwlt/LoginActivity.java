package com.example.cheny.ustcwlt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private TextView ip_View;
    private EditText id_Edit,pw_Edit;
    private Button LoginButton;
    private CheckBox svCheck;
    private HttpGet httpGet;
    private HttpPost httpPost;
    private HttpClient httpClient;
    private HttpResponse httpResponse;
    private String Cookie_rn;
    private String PREFERENCE_NAME = "survey";
    private Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            String Str;
            switch (msg.what){
                case 0:
                    Str = (String)msg.obj;
                    ip_View.setText(Str);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ip_View = (TextView)findViewById(R.id.IP_textView);
        id_Edit = (EditText)findViewById(R.id.id_editText);
        pw_Edit = (EditText)findViewById(R.id.pw_editText);
        LoginButton = (Button)findViewById(R.id.Login_button);
        svCheck = (CheckBox)findViewById(R.id.Savepw_checkBox);

        SharedPreferences sharedPreferences = getSharedPreferences(
                PREFERENCE_NAME,Activity.MODE_PRIVATE);
        id_Edit.setText(sharedPreferences.getString("userid",""));
        svCheck.setChecked(sharedPreferences.getBoolean("checked",false));
        if(svCheck.isChecked()){
            pw_Edit.setText(sharedPreferences.getString("userpw",""));
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED){
            //申请INTERNET权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 0);
        }
        //获取wifi服务
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(id_Edit.equals("")){
                    Toast.makeText(LoginActivity.this,"请填写用户名",Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
                else if(pw_Edit.equals("")){
                    Toast.makeText(LoginActivity.this,"请填写密码",Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
                else {
                    new wltLogin().start();
                }
            }
        });
        new wltConn().start();
    }

    private class wltLogin extends Thread{

        @Override
        public void run(){
            String urlstr = "http://wlt.ustc.edu.cn/cgi-bin/ip";
            httpPost = new HttpPost(urlstr);
            List<NameValuePair> params=new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("cmd","login"));
            params.add(new BasicNameValuePair("ip",ip_View.getText().toString()));
            params.add(new BasicNameValuePair("url","URL"));
            params.add(new BasicNameValuePair("name",id_Edit.getText().toString()));
            params.add(new BasicNameValuePair("password",pw_Edit.getText().toString()));
            params.add(new BasicNameValuePair("go","��¼�ʻ�"));
            try{
                httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            }catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
            httpClient = new DefaultHttpClient();
            try{
                httpResponse = httpClient.execute(httpPost);
            }catch (IOException e){
                e.printStackTrace();
            }
            if(httpResponse.getStatusLine().getStatusCode() == 200){
                Header[] Cookie = httpResponse.getHeaders("Set-Cookie");
                if(Cookie.length == 0){
                    Toast.makeText(LoginActivity.this,"用户名或密码不正确",Toast.LENGTH_SHORT).show();
                    Looper.loop();
                    return;
                }
                Cookie_rn = Cookie[0].getValue();
                String entityStr = getEntityStr(httpResponse);
                Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                intent.putExtra("IP", getIp(entityStr, 1));
                intent.putExtra("status", getStatus(entityStr));
                intent.putExtra("authority", getAuthority(entityStr));
                intent.putExtra("Cookie", Cookie_rn);
                startActivity(intent);
            }
            else {
                Toast.makeText(LoginActivity.this,"登陆失败",Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }
    }
    private String getStatus(String Str){
        String pattern = "(当前IP地址)(\\d+\\.\\d+\\.\\d+\\.\\d+)(状态:<br>出口:\\s)(\\S+)(<br>权限:\\s)(\\S\\S)";
        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(Str);
        Message msg = new Message();
        if(matcher.find()){
            return matcher.group(4).toString();
        }
        return "";
    }

    private String getAuthority(String Str){
        //先检查是否拥有连接权限，再检查是否是科大ip，都无错误再提取ip
        String pattern = "(您没有使用网络通对外连接的权限)";
        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(Str);
        if(matcher.find()){
            return matcher.group(0).toString();
        }
        pattern = "(您使用的IP地址)(\\d+\\.\\d+\\.\\d+\\.\\d+)(不是科大校内IP地址，<br>无法使用网络通账号设置权限。)";
        pattern1 = Pattern.compile(pattern);
        matcher = pattern1.matcher(Str);
        if(matcher.find()){
            return matcher.group(3).toString().replace("<br>","\n");
        }
        pattern = "(当前IP地址)(\\d+\\.\\d+\\.\\d+\\.\\d+)(状态:<br>出口:\\s)(\\S+)(<br>权限:\\s)(\\S\\S)";
        pattern1 = Pattern.compile(pattern);
        matcher = pattern1.matcher(Str);
        if(matcher.find()){
            return matcher.group(6).toString();
        }
        return "错误";
    }

    private String getEntityStr(HttpResponse response){
        String responseStr = "", line = "";
        StringBuilder total = new StringBuilder();
        BufferedReader bufferedReader;
        try{
            bufferedReader = new BufferedReader(new InputStreamReader(response
                    .getEntity().getContent(), "gb2312"));
            while((line = bufferedReader.readLine()) != null){
                total.append(line);
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
        responseStr = total.toString();
        responseStr.replace("\n","");
        responseStr.replace("\t","");
        return responseStr;
    }

    private String getIp(String Str, int type){
        String pattern = "";
        switch (type){
            case 0: {
                pattern = "(name=ip\\s+value=)(\\d+\\.\\d+\\.\\d+\\.\\d+)";
                Pattern pattern1 = Pattern.compile(pattern);
                Matcher matcher = pattern1.matcher(Str);
                if(matcher.find()){
                    return matcher.group(2);
                }
                break;
            }
            case 1: {
                pattern = "(当前IP地址)(\\d+\\.\\d+\\.\\d+\\.\\d+)(状态:<br>出口:\\s)(\\S+)(<br>权限:\\s)(\\S\\S)";
                Pattern pattern1 = Pattern.compile(pattern);
                Matcher matcher = pattern1.matcher(Str);
                if(matcher.find()){
                    return matcher.group(2);
                }
                pattern = "(您使用的IP地址)(\\d+\\.\\d+\\.\\d+\\.\\d+)(不是科大校内IP地址，<br>无法使用网络通账号设置权限。)";
                pattern1 = Pattern.compile(pattern);
                matcher = pattern1.matcher(Str);
                if(matcher.find()){
                    return matcher.group(2);
                }
                break;
            }
            default:
                break;
        }
        return "";
    }

    private boolean iswlt(String Str){
        String pattern = "(非科大IP地址)";
        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(Str);
        if(matcher.find()){
            return false;
        }
        return true;
    }

    private class wltConn extends Thread{
        @Override
        public void run(){
            String str_url = "http://wlt.ustc.edu.cn/cgi-bin/ip";
            httpGet = new HttpGet(str_url);
            httpClient = new DefaultHttpClient();
            try{
                httpResponse = httpClient.execute(httpGet);
            }
            catch(IOException e){
                e.printStackTrace();
            }
            if(httpResponse.getStatusLine().getStatusCode() == 200){
                Message msg = new Message();
                String str = getEntityStr(httpResponse);
                String objStr = getIp(str, 0);
                //不是科大IP
                if(!iswlt(str)){
                    objStr += "（非科大IP地址）";
                }
                msg.obj = objStr;
                handler.sendMessage(msg);
            }
            else{
                Toast.makeText(LoginActivity.this,"无法连接到网络通,请检查网络状态",Toast.LENGTH_SHORT).show();
                Looper.loop();
                Message msg = new Message();
                msg.obj = "0.0.0.0";
                handler.sendMessage(msg);
            }
        }
    }

    protected void onStop(){
        SharedPreferences sharedPreferences = getSharedPreferences(
                PREFERENCE_NAME, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userid",id_Edit.getText().toString());
        editor.putBoolean("checked",svCheck.isChecked());
        if(svCheck.isChecked()){
            editor.putString("userpw",pw_Edit.getText().toString());
        }
        editor.apply();
        super.onStop();
    }
}
