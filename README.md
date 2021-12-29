# NosoWallet-Android
NosoWallet for android, based on: https://github.com/Noso-Project/NosoWallet

## Running NOSOmobile

You just need to download the latest version of the wallet in the .APK format for direct installation on any compatible device (Android 6+)

## Building from source

To build from source you'll need a working installation of [Android Studio](https://developer.android.com/studio) and the following library:

- BouncyCastle (Cryptography Lib)

This library is already included in the build source under app/libs, however to ensure that you have the latest version you can get it from [BouncyCastle Official Website](https://www.bouncycastle.org/latest_releases.html) in .jar format

The way to add the library to the project is by adding the implementation line in build.gradle(app Module), it has to be under "app/libs" folder
![image](https://user-images.githubusercontent.com/53009062/147697515-8ce4be92-7dcf-4d24-9732-f31beb8fde43.png)


