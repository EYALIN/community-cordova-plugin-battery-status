/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.batterystatus;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BatteryListener extends CordovaPlugin {

    private static final String LOG_TAG = "BatteryManager";

    BroadcastReceiver receiver;

    private CallbackContext batteryCallbackContext = null;

    /**
     * Constructor.
     */
    public BatteryListener() {
        this.receiver = null;
    }

    /**
     * Executes the request.
     *
     * @param action        	The action to execute.
     * @param args          	JSONArry of arguments for the plugin.
     * @param callbackContext 	The callback context used when calling back into JavaScript.
     * @return              	True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        if (action.equals("start")) {
            if (this.batteryCallbackContext != null) {
                removeBatteryListener();
            }
            this.batteryCallbackContext = callbackContext;

            // We need to listen to power events to update battery status
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            if (this.receiver == null) {
                this.receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        updateBatteryInfo(intent);
                    }
                };
                webView.getContext().registerReceiver(this.receiver, intentFilter);
            }

            // Don't return any result now, since status results will be sent when events come in from broadcast receiver
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;
        }

        else if (action.equals("stop")) {
            removeBatteryListener();
            this.sendUpdate(new JSONObject(), false); // release status callback in JS side
            this.batteryCallbackContext = null;
            callbackContext.success();
            return true;
        }

        return false;
    }

    /**
     * Stop battery receiver.
     */
    public void onDestroy() {
        removeBatteryListener();
    }

    /**
     * Stop battery receiver.
     */
    public void onReset() {
        removeBatteryListener();
    }

    /**
     * Stop the battery receiver and set it to null.
     */
    private void removeBatteryListener() {
        if (this.receiver != null) {
            try {
                webView.getContext().unregisterReceiver(this.receiver);
                this.receiver = null;
            } catch (Exception e) {
                LOG.e(LOG_TAG, "Error unregistering battery receiver: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Creates a JSONObject with the current battery information
     *
     * @param batteryIntent the current battery information
     * @return a JSONObject containing the battery status information
     */
    private JSONObject getBatteryInfo(Intent batteryIntent) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("level", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0));
            obj.put("isPlugged", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) > 0 ? true : false);
            int chargeType = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1);
            if(chargeType == android.os.BatteryManager.BATTERY_PLUGGED_AC){
                obj.put("chargeType", "AC charger");
            }
            if(chargeType == android.os.BatteryManager.BATTERY_PLUGGED_USB){
                 obj.put("chargeType", "USB port");
            }
            if(chargeType == android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS){
                 obj.put("chargeType", "Wireless");
            }
            if(chargeType == -1){
                obj.put("chargeType", "Battery");
            }
            float voltage = ((float) batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0)) / 1000;
            obj.put("voltageLevel", voltage);
            float temperature = ((float) batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0)) / 10;
            obj.put("temperature", temperature);
            String technology = batteryIntent.getExtras().getString(android.os.BatteryManager.EXTRA_TECHNOLOGY);
            obj.put("technology", technology);
            boolean present  = batteryIntent.getExtras().getBoolean(android.os.BatteryManager.EXTRA_PRESENT);
            obj.put("present", present);
            obj.put("status", batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS,0));
            int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
             // On some phones, scale is always 0.
                if (scale == 0)
                    scale = 100;
            obj.put("scale", scale);

             int deviceHealth = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH,0);

            if(deviceHealth == android.os.BatteryManager.BATTERY_HEALTH_COLD){
                obj.put("currentBatteryHealth", "Cold");
            }

            if(deviceHealth == android.os.BatteryManager.BATTERY_HEALTH_DEAD){
                obj.put("currentBatteryHealth", "Dead");
            }

            if (deviceHealth == android.os.BatteryManager.BATTERY_HEALTH_GOOD){
                obj.put("currentBatteryHealth", "Good");
            }

            if(deviceHealth == android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT){
                obj.put("currentBatteryHealth", "OverHeat");
            }

            if (deviceHealth == android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE){
                obj.put("currentBatteryHealth", "Over voltage");
            }

            if (deviceHealth == android.os.BatteryManager.BATTERY_HEALTH_UNKNOWN){
                obj.put("currentBatteryHealth", "Unknown");
            }
            if (deviceHealth == android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE){
                obj.put("currentBatteryHealth", "Unspecified Failure");
            }
        } catch (JSONException e) {
            LOG.e(LOG_TAG, e.getMessage(), e);
        }
        return obj;
    }

    /**
     * Updates the JavaScript side whenever the battery changes
     *
     * @param batteryIntent the current battery information
     * @return
     */
    private void updateBatteryInfo(Intent batteryIntent) {
        sendUpdate(this.getBatteryInfo(batteryIntent), true);
    }

    /**
     * Create a new plugin result and send it back to JavaScript
     *
     * @param connection the network info to set as navigator.connection
     */
    private void sendUpdate(JSONObject info, boolean keepCallback) {
        if (this.batteryCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(keepCallback);
            this.batteryCallbackContext.sendPluginResult(result);
        }
    }
}
