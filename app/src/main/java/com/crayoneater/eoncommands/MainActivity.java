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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void executeDeleteCommand(String username, String hostname, int port)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(getKey());
            Session session = jsch.getSession(username, hostname, port);
            //session.setPassword(password);

            // Avoid asking for key confirmation
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            session.setConfig(prop);

            session.connect();

            // SSH Channel
            ChannelExec channelssh = (ChannelExec)
                    session.openChannel("exec");

            channelssh.setOutputStream(baos);

            // Execute command
            channelssh.setCommand("rm -Rf /data/media/0/realdata/*");
            channelssh.connect();
            ///////////////////////////
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
            ////////////////////////////////
            channelssh.disconnect();
        }catch (Exception ee) {
            Log.e("Error connecting.",ee.toString());
        }
        //return baos.toString();
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

    @SuppressLint("StaticFieldLeak")
    public void deleteButton(View v)
    {
        EditText ipText = (EditText) findViewById(R.id.editText);
        final String ip = ipText.getText().toString();
        Log.e("IP",ip);
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    executeDeleteCommand("root", ip, 8022);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }
}

