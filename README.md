<p align="center">
    <img src="./app/src/main/res/mipmap-xxhdpi/ic_launcher.webp" alt="gamecounter logo" />
</p>

An app for counting points at board, card, or role playing games:
 - keep track of multiple counters per player
 - customizable counter name and initial value
 - long press plus or minus for quick updates
 - players can change card colors
 - roll dices of any size, or pick player order

# Screenshots

![](metadata/en-US/images/featureGraphic.png)

# Build

```sh
# build a signed debug package
./gradlew assembleDebug

# sign the release binary with the debug key
./gradlew assembleRelease -PdebugSignRelease=true
```

# Install
```sh
adb install ./app/build/outputs/apk/release/app-release.apk
```