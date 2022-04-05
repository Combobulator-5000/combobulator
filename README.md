# combobulator
Combobulator is an augmented reality (AR) phone app that helps people keep their workspace organized.
This repository contains our working prototype, which we created on behalf of our sponsors at the
[UBC Engineering Physics Project Lab](https://projectlab.engphys.ubc.ca/) as part of a capstone project.

It currently only supports Android devices, because there is not a mature cross-platform AR framework
available.

# Installation
- Install [Android Studio](https://developer.android.com/studio)
- Clone the repo to your machine
  - Via the command line: `git clone https://github.com/Combobulator-5000/combobulator`
  - Via Android Studio: `File > New > Project from Version Control...`
- Open the project in Android Studio
- Press the Gradle Sync button near the top right of the IDE.

# Setting up the Emulator
If you want to deploy this app on your phone, [enable developer options and USB debugging](https://developer.android.com/studio/debug/dev-options).
When you run the app via USB debugging, your phone will automatically install ARCore, which is a dependency of the project.

To run the app in an emulator, you must [install the ARCore SDK on your emulated device](https://developers.google.com/ar/develop/c/emulator).

# Contributors
- [Davis Johnson](https://github.com/johnsonadavis)
- [Emily Love](https://github.com/emgineer)
- [Sean Lan](https://github.com/Xenans)
- [Seth Hinz](https://github.com/shinzlet)
