package com.mamemaki.co2alarm;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class NetatmoIntentService extends IntentService {
    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.mamemaki.co2alarm.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.mamemaki.co2alarm.extra.PARAM2";

    private Context mContext;
    private Handler mHandler;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
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
        mHandler.post(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(mContext, "NetatmoIntentService started", Toast.LENGTH_LONG).show();
            }
        });

        // Normally we would do some work here, like download a file.
        // For our sample, we just sleep for 5 seconds.
        long endTime = System.currentTimeMillis() + 5*1000;
        while (System.currentTimeMillis() < endTime) {
            synchronized (this) {
                try {
                    wait(endTime - System.currentTimeMillis());
                } catch (Exception e) {
                }
            }
        }

        mHandler.post(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(mContext, "NetatmoIntentService stopped", Toast.LENGTH_LONG).show();
            }
        });
    }
}
