/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.android_scripting.facade;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings.SettingNotFoundException;
import android.view.WindowManager;

import com.googlecode.android_scripting.BaseApplication;
import com.googlecode.android_scripting.FutureActivityTaskExecutor;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.future.FutureActivityTask;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcOptional;
import com.googlecode.android_scripting.rpc.RpcParameter;

import java.lang.reflect.Method;

/**
 * Exposes phone settings functionality.
 *
 * @author Frank Spychalski (frank.spychalski@gmail.com)
 */
public class SettingsFacade extends RpcReceiver {

    public static int AIRPLANE_MODE_OFF = 0;
    public static int AIRPLANE_MODE_ON = 1;

    private final Service mService;
    private final AudioManager mAudio;
    private final PowerManager mPower;
    private final AlarmManager mAlarm;
    private final ConnectivityManager mConnect;

    /**
     * Creates a new SettingsFacade.
     *
     * @param service is the {@link Context} the APIs will run under
     */
    public SettingsFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mAudio = (AudioManager) mService.getSystemService(Context.AUDIO_SERVICE);
        mPower = (PowerManager) mService.getSystemService(Context.POWER_SERVICE);
        mAlarm = (AlarmManager) mService.getSystemService(Context.ALARM_SERVICE);
        mConnect = (ConnectivityManager) mService.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Rpc(description = "Sets the screen timeout to this number of seconds.",
            returns = "The original screen timeout.")
    public Integer setScreenTimeout(@RpcParameter(name = "value") Integer value) {
        Integer oldValue = getScreenTimeout();
        android.provider.Settings.System.putInt(mService.getContentResolver(),
                android.provider.Settings.System.SCREEN_OFF_TIMEOUT, value * 1000);
        return oldValue;
    }

    @Rpc(description = "Returns the current screen timeout in seconds.",
            returns = "the current screen timeout in seconds.")
    public Integer getScreenTimeout() {
        try {
            return android.provider.Settings.System.getInt(mService.getContentResolver(),
                    android.provider.Settings.System.SCREEN_OFF_TIMEOUT) / 1000;
        } catch (SettingNotFoundException e) {
            return 0;
        }
    }

    @Rpc(description = "Checks the airplane mode setting.",
            returns = "True if airplane mode is enabled.")
    public Boolean checkAirplaneMode() {
        try {
            return android.provider.Settings.System.getInt(mService.getContentResolver(),
                    android.provider.Settings.Global.AIRPLANE_MODE_ON) == AIRPLANE_MODE_ON;
        } catch (SettingNotFoundException e) {
            return false;
        }
    }

    @Rpc(description = "Toggles airplane mode on and off.",
            returns = "True if airplane mode is enabled.")
    public void toggleAirplaneMode(@RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
        if (enabled == null) {
            enabled = !checkAirplaneMode();
        }
        mConnect.setAirplaneMode(enabled);
    }

    @Rpc(description = "Checks the ringer silent mode setting.",
            returns = "True if ringer silent mode is enabled.")
    public Boolean checkRingerSilentMode() {
        return mAudio.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
    }

    @Rpc(description = "Toggles ringer silent mode on and off.",
            returns = "True if ringer silent mode is enabled.")
    public Boolean toggleRingerSilentMode(
            @RpcParameter(name = "enabled") @RpcOptional Boolean enabled) {
        if (enabled == null) {
            enabled = !checkRingerSilentMode();
        }
        mAudio.setRingerMode(enabled ? AudioManager.RINGER_MODE_SILENT
                : AudioManager.RINGER_MODE_NORMAL);
        return enabled;
    }

    @Rpc(description = "Set the ringer to a specified mode")
    public void setRingerMode(@RpcParameter(name = "mode") Integer mode) throws Exception {
        if (AudioManager.isValidRingerMode(mode)) {
            mAudio.setRingerMode(mode);
        } else {
            throw new Exception("Ringer mode " + mode + " does not exist.");
        }
    }

    @Rpc(description = "Returns the current ringtone mode.",
            returns = "An integer representing the current ringer mode")
    public Integer getRingerMode() {
        return mAudio.getRingerMode();
    }

    @Rpc(description = "Returns the maximum ringer volume.")
    public int getMaxRingerVolume() {
        return mAudio.getStreamMaxVolume(AudioManager.STREAM_RING);
    }

    @Rpc(description = "Returns the current ringer volume.")
    public int getRingerVolume() {
        return mAudio.getStreamVolume(AudioManager.STREAM_RING);
    }

    @Rpc(description = "Sets the ringer volume.")
    public void setRingerVolume(@RpcParameter(name = "volume") Integer volume) {
        mAudio.setStreamVolume(AudioManager.STREAM_RING, volume, 0);
    }

    @Rpc(description = "Returns the maximum media volume.")
    public int getMaxMediaVolume() {
        return mAudio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    @Rpc(description = "Returns the current media volume.")
    public int getMediaVolume() {
        return mAudio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    @Rpc(description = "Sets the media volume.")
    public void setMediaVolume(@RpcParameter(name = "volume") Integer volume) {
        mAudio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

    @Rpc(description = "Returns the screen backlight brightness.",
            returns = "the current screen brightness between 0 and 255")
    public Integer getScreenBrightness() {
        try {
            return android.provider.Settings.System.getInt(mService.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS);
        } catch (SettingNotFoundException e) {
            return 0;
        }
    }

    @Rpc(description = "return the system time since boot in nanoseconds")
    public long getSystemElapsedRealtimeNanos() {
        return SystemClock.elapsedRealtimeNanos();
    }

    @Rpc(description = "Sets the the screen backlight brightness.",
            returns = "the original screen brightness.")
    public Integer setScreenBrightness(
            @RpcParameter(name = "value", description = "brightness value between 0 and 255") Integer value) {
        if (value < 0) {
            value = 0;
        } else if (value > 255) {
            value = 255;
        }
        final int brightness = value;
        Integer oldValue = getScreenBrightness();
        android.provider.Settings.System.putInt(mService.getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS, brightness);

        FutureActivityTask<Object> task = new FutureActivityTask<Object>() {
            @Override
            public void onCreate() {
                super.onCreate();
                WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
                lp.screenBrightness = brightness * 1.0f / 255;
                getActivity().getWindow().setAttributes(lp);
                setResult(null);
                finish();
            }
        };

        FutureActivityTaskExecutor taskExecutor =
                ((BaseApplication) mService.getApplication()).getTaskExecutor();
        taskExecutor.execute(task);

        return oldValue;
    }

    @Rpc(description = "Checks if the screen is on or off (requires API level 7).",
            returns = "True if the screen is currently on.")
    public Boolean checkScreenOn() throws Exception {
        Class<?> powerManagerClass = mPower.getClass();
        Boolean result = null;
        try {
            Method isScreenOn = powerManagerClass.getMethod("isScreenOn");
            result = (Boolean) isScreenOn.invoke(mPower);
        } catch (Exception e) {
            Log.e(e);
            throw new UnsupportedOperationException("This feature is only available after Eclair.");
        }
        return result;
    }

    @Rpc(description = "Wakeup screen(requires API level 19).")
    public void wakeupScreen() throws Exception {
        Class<?> powerManagerClass = mPower.getClass();
        try {
            Method wakeUp = powerManagerClass.getMethod("wakeUp", long.class);
            wakeUp.invoke(mPower, SystemClock.uptimeMillis());
        } catch (Exception e) {
            Log.e(e);
            throw new UnsupportedOperationException("This feature is only available after Kitkat.");
        }
    }

    @Rpc(description = "Get Up time of device.",
            returns = "Long value of device up time in milliseconds.")
    public long getDeviceUpTime() throws Exception {
        return SystemClock.elapsedRealtime();
    }

    @Rpc(description = "Set the system time in epoch.")
    public void setTime(Long currentTime) {
        mAlarm.setTime(currentTime);
    }

    @Override
    public void shutdown() {
        // Nothing to do yet.
    }
}
