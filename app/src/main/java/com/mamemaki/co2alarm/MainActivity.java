package com.mamemaki.co2alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Date;

public class MainActivity extends ActionBarActivity {

    private TextView mMessageText;
    private TextView mCo2Text;
    private TextView mLastUpdatedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessageText = (TextView) findViewById(R.id.message);
        mCo2Text = (TextView) findViewById(R.id.co2);
        mLastUpdatedText = (TextView) findViewById(R.id.last_updated);

        mLastUpdatedText.setText("");
        mCo2Text.setText("0");
        mMessageText.setText("");

        NetatmoIntentService.start(this, "", "");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(NetatmoIntentService.BROADCAST);
        LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(onNotice);
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

    private BroadcastReceiver onNotice = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final int status = intent.getIntExtra(NetatmoIntentService.EXTRA_STATUS, NetatmoIntentService.STATUS_NORMAL);
            final int statusContinuousCount = intent.getIntExtra(NetatmoIntentService.EXTRA_STATUS_CONTINUOUS_COUNT, 0);
            final int co2 = intent.getIntExtra(NetatmoIntentService.EXTRA_CO2, 0);

            String msg;
            switch (status) {
                default:
                case NetatmoIntentService.STATUS_NORMAL:
                    msg = "CO2濃度は正常範囲内です。";
                    break;
                case NetatmoIntentService.STATUS_CO2_HIGH:
                    msg = "CO2濃度が基準値を超えています。換気を行ってください。";
                    beep();
                    beep();
                    beep();
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    beep();
                    beep();
                    beep();
                    break;
            }
            mLastUpdatedText.setText(new Date().toString());
            mCo2Text.setText(String.valueOf(co2));
            mMessageText.setText(msg);
        }
    };

    private void beep() {
        ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP);
        toneGenerator.release();
    }

    public void onBeepButtonClicked(View view) {
        beep();
    }
}
