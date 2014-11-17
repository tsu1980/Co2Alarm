package com.mamemaki.co2alarm;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class NetatmoIntentService extends IntentService {
    private static final String TAG = "NetatmoIntentService";

    public static final String BROADCAST = "com.mamemaki.co2alarm.localcast.NetatmoIntentService.BROADCAST";

    public static final String EXTRA_PARAM1 = "com.mamemaki.co2alarm.extra.PARAM1";
    public static final String EXTRA_PARAM2 = "com.mamemaki.co2alarm.extra.PARAM2";
    public static final String EXTRA_STATUS_CONTINUOUS_COUNT = "com.mamemaki.co2alarm.extra.STATUS_CONTINUOUS_COUNT";
    public static final String EXTRA_STATUS = "com.mamemaki.co2alarm.extra.status";

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_CO2_HIGH = 1;

    private static final long FETCH_INTERVAL = 5000;
    private static final long CO2_THRESHOLD = 2200;

    private Context mContext;
    private Handler mHandler;
    // 同じ状態の蓄積数
    private int mStatusContinuousCount;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void start(Context context, String param1, String param2) {
        Intent intent = new Intent(context, NetatmoIntentService.class);
        intent.setAction("com.mamemaki.co2alarm.action.start");
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public NetatmoIntentService() {
        super("NetatmoIntentService");
        mHandler = new Handler();
        mStatusContinuousCount = 0;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mContext = getApplicationContext();
            final String param1 = intent.getStringExtra(EXTRA_PARAM1);
            final String param2 = intent.getStringExtra(EXTRA_PARAM2);
            handleActionFoo(param1, param2);
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        Log.d(TAG, "BEGIN NetatmoIntentService" + this + param1 + param2);
        showToast("NetatmoIntentService started", Toast.LENGTH_LONG);

        boolean b1 = false;
        int prevStatus = STATUS_NORMAL;
        while (true) {
            int status = STATUS_NORMAL;
            //TODO: Fetch co2 status
            if (b1) {
                status = STATUS_CO2_HIGH;
            }
            b1 = true;

            if (prevStatus != status) {
                mStatusContinuousCount = 0;
            } else {
                mStatusContinuousCount++;
            }
            prevStatus = status;

            Intent intent = new Intent(BROADCAST);
            intent.putExtra(EXTRA_STATUS, status);
            intent.putExtra(EXTRA_STATUS_CONTINUOUS_COUNT, mStatusContinuousCount);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

            // Wait for interval
            synchronized (this) {
                try {
                    wait(FETCH_INTERVAL);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "wait() failed", e);
                }
            }
        }

        showToast("NetatmoIntentService stopped", Toast.LENGTH_LONG);
    }

    private void showToast(final String message, final int duration) {
        mHandler.post(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(mContext, message, duration).show();
            }
        });
    }
}
