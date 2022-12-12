![MZmine 3](logo/MZmine_logo_RGB.png)

![Build Status](https://github.com/mzmine/mzmine3/actions/workflows/gradle.yml/badge.svg?event=push)

MZmine is an open-source software for mass-spectrometry data processing. The goals of the project is
to provide a user-friendly, flexible and easily extendable software with a complete set of modules
covering the entire MS data analysis workflow.

More information about the software can be found on the [MZmine](http://mzmine.github.io) website.

Getting started with the [Documentation](https://mzmine.github.io/mzmine_documentation/index.html)

## License

MZmine is a free software; you can redistribute it and/or modify it under the terms of the MIT license.

MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

## Releases

[Releases](https://github.com/mzmine/mzmine3/releases?q=&expanded=true) are split into [stable releases](https://github.com/mzmine/mzmine3/releases/latest)
and
the [latest development build](https://github.com/mzmine/mzmine3/releases/tag/Development-release)
which reflects the current state of the master branch and is meant for testing purposes. Download
options include portable versions and installers for the Window, macOS, and Linux.

### Running on macOS

Currently, MZmine 3 lacks a signature for macOS. While we are working on this, user can allow MZmine
in the macOS Gatekeeper protection by running the following command in the terminal from the folder
containing the .app.

```
sudo xattr -cr MZmine.app
```

Find a step-by-step guide in the [documentations](https://mzmine.github.io/mzmine_documentation/getting_started.html#on-macos).


## Development

### Tutorial

Please read our brief [tutorial](http://mzmine.github.io/development.html) on how to contribute new
code to MZmine.

### Java version

MZmine development requires Java Development Kit (JDK) version 16 or newer (http://jdk.java.net).

### Building

To build the MZmine package from the sources, run the following command:

    ./gradlew

or

    gradlew.bat

The final MZmine distribution will be placed in build/jpackage

If you encounter any problems, please contact the developers:
https://github.com/mzmine/mzmine3/issues

### Code style

Since this is a collaborative project, please adhere to the following code formatting conventions:

* We use the Google Java Style Guide (https://github.com/google/styleguide)
* Please write JavaDoc comments as full sentences, starting with a capital letter and ending with a
  period. Brevity is preferred (e.g., "Calculates standard deviation" is preferred over "This method
  calculates and returns a standard deviation of given set of numbers").

