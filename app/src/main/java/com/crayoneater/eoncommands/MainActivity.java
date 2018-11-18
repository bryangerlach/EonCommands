package com.crayoneater.eoncommands;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {

    public int pingCount = 0;

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
        EditText ipText = (EditText) findViewById(R.id.editText2);
        final String com = ipText.getText().toString();
        String command = "cd /data; rm -rf openpilot; git clone "+com;
        prepareCommand(command);
    }

    public void checkoutButton(View v)
    {
        EditText ipText = (EditText) findViewById(R.id.editText3);
        final String com = ipText.getText().toString();
        String command = "cd /data/openpilot; git checkout "+com;
        prepareCommand(command);
    }

    public void customButton(View v)
    {
        EditText ipText = (EditText) findViewById(R.id.editText4);
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
        catch (InterruptedException ignore)
        {
            ignore.printStackTrace();
            System.out.println(" Exception:"+ignore);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.out.println(" Exception:"+e);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void prepareCommand(final String command)
    {
        Log.e("command: ",command);
        EditText ipText = (EditText) findViewById(R.id.editText);
        final String ip = ipText.getText().toString();
        ping(ip);
        Log.e("IP",ip);
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    executeCommand("root", ip, 8022, command);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }

    public void executeCommand(String username, String hostname, int port, String command)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(getKey());
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

