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

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

        else if (action.equals("getPowerInfo")) {
            getPowerInfo(callbackContext);
            return true;
        }

        else if (action.equals("getEnhancedPowerMetrics")) {
            getEnhancedPowerMetrics(callbackContext);
            return true;
        }

        else if (action.equals("checkUsageStatsPermission")) {
            checkUsageStatsPermission(callbackContext);
            return true;
        }

        else if (action.equals("requestUsageStatsPermission")) {
            requestUsageStatsPermission(callbackContext);
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

    /**
     * Get current power/battery information including current draw and capacity
     * Note: Detailed power consumption breakdown (per-app, screen, system) is not available
     * to regular apps on Android 8.0+ due to security restrictions.
     */
    private void getPowerInfo(CallbackContext callbackContext) {
        try {
            JSONObject powerInfo = new JSONObject();
            Context context = webView.getContext();
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && batteryManager != null) {
                // Get current battery properties (Android 5.0+)
                try {
                    // Current instantaneous battery current in microamperes
                    int currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                    powerInfo.put("currentNow", currentNow); // in microamps (µA)
                    powerInfo.put("currentNowMA", currentNow / 1000.0); // convert to milliamps (mA)

                    // Average battery current in microamperes
                    int currentAvg = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
                    powerInfo.put("currentAverage", currentAvg);
                    powerInfo.put("currentAverageMA", currentAvg / 1000.0);

                    // Battery capacity in microampere-hours
                    int capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
                    powerInfo.put("chargeCounter", capacity);
                    powerInfo.put("chargeCounterMAH", capacity / 1000.0); // convert to mAh

                    // Battery capacity (design capacity)
                    int energyCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
                    powerInfo.put("energyCounter", energyCounter);

                    // Determine if charging or discharging
                    boolean isCharging = currentNow < 0; // Negative means discharging
                    powerInfo.put("isDischarging", isCharging);
                    powerInfo.put("status", isCharging ? "discharging" : "charging");

                    // Calculate estimated time remaining (rough estimate)
                    if (isCharging && currentAvg != 0) {
                        // Discharging - estimate time until empty
                        double hoursRemaining = Math.abs((double) capacity / currentAvg);
                        int minutesRemaining = (int) (hoursRemaining * 60);
                        powerInfo.put("estimatedMinutesRemaining", minutesRemaining);
                        powerInfo.put("estimatedHoursRemaining", hoursRemaining);
                    }

                    powerInfo.put("supported", true);
                } catch (Exception e) {
                    LOG.e(LOG_TAG, "Error getting battery properties", e);
                    powerInfo.put("supported", false);
                    powerInfo.put("error", e.getMessage());
                }
            } else {
                powerInfo.put("supported", false);
                powerInfo.put("message", "Power monitoring requires Android 5.0 (API 21) or higher");
            }

            // Add disclaimer about power consumption breakdown
            powerInfo.put("note", "Detailed power consumption breakdown (per-app, screen, system, network) is not available to regular apps on Android 8.0+ due to platform restrictions. Only current power draw and battery statistics are accessible.");

            callbackContext.success(powerInfo);
        } catch (Exception e) {
            LOG.e(LOG_TAG, "Error getting power info", e);
            callbackContext.error("Error getting power info: " + e.getMessage());
        }
    }

    /**
     * Get enhanced power metrics including screen-on time, usage stats, and drain rate
     * Available on Android 5.0+ with some features requiring Android 8.0+
     */
    private void getEnhancedPowerMetrics(CallbackContext callbackContext) {
        try {
            JSONObject metrics = new JSONObject();
            Context context = webView.getContext();

            // 1. Screen-on time since last charge (using SystemClock)
            long screenOnTime = getScreenOnTimeSinceLastCharge(context);
            metrics.put("screenOnTimeMs", screenOnTime);
            metrics.put("screenOnTimeMinutes", screenOnTime / 60000);
            metrics.put("screenOnTimeFormatted", formatDuration(screenOnTime));

            // 2. Device uptime (time since boot)
            long uptimeMs = SystemClock.elapsedRealtime();
            metrics.put("uptimeMs", uptimeMs);
            metrics.put("uptimeMinutes", uptimeMs / 60000);
            metrics.put("uptimeFormatted", formatDuration(uptimeMs));

            // 3. Battery drain rate calculation
            JSONObject drainRate = calculateBatteryDrainRate(context);
            metrics.put("drainRate", drainRate);

            // 4. Get current battery info for context
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && batteryManager != null) {
                int currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                int currentAvg = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
                int capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);

                metrics.put("currentDrawMA", Math.abs(currentNow / 1000.0));
                metrics.put("avgCurrentMA", Math.abs(currentAvg / 1000.0));
                metrics.put("remainingCapacityMAH", capacity / 1000.0);

                // Determine power state
                String powerState = "idle";
                double currentMA = Math.abs(currentNow / 1000.0);
                if (currentMA > 800) {
                    powerState = "heavy";
                } else if (currentMA > 400) {
                    powerState = "moderate";
                } else if (currentMA > 150) {
                    powerState = "light";
                }
                metrics.put("powerState", powerState);
            }

            // 5. Usage stats (requires PACKAGE_USAGE_STATS permission)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                boolean hasPermission = hasUsageStatsPermission(context);
                metrics.put("usageStatsPermission", hasPermission);

                if (hasPermission) {
                    JSONObject usageStats = getAppUsageStats(context);
                    metrics.put("usageStats", usageStats);
                } else {
                    metrics.put("usageStatsNote", "Grant Usage Access permission for detailed app usage data");
                }
            }

            // 6. Power saving mode status
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    metrics.put("isPowerSaveMode", pm.isPowerSaveMode());
                }
            }

            // 7. Battery optimization status
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    String packageName = context.getPackageName();
                    metrics.put("isIgnoringBatteryOptimizations", pm.isIgnoringBatteryOptimizations(packageName));
                }
            }

            metrics.put("supported", true);
            metrics.put("androidVersion", Build.VERSION.SDK_INT);

            callbackContext.success(metrics);
        } catch (Exception e) {
            LOG.e(LOG_TAG, "Error getting enhanced power metrics", e);
            callbackContext.error("Error: " + e.getMessage());
        }
    }

    /**
     * Check if usage stats permission is granted
     */
    private void checkUsageStatsPermission(CallbackContext callbackContext) {
        try {
            Context context = webView.getContext();
            boolean hasPermission = hasUsageStatsPermission(context);

            JSONObject result = new JSONObject();
            result.put("granted", hasPermission);
            result.put("required", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);

            callbackContext.success(result);
        } catch (Exception e) {
            callbackContext.error("Error checking permission: " + e.getMessage());
        }
    }

    /**
     * Open system settings to request usage stats permission
     */
    private void requestUsageStatsPermission(CallbackContext callbackContext) {
        try {
            Context context = webView.getContext();
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            callbackContext.success("Settings opened");
        } catch (Exception e) {
            callbackContext.error("Error opening settings: " + e.getMessage());
        }
    }

    /**
     * Check if app has usage stats permission
     */
    private boolean hasUsageStatsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                if (usm == null) return false;

                long now = System.currentTimeMillis();
                List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60000, now);
                return stats != null && !stats.isEmpty();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Get app usage statistics
     */
    private JSONObject getAppUsageStats(Context context) {
        JSONObject stats = new JSONObject();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                if (usm == null) return stats;

                // Get stats for the last 24 hours
                long endTime = System.currentTimeMillis();
                long startTime = endTime - (24 * 60 * 60 * 1000); // 24 hours ago

                List<UsageStats> usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);

                if (usageStatsList != null && !usageStatsList.isEmpty()) {
                    // Calculate total foreground time
                    long totalForegroundTime = 0;
                    int appCount = 0;
                    JSONArray topApps = new JSONArray();

                    // Sort by foreground time
                    java.util.Collections.sort(usageStatsList, (a, b) ->
                        Long.compare(b.getTotalTimeInForeground(), a.getTotalTimeInForeground()));

                    // Get top 10 apps by usage
                    int count = 0;
                    for (UsageStats usageStats : usageStatsList) {
                        long foregroundTime = usageStats.getTotalTimeInForeground();
                        if (foregroundTime > 60000) { // More than 1 minute
                            totalForegroundTime += foregroundTime;
                            appCount++;

                            if (count < 10) {
                                JSONObject appStat = new JSONObject();
                                appStat.put("packageName", usageStats.getPackageName());
                                appStat.put("foregroundTimeMs", foregroundTime);
                                appStat.put("foregroundTimeFormatted", formatDuration(foregroundTime));
                                topApps.put(appStat);
                                count++;
                            }
                        }
                    }

                    stats.put("totalForegroundTimeMs", totalForegroundTime);
                    stats.put("totalForegroundTimeFormatted", formatDuration(totalForegroundTime));
                    stats.put("activeAppCount", appCount);
                    stats.put("topApps", topApps);
                    stats.put("periodHours", 24);
                }
            } catch (Exception e) {
                LOG.e(LOG_TAG, "Error getting usage stats", e);
            }
        }

        return stats;
    }

    /**
     * Calculate battery drain rate based on stored history
     */
    private JSONObject calculateBatteryDrainRate(Context context) {
        JSONObject drainRate = new JSONObject();

        try {
            SharedPreferences prefs = context.getSharedPreferences("battery_history", Context.MODE_PRIVATE);

            // Get current battery level
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            int level = batteryStatus != null ?
                batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ?
                batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, 100) : 100;
            int status = batteryStatus != null ?
                batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1) : -1;

            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL;

            float batteryPct = level * 100 / (float) scale;
            long now = System.currentTimeMillis();

            drainRate.put("currentLevel", batteryPct);
            drainRate.put("isCharging", isCharging);

            // Store current reading
            SharedPreferences.Editor editor = prefs.edit();

            if (!isCharging) {
                // Get last recorded values
                float lastLevel = prefs.getFloat("last_level", -1);
                long lastTime = prefs.getLong("last_time", -1);

                if (lastLevel >= 0 && lastTime > 0 && lastLevel > batteryPct) {
                    // Calculate drain rate
                    float levelDrop = lastLevel - batteryPct;
                    long timeDiffMs = now - lastTime;
                    float timeDiffHours = timeDiffMs / (1000.0f * 60 * 60);

                    if (timeDiffHours > 0.05) { // At least 3 minutes
                        float drainPerHour = levelDrop / timeDiffHours;
                        float estimatedHoursRemaining = batteryPct / drainPerHour;

                        drainRate.put("drainPerHour", Math.round(drainPerHour * 10) / 10.0);
                        drainRate.put("estimatedHoursRemaining", Math.round(estimatedHoursRemaining * 10) / 10.0);
                        drainRate.put("estimatedTimeFormatted", formatDuration((long)(estimatedHoursRemaining * 60 * 60 * 1000)));
                        drainRate.put("measurementPeriodMinutes", Math.round(timeDiffMs / 60000.0));

                        // Categorize drain rate
                        String drainCategory;
                        if (drainPerHour > 20) {
                            drainCategory = "very_high";
                        } else if (drainPerHour > 10) {
                            drainCategory = "high";
                        } else if (drainPerHour > 5) {
                            drainCategory = "moderate";
                        } else if (drainPerHour > 2) {
                            drainCategory = "low";
                        } else {
                            drainCategory = "very_low";
                        }
                        drainRate.put("drainCategory", drainCategory);
                    }
                }

                // Update stored values for next calculation
                editor.putFloat("last_level", batteryPct);
                editor.putLong("last_time", now);
            } else {
                // Reset when charging
                editor.remove("last_level");
                editor.remove("last_time");
                drainRate.put("note", "Device is charging");
            }

            editor.apply();

        } catch (Exception e) {
            LOG.e(LOG_TAG, "Error calculating drain rate", e);
        }

        return drainRate;
    }

    /**
     * Get screen-on time estimation
     */
    private long getScreenOnTimeSinceLastCharge(Context context) {
        // This is an approximation using device uptime
        // True screen-on time would require UsageStatsManager with special permission
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && hasUsageStatsPermission(context)) {
                UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                if (usm != null) {
                    long endTime = System.currentTimeMillis();
                    long startTime = endTime - (24 * 60 * 60 * 1000); // Last 24 hours

                    List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
                    long totalForeground = 0;
                    if (stats != null) {
                        for (UsageStats stat : stats) {
                            totalForeground += stat.getTotalTimeInForeground();
                        }
                    }
                    return totalForeground;
                }
            }
        } catch (Exception e) {
            LOG.e(LOG_TAG, "Error getting screen on time", e);
        }

        // Fallback: return -1 to indicate not available
        return -1;
    }

    /**
     * Format duration in milliseconds to readable string
     */
    private String formatDuration(long durationMs) {
        if (durationMs < 0) return "N/A";

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
