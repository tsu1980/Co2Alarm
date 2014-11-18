package com.mamemaki.co2alarm;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.netatmo.weatherstation.api.NetatmoResponseHandler;
import com.netatmo.weatherstation.api.model.Measures;
import com.netatmo.weatherstation.api.model.Module;
import com.netatmo.weatherstation.api.model.Params;
import com.netatmo.weatherstation.api.model.Station;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * helper methods.
 */
public class NetatmoIntentService extends IntentService {
    private static final String TAG = "NetatmoIntentService";

    private String mEmail = "tsu1980@gmail.com";
    private String mPassword = "siva222";

    public static final String BROADCAST = "com.mamemaki.co2alarm.localcast.NetatmoIntentService.BROADCAST";

    public static final String EXTRA_PARAM1 = "com.mamemaki.co2alarm.extra.PARAM1";
    public static final String EXTRA_PARAM2 = "com.mamemaki.co2alarm.extra.PARAM2";
    public static final String EXTRA_STATUS_CONTINUOUS_COUNT = "com.mamemaki.co2alarm.extra.STATUS_CONTINUOUS_COUNT";
    public static final String EXTRA_STATUS = "com.mamemaki.co2alarm.extra.status";
    public static final String EXTRA_CO2 = "com.mamemaki.co2alarm.extra.co2";

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_CO2_HIGH = 1;

//    private static final long FETCH_INTERVAL = 5*1000;
    private static final long FETCH_INTERVAL = 5*60*1000;
    private static final long THRESHOLD_CO2 = 2200;

    private Context mContext;
    private Handler mHandler;
    // 同じ状態の蓄積数
    private int mStatusContinuousCount;

    private SampleHttpClient mHttpClient;

    private List<Station> mDevices;

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

        int prevStatus = STATUS_NORMAL;
        while (true) {
            // Fetch Co2 measure
            fetchMeasures();

            int co2 = 0;
            Station station = mDevices.get(0);
            Measures measures;
            if (station != null) {
                Module module = station.getModules().get(0);
                if (module != null) {
                    measures = module.getMeasures();

                    try {
                        co2 = Integer.parseInt(measures.getCO2());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            int status = STATUS_NORMAL;
            if (co2 > THRESHOLD_CO2) {
                status = STATUS_CO2_HIGH;
            }

            if (prevStatus != status) {
                mStatusContinuousCount = 0;
            } else {
                mStatusContinuousCount++;
            }
            prevStatus = status;

            Intent intent = new Intent(BROADCAST);
            intent.putExtra(EXTRA_STATUS, status);
            intent.putExtra(EXTRA_STATUS_CONTINUOUS_COUNT, mStatusContinuousCount);
            intent.putExtra(EXTRA_CO2, co2);
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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, duration).show();
            }
        });
    }

    private final Object lock = new Object();
    private boolean ready = false;
    private void fetchMeasures() {
        // HttpClient used for all requests in this activity.
        mHttpClient = new SampleHttpClient(this);

        mHttpClient.clearTokens();

        if (mHttpClient.getAccessToken() != null) {
            // If the user is already logged in.
            if (mDevices != null) {
                getMeasures();
            } else {
                getDevices();
            }
        } else {
            netatmoLogin();
        }

        // Wait for finish
        synchronized(lock) {
            while(!ready)
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }

    private void netatmoLogin() {
        final String M = "netatmoLogin: ";
        Log.i(TAG,M);

        // NetatmoResponseHandler parses and handles the response.
        // You can also use JsonHttpResponseHandler and process the response as you wish.
        mHttpClient.login(mEmail, mPassword, new NetatmoResponseHandler(mHttpClient,
                NetatmoResponseHandler.REQUEST_LOGIN, null) {
            @Override
            public void onStart() {
                Log.i(TAG, M + " onStart:");
                super.onStart();
            }

            @Override
            public void onLoginResponse() {
                Log.i(TAG, M + " onLoginResponse:");
//                setResult(RESULT_OK);
//                finish();
            }

            @Override
            public void onFailure(Throwable e, JSONObject errorResponse) {
                Log.i(TAG, M + " onFailure:");

                super.onFailure(e, errorResponse);

                Log.i(TAG, M + " onFailure:");
            }

            @Override
            public void onFinish() {
                Log.i(TAG, M + " onFinish:");

                super.onFinish();

                if (mDevices != null) {
                    getMeasures();
                } else {
                    getDevices();
                }
            }
        });
    }

    /**
     * Initializing the action bar with the stations' names using the parsed response returned by
     * NetatmoHttpClient.getDevicesList(NetatmoResponseHandler).
     */
    private void getDevices() {
//        mAdapter = new CustomAdapter(this, mListItems);
//        setListAdapter(mAdapter);
//
        // NetatmoResponseHandler returns a parsed response (by overriding onGetDevicesListResponse).
        // You can also use JsonHttpResponseHandler and process the response as you wish.
        mHttpClient.getDevicesList(new NetatmoResponseHandler(mHttpClient,
                NetatmoResponseHandler.REQUEST_GET_DEVICES_LIST, null) {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onGetDevicesListResponse(final List<Station> devices) {
                mDevices = devices;

                List<String> stationsNames = new ArrayList<String>();
                for (Station station : devices) {
                    stationsNames.add(station.getName());
                }

                getMeasures();
            }

            @Override
            public void onFinish() {
                super.onFinish();
            }
        });
    }

    /**
     * Calls getLastMeasures() for all modules associated with the selected station.
     */
    public boolean getMeasures() {
        Station station = mDevices.get(0);
        final List<Module> modules = station.getModules();

        String[] types = new String[]{
                Params.TYPE_NOISE,
                Params.TYPE_CO2,
                Params.TYPE_PRESSURE,
                Params.TYPE_HUMIDITY,
                Params.TYPE_TEMPERATURE
        };

        Log.d(TAG, "calling HTTP");
        /* NetatmoResponseHandler returns a parsed response (by overriding onGetMeasuresResponse).
         * You can also use JsonHttpResponseHandler and process the response as you wish.
         *
         * The API changed a bit, and now the deviceList contains all the basic data you may need, no need to call
         * getMeasures (except if you need only module-specific data, like for a widget for example)
         * We are reloading it at every item selected only to show the update process, it's not really optimized.
         */
        mHttpClient.getDevicesList(
                new NetatmoResponseHandler(mHttpClient, NetatmoResponseHandler.REQUEST_GET_LAST_MEASURES, types) {
                    @Override
                    public void onGetMeasuresResponse( final HashMap<String, Measures> measures) {
                        for (final Module module : modules) {
                            if (measures.containsKey(module.getId())) {
                                module.setMeasures(measures.get(module.getId()));
//                                mListItems.add(module);
                            }
                        }

                        synchronized(lock) {
                            ready = true;
                            lock.notifyAll();
                        }
                    }
                });

        return true;
    }
}
