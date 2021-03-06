[![Build Status](https://github.com/igniterealtime/openfire-pushserver-plugin/workflows/Java%20CI/badge.svg)](https://github.com/igniterealtime/openfire-pushserver-plugin/actions)

# Openfire Push Server Plugin
An [XEP-0357: Push Notifications](https://xmpp.org/extensions/xep-0357.html) app server that relays push messages between the user’s server and FCM (Firebase Cloud Messaging) or APNS (Apple Push Notification Service).

Building
--------

This project is using the Maven-based Openfire build process, as introduced in Openfire 4.2.0. To build this plugin locally, ensure that the following are available on your local host:

* A Java Development Kit, version 7 or (preferably) 8
* Apache Maven 3

To build this project, invoke on a command shell:

    $ mvn clean package

Upon completion, the openfire plugin will be available in `target/pushserver-openfire-plugin-assembly.jar`. This file should be renamed to `pushserver.jar`

Installation
------------
Copy `pushserver.jar` into the plugins directory of your Openfire server, or use the Openfire Admin Console to upload the plugin. The plugin will then be automatically deployed.

To upgrade to a new version, copy the new `pushserver.jar` file over the existing file.

Configuration
------------

After installing plugin, you should fill the fields under the page `Server -> Server Settings -> Push Server Properties.

**Note:** After filling and saving values, you should restart the plugin.

### iOS

Push Server Plugin uses token-based authentication to communicate with Apple Push Notification service. (For more information, see [Establishing a Token-Based Connection to APNs](https://developer.apple.com/documentation/usernotifications/setting_up_a_remote_notification_server/establishing_a_token-based_connection_to_apns))

* **Bundle Id** ([Bundle IDs](https://developer.apple.com/documentation/appstoreconnectapi/bundle_ids))
* **Team Id** ([Locate your Team ID](https://help.apple.com/developer-account/#/dev55c3c710c))
* **Key Id**
* **Encryption Key**

### Android

Push Server Plugin uses Firebase Cloud Messaging to send push notifications to Android clients. In order to activate sending push notifications to Android clients, you should create a Firebase project and add Firebase to your Android application. (For more information, see [Add Firebase to your Android project](https://firebase.google.com/docs/android/setup?authuser=0&hl=en))

* **FCM Project Id**
* **Google Cloud Service Account Key** (Service Account Key Json File)

Both of these can be found in your Firebase console, under Project Settings:
* The ID is in the "General" tab
* The Service Account JSON file can be created and downloaded from the "Service Account" tab. (or [ Create service account key using Google Console ](https://cloud.google.com/iam/docs/creating-managing-service-account-keys?hl=en#creating))

Client Configuration
------------

### Registering Devices

* Clients need to register with push server and get node and secret information

### iOS

```xml
<iq from="user@example.com/mobile" id="x20" to="push.example.com" type="set">
    <command xmlns="http://jabber.org/protocol/commands" action="execute" node="register-push-apns">
        <x xmlns="jabber:x:data" type="submit">
            <field var="token">
                <value>243CA5F2A4EE00B66E3208CD05C962A30EFA80B2D9F8DD508CE8182E04EAB695</value>
            </field>
            <field var="device-id">
                <value>0523AD60-ADD0-45A4-8D05-DC1AC59BB1CA</value>
            </field>
        </x>
    </command>
</iq>
```

```xml
<iq from="push.example.com" id="x20" to="user@example.com/mobile" type="result">
    <command xmlns="http://jabber.org/protocol/commands" action="complete" node="register-push-apns">
        <x xmlns="jabber:x:data" type="form">
            <field type="text-single" var="node">
                <value>37Ni514izxHG</value>
            </field>
            <field type="text-single" var="secret">
                <value>Zt9z9wOtAUOSYCtYC7a5OORa</value>
            </field>
        </x>
    </command>
</iq>
```

### Android

```xml
<iq from="user@example.com/mobile" id="x20" to="push.example.com" type="set">
    <command xmlns="http://jabber.org/protocol/commands" action="execute" node="register-push-fcm">
        <x xmlns="jabber:x:data" type="submit">
          <field var="token">
            <value>dPrh685pTdGns_MHsu1I-b:APA91bGwfwGthGPxE2aUJ5o-pyn1eMzV0WPqFulpyYo20xOEy7efh8soyJpcCuibleBGjCaRDRgjl6vSYNwDDE7pq0lfKOzubfUvCrvKvLMN4uRLEY373L11sCKqHeOf-_Qn3eooeOge</value>
          </field>
          <field var="device-id">
            <value>0ab43fb78f92ba10</value>
          </field>
        </x>
    </command>
</iq>
```

```xml
<iq from="push.example.com" id="x20" to="user@example.com/mobile" type="result">
    <command xmlns="http://jabber.org/protocol/commands" action="complete" node="register-push-fcm">
        <x xmlns="jabber:x:data" type="form">
            <field type="text-single" var="node">
                <value>KmDtdKoUTiGr</value>
            </field>
            <field type="text-single" var="secret">
                <value>FBfIl4qflMTDKn6CYPxXJHyH</value>
            </field>
        </x>
    </command>
</iq>
```

### Enabling Notifications

* After registering with the push server, Client sends the node ID and the jid of the app server (push.example.com) to the user's server.

```xml
<iq type='set' id='x42'>
  <enable xmlns='urn:xmpp:push:0' jid='push.example.com' node='KmDtdKoUTiGr'>
    <x xmlns='jabber:x:data' type='submit'>
      <field var='FORM_TYPE'><value>http://jabber.org/protocol/pubsub#publish-options</value></field>
      <field var='secret'><value>FBfIl4qflMTDKn6CYPxXJHyH</value></field>
    </x>
  </enable>
</iq>
```

Note: To support sandbox devices in iOS, you should provide the `<field var="sandbox"><value>true</value></field>` in publish options.

Example: 
```xml
<iq type='set' id='x42'>
  <enable xmlns='urn:xmpp:push:0' jid='push.example.com' node='37Ni514izxHG'>
    <x xmlns='jabber:x:data' type='submit'>
      <field var='FORM_TYPE'><value>http://jabber.org/protocol/pubsub#publish-options</value></field>
      <field var='secret'><value>Zt9z9wOtAUOSYCtYC7a5OORa</value></field>
      <field var="sandbox"><value>true</value></field>
    </x>
  </enable>
</iq>
```

More info can be found in [XEP-0357 Section 5](https://xmpp.org/extensions/xep-0357.html#enabling)

### Disabling Notifications

```xml
<iq type='set' id='x97'>
  <disable xmlns='urn:xmpp:push:0' jid='push.example.com' node='37Ni514izxHG' />
</iq>
```

More info can be found in [XEP-0357 Section 6](https://xmpp.org/extensions/xep-0357.html#disabling)

### Publishing Notifications

It can be found in [XEP-0357 Section 7](https://xmpp.org/extensions/xep-0357.html#publishing).
