
# Hades Mobile Backend cocos2d-x sample

This cocos2d-x sample provides full native implementation of push notification for Android and iOS.

Note that both implementations hard code the userid and perform the registration process on app init. This is for sample purposes only. On a real situation the registration process should be delayed until userid is known by the game (after login, or similar).

## Android

https://developer.android.com/google/gcm/client.html

* proj.android/AndroidManifest.xml

`AndroidManifest.xml` has all relevant sections added or modified to allow receiving GCM messages. 

* proj.android/src/org/cocos2dx/cpp/AppActivity.java
* proj.android/src/net/qb9/hades/HadesConfig.java
* proj.android/src/net/qb9/hades/GCMBroadcastReceiver.java
* proj.android/src/net/qb9/hades/GCMIntentService.java
* proj.android/src/net/qb9/hades/GCMHelper.java

`HadesConfig.java` holds configuration constants.

`AppActivity.java` will launch the device registration process after checking the availability of Play Services.

`GCMHelper.java` has helper methods to call the REST endpoint to register the device token.

`GCMIntentService.java` will handle incoming messages and build the notification.

Extra notification parameters can be obtained from the `Intent` object as shown in the `processNotification` method in `AppActivity.java`.

## iOS

https://developer.apple.com/library/ios/documentation/NetworkingInternet/Conceptual/RemoteNotificationsPG/Chapters/IPhoneOSClientImp.html

* proj.ios_mac/ios/AppController.mm

The `didFinishLaunchingWithOptions` method requests the device token to APN server and, in `didRegisterForRemoteNotificationsWithDeviceToken` method, it base64 encodes it and sends it over to the REST registration endpoint.

Please note that throughout the code `gameid`, `userid` and HMAC client key `cKey`are all hardcoded.

Extra notification parameters can be retrieved on the `didReceiveRemoteNotification` method.
