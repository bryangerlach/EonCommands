package com.crayoneater.eoncommands;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static EditText ipText1;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipText1 = findViewById(R.id.editText);
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

    public static Future<String> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
        return es.submit(new Callable<String>() {
            @Override public String call() {
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();

                    Log.e("EON detected: ",""+ip);
                    ipText1.setText(ip);
                    return ip;
                } catch (Exception ex) {
                    Log.e("NO: ",""+ip);
                    return null;
                }
            }
        });
    }

    public void scanNetwork(View v) {
        final ExecutorService es = Executors.newFixedThreadPool(20);
        final String fullIp = getIPAddress();
        //final String fullIp = "192.168.86.16";
        final String ip = fullIp.substring(0,fullIp.lastIndexOf('.'));
        final int timeout = 800;
        final List<Future<String>> futures = new ArrayList<>();
        for (int ipr = 0; ipr <= 255; ipr++) {
            futures.add(portIsOpen(es, ip+"."+ipr, 8022, timeout));
        }
        es.shutdown();
        int openPorts = 0;
        for (final Future<String> f : futures) {
            try {
                if (f.get() != null) {
                    openPorts++;
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("There are " + openPorts + " open ports on host " + ip + " (probed with a timeout of " + timeout + "ms)");
    }

    public static String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (isIPv4) {return sAddr;}
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }
}

