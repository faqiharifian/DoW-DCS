package com.digitcreativestudio.safian.dowdcs;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by faqih_000 on 10/7/2015.
 */
public class Utility {

    private final Pattern IPV4_PATTERN =
            Pattern.compile(
                    "^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    private int configPort = 5555;

    private boolean tethering = false;

    private Context context;

    public Utility(Context context){
        this.context = context;
    }

    // adb wifi port, default 5555
    public int getPort() {
        return configPort;
    }

    // get phone ip
    public String getIp() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = isIPv4Address(sAddr);
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
            if(tethering){
                return "192.168.43.1";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // detect if wifi is connected
    public boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        NetworkInfo.State state = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        try{
            Method method = wifi.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true); //in the case of visibility change in future APIs
            Boolean stateTethering =  (Boolean) method.invoke(wifi);
            Toast.makeText(context, String.valueOf(stateTethering) + " ---- " + String.valueOf(state), Toast.LENGTH_LONG);
            tethering = stateTethering;
            if (state == NetworkInfo.State.CONNECTED || stateTethering) {
                return true;
            }
        }catch (final Throwable ignored)
        {
        }

        return false;
    }

    // detect if adbd is running
    public boolean getAdbdStatus() {
        int lineCount = 0;
        try {
            Process process = Runtime.getRuntime().exec("ps | grep adbd");
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String str = input.readLine();
            while (str != null) {
                lineCount++;
                str = input.readLine();
            }
            if (lineCount >= 2) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // set adb wifi service
    public boolean setAdbWifiStatus(boolean status) {
        Process p;
        try {
            p = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(p.getOutputStream());


            if (status) {
                os.writeBytes("setprop service.adb.tcp.port " + String.valueOf(getPort()) + "\n");
                os.writeBytes("stop adbd\n");
                os.writeBytes("start adbd\n");
            }else{
                os.writeBytes("setprop service.adb.tcp.port -1\n");
                os.writeBytes("stop adbd\n");
                os.writeBytes("start adbd\n");
            }
            os.writeBytes("exit\n");
            os.flush();

            p.waitFor();
            if (p.exitValue() != 255) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isIPv4Address(final String input) {
        return IPV4_PATTERN.matcher(input).matches();
    }
}
