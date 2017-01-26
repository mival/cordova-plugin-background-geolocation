/*
According to apache license

This is fork of christocracy cordova-plugin-background-geolocation plugin
https://github.com/christocracy/cordova-plugin-background-geolocation

Differences to original version:

1. location is not persisted to db anymore, but broadcasted using intents instead
*/

package cz.iquest.bgloc;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.Toast;

import com.marianhello.bgloc.AbstractLocationProvider;
import com.marianhello.bgloc.LocationService;
import com.marianhello.logging.LoggerManager;

public class GpsLocationProvider extends AbstractLocationProvider implements LocationListener {
    private static final String TAG = GpsLocationProvider.class.getSimpleName();

    private PowerManager.WakeLock wakeLock;

    private String activity;

    private LocationManager locationManager;

    private org.slf4j.Logger log;

    public GpsLocationProvider(LocationService context) {
        super(context);
        PROVIDER_ID = 2;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        log = LoggerManager.getLogger(GpsLocationProvider.class);
        log.info("Creating GpsLocationProvider");

        locationManager = (LocationManager) locationService.getSystemService(Context.LOCATION_SERVICE);

        PowerManager pm = (PowerManager) locationService.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();
    }

    public void startRecording() {
        log.info("Start recording");
        setPace(false);
    }

    public void stopRecording() {
        log.info("stopRecording not implemented yet");
    }

    /**
     *
     * @param value set true to engage "aggressive", battery-consuming tracking, false for stationary-region tracking
     */
    private void setPace(Boolean value) {
        log.info("Setting pace: {}", value);

        try {
            locationManager.removeUpdates(this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        } catch (SecurityException e) {
            log.error("Security exception: {}", e.getMessage());
            this.handleSecurityException(e);
        }
    }

    public void onLocationChanged(Location location) {
        log.debug("Location change: {} isMoving={}", location.toString());

        if (config.isDebugging()) {
            Toast.makeText(locationService, "acy:" + location.getAccuracy() + ",v:" + location.getSpeed(), Toast.LENGTH_LONG).show();
        }
        // Go ahead and cache, push to server
        lastLocation = location;
        handleLocation(location);
    }

    /**
    * Broadcast receiver for receiving a single-update from LocationManager.
    */
    private BroadcastReceiver singleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String key = LocationManager.KEY_LOCATION_CHANGED;
            Location location = (Location)intent.getExtras().get(key);
            if (location != null) {
                log.debug("Single location update: " + location.toString());
            }
        }
    };


    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        log.debug("Provider {} was disabled", provider);
    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        log.debug("Provider {} was enabled", provider);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        log.debug("Provider {} status changed: {}", provider, status);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log.info("Destroying GpsLocationProvider");

        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            //noop
        }
        unregisterReceiver(singleUpdateReceiver);
        wakeLock.release();
    }
}
