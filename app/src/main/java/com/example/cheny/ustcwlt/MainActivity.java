package com.example.cheny.ustcwlt;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView ip_View, status_View, auth_View;
    private Spinner outSpi, timeSpi;
    private Button connButton, disConnButton, exitButton;
    private String Cookie_rn;
    private HttpGet httpGet;
    private HttpClient httpClient;
    private HttpResponse httpResponse;
    private Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            String responstr;
            switch (msg.what){
                case 0:
                    responstr = (String)msg.obj;
                    ip_View.setText(responstr);
                    break;
                case 1:
                    responstr = (String)msg.obj;
                    status_View.setText(responstr);
                    break;
                case 2:
                    responstr = (String)msg.obj;
                    auth_View.setText(responstr);
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outSpi = (Spinner)findViewById(R.id.out_spinner);
        timeSpi = (Spinner)findViewById(R.id.time_spinner);
        connButton = (Button)findViewById(R.id.Conn_button);
        disConnButton = (Button)findViewById(R.id.DisConn_button);
        exitButton = (Button)findViewById(R.id.exit_button);
        ip_View = (TextView)findViewById(R.id.IP_textView);
        status_View = (TextView)findViewById(R.id.status_textView);
        auth_View = (TextView)findViewById(R.id.auth_textView);

        Bundle bundle = getIntent().getExtras();
        Cookie_rn = bundle.getString("Cookie");
        ip_View.setText(bundle.getString("IP"));
        status_View.setText(bundle.getString("status"));
        auth_View.setText(bundle.getString("authority"));

        connButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new wltSetting().start();
            }
        });
        disConnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new wltDisconn().start();
            }
        });
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new wltLogout().start();
            }
        });
    }

    private int getType(String selectedType){
        String[] outStr = getResources().getStringArray(R.array.out);
        if(selectedType.equals(outStr[0])){
            return 0;
        }
        else if(selectedType.equals(outStr[1])){
            return 1;
        }
        else if(selectedType.equals(outStr[2])){
            return 2;
        }
        else if(selectedType.equals(outStr[3])){
            return 3;
        }
        else if(selectedType.equals(outStr[4])){
            return 4;
        }
        else if(selectedType.equals(outStr[5])){
            return 5;
        }
        else if(selectedType.equals(outStr[6])){
            return 6;
        }
        else if(selectedType.equals(outStr[7])){
            return 7;
        }
        else if(selectedType.equals(outStr[8])){
            return 8;
        }
        return -1;
    }
    private int getExp(String selectedExp){
        String[] expStr = getResources().getStringArray(R.array.time);
        if(selectedExp.equals(expStr[0])){
            return 3600;
        }
        else if(selectedExp.equals(expStr[1])){
            return 14400;
        }
        else if(selectedExp.equals(expStr[2])){
            return 39600;
        }
        else if(selectedExp.equals(expStr[3])){
            return 50400;
        }
        else if(selectedExp.equals(expStr[4])){
            return 0;
        }
        return -1;
    }

    private class wltSetting extends Thread{
        public void run(){
            String urlstr = "http://wlt.ustc.edu.cn/cgi-bin/ip" +
                    "?cmd=set&url=URL&type=" + String.valueOf(getType(outSpi.getSelectedItem().toString()))
                    + "&exp=" + String.valueOf(getExp(timeSpi.getSelectedItem().toString()))
                    + "&go=+%BF%AA%CD%A8%CD%F8%C2%E7+";
            httpGet = new HttpGet(urlstr);
            httpGet.setHeader("Cookie", Cookie_rn);
            httpClient = new DefaultHttpClient();
            try{
                httpResponse = httpClient.execute(httpGet);
            }catch (IOException e){
                e.printStackTrace();
            }
            if(httpResponse.getStatusLine().getStatusCode() == 200){
                String responseStr = getEntityStr(httpResponse);
                getIP(responseStr);
                getStatus(responseStr);
                getAuthority(responseStr);
                Toast.makeText(MainActivity.this,"设置成功",Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
            else{
                Toast.makeText(MainActivity.this,"设置失败",Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }
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

    private void getIP(String Str){
        String pattern = "(当前IP地址)(\\d+\\.\\d+\\.\\d+\\.\\d+)(状态:<br>出口:\\s)(\\S+)(<br>权限:\\s)(\\S\\S)";
        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(Str);
        Message msg = new Message();
        if(matcher.find()){
            msg.obj = matcher.group(2).toString();
            handler.sendMessage(msg);
            return;
        }
        msg.obj = "错误";
        handler.sendMessage(msg);
        return;
    }

    private void getStatus(String Str){
        String pattern = "(当前IP地址)(\\d+\\.\\d+\\.\\d+\\.\\d+)(状态:<br>出口:\\s)(\\S+)(<br>权限:\\s)(\\S\\S)";
        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(Str);
        Message msg = new Message();
        if(matcher.find()){
            msg.obj = matcher.group(4).toString();
            msg.what = 1;
            handler.sendMessage(msg);
            return;
        }
        msg.obj = "错误";
        msg.what = 1;
        handler.sendMessage(msg);
        return;
    }

    private void getAuthority(String Str){
        String pattern = "(当前IP地址)(\\d+\\.\\d+\\.\\d+\\.\\d+)(状态:<br>出口:\\s)(\\S+)(<br>权限:\\s)(\\S\\S)";
        Pattern pattern1 = Pattern.compile(pattern);
        Matcher matcher = pattern1.matcher(Str);
        Message msg = new Message();
        if(matcher.find()){
            msg.obj = matcher.group(6).toString();
            msg.what = 2;
            handler.sendMessage(msg);
            return;
        }
        msg.obj = "错误";
        handler.sendMessage(msg);
        return;
    }

    private class wltDisconn extends Thread{
        public void run(){
            String urlstr = "http://wlt.ustc.edu.cn/cgi-bin/ip"
                    + "?cmd=set&url=URL&type=8&exp=14400&setdefault=+%B9%D8%B1%D5%CD%F8%C2%E7";
            httpGet = new HttpGet(urlstr);
            httpGet.setHeader("Cookie", Cookie_rn);
            httpClient = new DefaultHttpClient();
            try{
                httpResponse = httpClient.execute(httpGet);
            }catch(IOException e){
                e.printStackTrace();
            }
            if(httpResponse.getStatusLine().getStatusCode() == 200){
                String responseStr = getEntityStr(httpResponse);
                getIP(responseStr);
                getStatus(responseStr);
                getAuthority(responseStr);
                Toast.makeText(MainActivity.this,"设置成功",Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
            else{
                Toast.makeText(MainActivity.this,"设置失败",Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }
    }

    private class wltLogout extends Thread{
        public void run(){
            String urlstr = "http://wlt.ustc.edu.cn/cgi-bin/ip?cmd=logout";
            httpGet = new HttpGet(urlstr);
            httpGet.setHeader("Cookie", Cookie_rn);
            httpClient = new DefaultHttpClient();
            try{
                httpResponse = httpClient.execute(httpGet);
            }
            catch (IOException e){
                e.printStackTrace();
            }
            if(httpResponse.getStatusLine().getStatusCode() == 200){
                finish();
            }
            else{
                Toast.makeText(MainActivity.this,"退出失败",Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }
    }
}
