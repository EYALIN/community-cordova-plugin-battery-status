[![NPM version](https://img.shields.io/npm/v/community-cordova-plugin-battery-status)](https://www.npmjs.com/package/community-cordova-plugin-battery-status)


### This is a fork of the original plugin cordova-plugin-battery-status
# cordova-plugin-battery-status


I dedicate a considerable amount of my free time to developing and maintaining many cordova plugins for the community ([See the list with all my maintained plugins][community_plugins]).
To help ensure this plugin is kept updated,
new features are added and bugfixes are implemented quickly,
please donate a couple of dollars (or a little more if you can stretch) as this will help me to afford to dedicate time to its maintenance.
Please consider donating if you're using this plugin in an app that makes you money, 
or if you're asking for new features or priority bug fixes. Thank you!

[![](https://img.shields.io/static/v1?label=Sponsor%20Me&style=for-the-badge&message=%E2%9D%A4&logo=GitHub&color=%23fe8e86)](https://github.com/sponsors/eyalin)


[comment]: <> ([![Downloads]&#40;https://img.shields.io/npm/dm/community-cordova-plugin-battery-status&#41;]&#40;https://www.npmjs.com/package/community-cordova-plugin-battery-status&#41;)




---
title: Battery Status
description: Get events for device battery level.
---
<!--
# license: Licensed to the Apache Software Foundation (ASF) under one
#         or more contributor license agreements.  See the NOTICE file
#         distributed with this work for additional information
#         regarding copyright ownership.  The ASF licenses this file
#         to you under the Apache License, Version 2.0 (the
#         "License"); you may not use this file except in compliance
#         with the License.  You may obtain a copy of the License at
#
#           http://www.apache.org/licenses/LICENSE-2.0
#
#         Unless required by applicable law or agreed to in writing,
#         software distributed under the License is distributed on an
#         "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#         KIND, either express or implied.  See the License for the
#         specific language governing permissions and limitations
#         under the License.
-->



[![Android Testsuite](https://github.com/apache/cordova-plugin-battery-status/actions/workflows/android.yml/badge.svg)](https://github.com/apache/cordova-plugin-battery-status/actions/workflows/android.yml) [![Chrome Testsuite](https://github.com/apache/cordova-plugin-battery-status/actions/workflows/chrome.yml/badge.svg)](https://github.com/apache/cordova-plugin-battery-status/actions/workflows/chrome.yml) [![iOS Testsuite](https://github.com/apache/cordova-plugin-battery-status/actions/workflows/ios.yml/badge.svg)](https://github.com/apache/cordova-plugin-battery-status/actions/workflows/ios.yml) [![Lint Test](https://github.com/apache/cordova-plugin-battery-status/actions/workflows/lint.yml/badge.svg)](https://github.com/apache/cordova-plugin-battery-status/actions/workflows/lint.yml)

This plugin provides an implementation of an old version of the [Battery Status Events API][w3c_spec]. It adds the following three events to the `window` object:

* batterystatus
* batterycritical
* batterylow

Applications may use `window.addEventListener` to attach an event listener for any of the above events after the `deviceready` event fires.

## Installation

    cordova plugin add community-cordova-plugin-battery-status

## Status object

All events in this plugin return an object with the following properties:

- __level__: The battery charge percentage (0-100). _(Number)_
- __isPlugged__: A boolean that indicates whether the device is plugged in. _(Boolean)_
from version 2.1.0 (for Android only for now) we added the following:
  - __chargeType__: A string that indicates charging type. _(string)_
  - __temperature__: A number that indicates the battery temperature. _(number)_
  - __technology__: A string that indicates the battery technology. _(string)_
  - __present__: The scale of the battery _(number)_
  - __voltageLevel__: containing the current battery voltage level. _(number)_
  - __currentBatteryHealth__:  a string indicates Health level. _(string)_
## batterystatus event

Fires when the battery charge percentage changes by at least 1 percent, or when the device is plugged in or unplugged. Returns an [object][status_object] containing battery status.

### Example

    window.addEventListener("batterystatus", onBatteryStatus, false);

    function onBatteryStatus(status) {
        console.log("Level: " + status.level + " isPlugged: " + status.isPlugged);
    }

### Supported Platforms

- iOS
- Android
- Windows
- Browser (Chrome, Firefox, Opera)

### Quirks: Android

**Warning**: the Android implementation is greedy and prolonged use will drain the device's battery.

## batterylow event

Fires when the battery charge percentage reaches the low charge threshold. This threshold value is device-specific. Returns an [object][status_object] containing battery status.

### Example

    window.addEventListener("batterylow", onBatteryLow, false);

    function onBatteryLow(status) {
        alert("Battery Level Low " + status.level + "%");
    }

### Supported Platforms

- iOS
- Android
- Windows
- Browser (Chrome, Firefox, Opera)

## batterycritical event

Fires when the battery charge percentage reaches the critical charge threshold. This threshold value is device-specific. Returns an [object][status_object] containing battery status.

### Example

    window.addEventListener("batterycritical", onBatteryCritical, false);

    function onBatteryCritical(status) {
        alert("Battery Level Critical " + status.level + "%\nRecharge Soon!");
    }

### Supported Platforms

- iOS
- Android
- Windows
- Browser (Chrome, Firefox, Opera)


[w3c_spec]: https://www.w3.org/TR/battery-status/
[status_object]: #status-object
[community_plugins]: https://github.com/EYALIN?tab=repositories&q=community&type=&language=&sort=
