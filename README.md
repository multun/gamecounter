<p align="center">
    <img src="./app/src/main/res/mipmap-xxhdpi/ic_launcher.webp" alt="gamecounter logo" />
</p>

An app for counting points at board, card, or role playing games:
 - keep track of multiple counters per player
 - customizable counter name and initial value
 - long press plus or minus for quick updates
 - players can change card colors
 - roll dices of any size, or pick player order

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/net.multun.gamecounter.fdroid/)

# Preview

<p align="center">
    <img src="fastlane/metadata/en-US/images/phoneScreenshots/board_dark.png" alt="board screenshot" />
</p>

# Privacy policy

- No personal or device information is collected
- No permissions are required. Most notably, the application does not require access to the internet.

# Build

```sh
# build a signed debug package
./gradlew assembleDevRelease # or assembleDevDebug
```

# Install

```sh
adb install ./app/build/outputs/apk/dev/release/app-dev-release.apk
```
