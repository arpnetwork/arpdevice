# arpdevice

An Android app act as a container to install and run third-party android apps and streaming its screen content to arpclient which connecting with. This app also register current device to the cloud so that the cloud can pick one desired device to run this app once the arpclient request.

## Pre-requisites

* Android SDK v22
* Android Studio v3.0

## Features

*  install and run third-party android apps.
*  streaming the third-party android apps contents to client.
*  register user device to cloud.

## Building
Clone this project from github.

```
git clone https://github.com/arpnetwork/arpdevice.git

```
download git submodule.

```
git submodule init
git submodule update

```

## Running
1,connect your android phone to computer with USB.

2,push `arptouch` binary to /data/local/tmp by adb.

```
adb push /arptouch /data/local/tmp
```

3,make your adb run in tcpip mode with 5555 port.
```
adb tcpip 5555
```

4,run this app and wait clients to connect.
