// Type definitions for cordova-plugin-battery-status
// Project: https://github.com/apache/cordova-plugin-battery-status
// Definitions by: Microsoft Open Technologies Inc <http://msopentech.com>
//                 Tim Brust <https://github.com/timbru31>
// Definitions: https://github.com/DefinitelyTyped/DefinitelyTyped

type batteryEvent = 'batterystatus' | 'batterycritical' | 'batterylow';

interface Window {
    onbatterystatus: (type: BatteryStatusEvent) => void;
    onbatterycritical: (type: BatteryStatusEvent) => void;
    onbatterylow: (type: BatteryStatusEvent) => void;
    /**
     * Adds a listener for an event from the BatteryStatus plugin.
     * @param type       - The event to listen for.
     *
     *                     `batterystatus`: event fires when the percentage of battery charge changes by at least 1 percent, or if the device is plugged in or unplugged.
     *
     *                     `batterycritical`: event fires when the percentage of battery charge has reached the critical battery threshold. The value is device-specific.
     *
     *                     `batterylow`: event fires when the percentage of battery charge has reached the low battery threshold, device-specific value.
     * @param listener   - The function that executes when the event fires. The function is passed an BatteryStatusEvent object as a parameter.
     * @param useCapture - A Boolean indicating whether events of this type will be dispatched to the registered listener before being dispatched to any EventTarget beneath it in the DOM tree.
     */
    addEventListener(type: batteryEvent, listener: (ev: BatteryStatusEvent) => any, useCapture?: boolean): void;
    /**
     * Removes a listener for an event from the BatteryStatus plugin.
     * @param Atype      - The event to stop listening for.
     *
     *                     `batterystatus`: event fires when the percentage of battery charge changes by at least 1 percent, or if the device is plugged in or unplugged.
     *
     *                     `batterycritical`: event fires when the percentage of battery charge has reached the critical battery threshold. The value is device-specific.
     *
     *                     `batterylow`: event fires when the percentage of battery charge has reached the low battery threshold, device-specific value.
     * @param callback   - The function that executes when the event fires. The function is passed an BatteryStatusEvent object as a parameter.
     * @param useCapture - A Boolean indicating whether events of this type will be dispatched to the registered listener before being dispatched to any EventTarget beneath it in the DOM tree.
     */
    removeEventListener(type: batteryEvent, listener: (ev: BatteryStatusEvent) => any, useCapture?: boolean): void;
}

interface BatteryStatusEvent extends Event {
	/* The percentage of battery charge (0-100). */
    level: number;
	/* A boolean that indicates whether the device is plugged in. */
    isPlugged: boolean;
    /* A string that indicates charging type. */
    chargeType: string;
    /* A string that indicates the battery technology. */
    technology: string;
    /* A number that indicates the battery temperature. */
    temperature: number;
    /* A boolean that indicates whether the device is present. */
    present: boolean;
    /* The scale of the battery */
    scale: number;
    /* integer containing the current battery voltage level. */
    voltageLevel: number;
    /* a string indicates Health level */
    currentBatteryHealth: string;
}

/** Drain rate information */
export interface IDrainRate {
    drainPerHour: number;
    drainCategory: 'low' | 'moderate' | 'high' | 'very_high';
    estimatedHoursRemaining: number;
    estimatedTimeFormatted: string;
    isCharging: boolean;
}

/** Usage stats information (Android 5.0+) */
export interface IUsageStats {
    totalForegroundTime: number;
    totalForegroundTimeFormatted: string;
    appCount: number;
}

/** Enhanced power metrics (Android 5.0+) */
export interface IEnhancedPowerMetrics {
    supported: boolean;
    currentDrawMA?: number;
    powerState?: 'idle' | 'light' | 'moderate' | 'heavy';
    drainRate?: IDrainRate;
    screenOnTime?: number;
    screenOnTimeFormatted?: string;
    isPowerSaveMode?: boolean;
    usageStats?: IUsageStats;
    usageStatsPermission?: boolean;
    uptimeFormatted?: string;
}

/** Basic power info (Android) */
export interface IPowerInfo {
    supported: boolean;
    currentNowMA?: number;
    currentAverageMA?: number;
    chargeCounterMAH?: number;
    energyCounterNWH?: number;
    estimatedHoursRemaining?: number;
    isDischarging?: boolean;
}

/** Battery Manager class for power metrics */
export default class BatteryManager {
    /**
     * Get enhanced power metrics including drain rate, screen time, and usage stats
     * @returns Promise with enhanced power metrics
     */
    getEnhancedPowerMetrics(): Promise<IEnhancedPowerMetrics>;

    /**
     * Get basic power info from BatteryManager API
     * @returns Promise with power info
     */
    getPowerInfo(): Promise<IPowerInfo>;

    /**
     * Check if usage stats permission is granted (Android)
     * @returns Promise with boolean
     */
    checkUsageStatsPermission(): Promise<boolean>;

    /**
     * Request usage stats permission (opens system settings on Android)
     */
    requestUsageStatsPermission(): Promise<void>;
}
