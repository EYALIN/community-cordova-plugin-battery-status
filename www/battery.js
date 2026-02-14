/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

var PLUGIN_NAME = 'Battery';

var cordova = require('cordova');
var exec = require('cordova/exec');

var STATUS_CRITICAL = 5;
var STATUS_LOW = 20;

class BatteryManager {
    constructor () {
        this._level = null;
        this._isPlugged = null;
        // Create new event handlers on the window (returns a channel instance)
        this.channels = {
            batterystatus: cordova.addWindowEventHandler('batterystatus'),
            batterylow: cordova.addWindowEventHandler('batterylow'),
            batterycritical: cordova.addWindowEventHandler('batterycritical')
        };
        for (var key in this.channels) {
            this.channels[key].onHasSubscribersChange = this._onHasSubscribersChange.bind(this);
        }
    }

    _handlers () {
        return (
            this.channels.batterystatus.numHandlers +
            this.channels.batterylow.numHandlers +
            this.channels.batterycritical.numHandlers
        );
    }

    _onHasSubscribersChange () {
        // If we just registered the first handler, make sure native listener is started.
        if (this._handlers() === 1) {
            exec(this._status.bind(this), this._error.bind(this), PLUGIN_NAME, 'start', []);
        } else if (this._handlers() === 0) {
            exec(null, null, PLUGIN_NAME, 'stop', []);
        }
    }

    _status (info) {
        if (info) {
            if (this._level !== info.level || this._isPlugged !== info.isPlugged) {
                if (info.level === null && this._level !== null) {
                    return; // special case where callback is called because we stopped listening to the native side.
                }

                // Something changed. Fire batterystatus event
                cordova.fireWindowEvent('batterystatus', info);

                // do not fire low/critical if we are charging. issue: CB-4520
                if (!info.isPlugged) {
                    // note the following are NOT exact checks, as we want to catch a transition from
                    // above the threshold to below. issue: CB-4519
                    if (this._level > STATUS_CRITICAL && info.level <= STATUS_CRITICAL) {
                        // Fire critical battery event
                        cordova.fireWindowEvent('batterycritical', info);
                    } else if (this._level > STATUS_LOW && info.level <= STATUS_LOW) {
                        // Fire low battery event
                        cordova.fireWindowEvent('batterylow', info);
                    }
                }
                this._level = info.level;
                this._isPlugged = info.isPlugged;
            }
        }
    }

    _error (e) {
        console.log('Error initializing Battery: ' + e);
    }

    /**
     * Get enhanced power metrics including drain rate, screen time, and usage stats
     * @returns Promise with enhanced power metrics
     */
    getEnhancedPowerMetrics () {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, PLUGIN_NAME, 'getEnhancedPowerMetrics', []);
        });
    }

    /**
     * Get basic power info from BatteryManager API
     * @returns Promise with power info
     */
    getPowerInfo () {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, PLUGIN_NAME, 'getPowerInfo', []);
        });
    }

    /**
     * Check if usage stats permission is granted (Android)
     * @returns Promise with boolean
     */
    checkUsageStatsPermission () {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, PLUGIN_NAME, 'checkUsageStatsPermission', []);
        });
    }

    /**
     * Request usage stats permission (opens system settings on Android)
     */
    requestUsageStatsPermission () {
        return new Promise((resolve, reject) => {
            exec(resolve, reject, PLUGIN_NAME, 'requestUsageStatsPermission', []);
        });
    }
}

module.exports = new BatteryManager();
