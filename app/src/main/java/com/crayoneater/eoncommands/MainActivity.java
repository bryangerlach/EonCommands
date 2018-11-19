package com.crayoneater.eoncommands;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Properties;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private String getKey()
    {
        String fileName = "op_rsa";
        InputStream is = getResources().openRawResource(R.raw.op_rsa);
        InputStreamReader inputreader = new InputStreamReader(is);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(fileName,MODE_PRIVATE);
            outputStream.write(text.toString().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        File file = new File(getFilesDir(), fileName);
        return file.toString();
    }

    public void deleteButton(View v)
    {
        String command = "rm -rf /data/media/0/realdata";
        prepareCommand(command);
    }

    public void pullButton(View v)
    {
        String command = "cd /data/openpilot; git pull; reboot";
        prepareCommand(command);
    }

    public void settingsButton(View v)
    {
        String command = "am start -a android.settings.SETTINGS";
        prepareCommand(command);
    }

    public void cloneButton(View v)
    {
        EditText ipText = findViewById(R.id.editText2);
        final String com = ipText.getText().toString();
        String command = "cd /data; rm -rf openpilot; git clone "+com;
        prepareCommand(command);
    }

    public void checkoutButton(View v)
    {
        EditText ipText = findViewById(R.id.editText3);
        final String com = ipText.getText().toString();
        String command = "cd /data/openpilot; git checkout "+com;
        prepareCommand(command);
    }

    public void customButton(View v)
    {
        EditText ipText = findViewById(R.id.editText4);
        final String command = ipText.getText().toString();
        prepareCommand(command);
    }

    public void ping(String ip)
    {
        Log.e("ping",ip);
        Runtime runtime = Runtime.getRuntime();
        try
        {
            for(int i = 0; i < 15; i++) {
                Process mIpAddrProcess = runtime.exec("/system/bin/ping -n 1 " + ip);
                int mExitValue = mIpAddrProcess.waitFor();
                System.out.println(" mExitValue " + mExitValue);
            }
        }
        catch (InterruptedException ee)
        {
            ee.printStackTrace();
            System.out.println(" Exception:"+ee);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println(" Exception:"+e);
        }
    }

    public void prepareCommand(final String command)
    {
        Log.e("command: ",command);
        EditText ipText = findViewById(R.id.editText);
        final String ip = ipText.getText().toString();
        final String key = getKey();
        ping(ip);
        Log.e("IP",ip);
        new MyTask(this,ip,command,key).execute();
    }

    private static class MyTask extends AsyncTask<Void, Void, String> {

        private WeakReference<MainActivity> activityReference;
        private String ip;
        private String command;
        private String key;
        // only retain a weak reference to the activity
        MyTask(MainActivity context,String ip,String command,String key) {
            this.activityReference = new WeakReference<>(context);
            this.ip = ip;
            this.command = command;
            this.key = key;
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                executeCommand("root", this.ip, 8022, this.command, this.key);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "task finished";
        }

        @Override
        protected void onPostExecute(String result) {
            Activity activity = this.activityReference.get();
            Toast.makeText(activity,"Command Sent: "+this.command,Toast.LENGTH_LONG).show();
        }
    }

    public static void executeCommand(String username, String hostname, int port, String command, String key)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(key);
            Session session = jsch.getSession(username, hostname, port);
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            session.setConfig(prop);
            session.connect();

            ChannelExec channelssh = (ChannelExec) session.openChannel("exec");
            channelssh.setOutputStream(baos);

            channelssh.setCommand(command);
            channelssh.connect();

            byte[] buffer = new byte[1024];
            try {
                InputStream in = channelssh.getInputStream();
                String line = "";
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(buffer, 0, 1024);
                        if (i < 0) {
                            break;
                        }
                        line = new String(buffer, 0, i);
                        Log.e("results: ", line);
                    }

                    if (line.contains("logout")) {
                        break;
                    }

                    if (channelssh.isClosed()) {
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ee) {
                        System.out.println("Error: "+ee);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error while reading channel output: " + e);
            }
            channelssh.disconnect();
        }catch (Exception ee) {
            Log.e("Error connecting.",ee.toString());
        }
    }
}

