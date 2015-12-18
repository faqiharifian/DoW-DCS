package com.digitcreativestudio.safian.dowdcs;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private LinearLayout toggleButton;
    private TextView hint, toggleLeft, toggleRight;
    private boolean                      toggleStatus = false;
    private WifiStateReceiver            wifiStateReceiver;
    private Utility utility;
    private ImageView imageView;
    private NotificationCompat.Builder mBuilder;
    private final int notificationId = 1756;
    private Intent notificationIntent;
    private PendingIntent notificationPendingIntent;
    private NotificationManager mNotifyMgr;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.toggleButton = (LinearLayout) findViewById(R.id.toggleLayout);
        this.hint = (TextView) findViewById(R.id.hint);
        this.toggleLeft = (TextView) findViewById(R.id.toggleLeft);
        this.toggleRight = (TextView) findViewById(R.id.toggleRight);
        this.imageView = (ImageView) findViewById(R.id.dcs_logo);

        this.wifiStateReceiver = new WifiStateReceiver();
        registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));

        utility = new Utility(MainActivity.this);

        notificationIntent = new Intent(this, MainActivity.class);

        notificationPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setAutoCancel(false)
                .setContentIntent(notificationPendingIntent)
                .setOngoing(true);

        mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

        init();
    }

    private void init() {
        if (utility.isWifiConnected()) {
            resetToggleStatus();
        } else {
            lockToggle();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void resetToggleStatus() {
        toggleStatus = utility.getAdbdStatus();
        boolean ret = utility.setAdbWifiStatus(toggleStatus);
        setToggleStatus(ret);
        toggleButton.setOnClickListener(null);
        toggleButton.setOnClickListener(new ToggleClickListener());
    }

    private void lockToggle() {
        utility.setAdbWifiStatus(false);             // try stop
        toggleStatus = false;
        setToggleStatus(toggleStatus);
        hint.setText("wifi is not connected");
        toggleButton.setOnClickListener(null);
    }

    private class ToggleClickListener implements View.OnClickListener {
        @Override public void onClick(View v) {
            if (utility.isWifiConnected()) {
                // try switch
                boolean ret = utility.setAdbWifiStatus(!toggleStatus);
                if (ret) {
                    // switch successfully
                    toggleStatus = !toggleStatus;
                    setToggleStatus(toggleStatus);

                } else {
                    // failed
                    Toast.makeText(MainActivity.this, "something wrong", Toast.LENGTH_SHORT).show();
                }
            } else {
                lockToggle();
            }
        }
    }

    private void setToggleStatus(boolean status) {
        if (!status) {
            toggleLeft.setText("OFF");
            toggleLeft.setBackgroundColor(getResources().getColor(R.color.gray_dark));
            toggleRight.setText("");
            toggleRight.setBackgroundColor(getResources().getColor(R.color.gray_light));
            hint.setText("");
            imageView.setImageResource(R.drawable.dcs_gray);

            Toast.makeText(MainActivity.this, "adb wifi service stopped", Toast.LENGTH_SHORT).show();
            mNotifyMgr.cancel(notificationId);
        } else {
            toggleLeft.setText("");
            toggleLeft.setBackgroundColor(getResources().getColor(R.color.gray_light));
            toggleRight.setText("ON");
            toggleRight.setBackgroundColor(getResources().getColor(R.color.accent));
            hint.setText("adb connect " + utility.getIp() + ":" + String.valueOf(utility.getPort()));
            imageView.setImageResource(R.drawable.dcs);

            Toast.makeText(MainActivity.this, "adb wifi service started", Toast.LENGTH_SHORT).show();
            mBuilder.setContentText("adb connect " + utility.getIp() + ":" + String.valueOf(utility.getPort()));
            mNotifyMgr.notify(notificationId, mBuilder.build());
        }
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    // wifi is connected, waiting for ip
                    try {
                        Thread.sleep(10000);
                        int tryTimes = 0;
                        while (utility.getIp() == null && tryTimes < 0) {
                            Thread.sleep(1000);
                        }
                        resetToggleStatus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // wifi connection lost
                    lockToggle();
                }
            }
        }
    }
}
