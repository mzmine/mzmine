![logo300_mzmine.png](mzmine-community%2Flogo%2Flogo300_mzmine.png)

![GitHub all releases](https://img.shields.io/github/downloads/mzmine/mzmine/total)
![GitHub all releases](https://img.shields.io/github/downloads/mzmine/mzmine/latest/total)
![GitHub contributors](https://img.shields.io/github/contributors/mzmine/mzmine)
[![Development Build Release](https://github.com/mzmine/mzmine/actions/workflows/dev_build_release.yml/badge.svg)](https://github.com/mzmine/mzmine/actions/workflows/dev_build_release.yml)
![Static Badge](https://img.shields.io/badge/JDK%20version-23-blue)
![Static Badge](https://img.shields.io/badge/JavaFX%20version-23-%2391219c)

mzmine is an open-source software for mass-spectrometry data processing. The goals of the project is
to provide a user-friendly, flexible and easily extendable software with a complete set of modules
covering the entire MS data analysis workflow.

More information about the software can be found on the [mzmine](http://mzmine.github.io) website.

Getting started with the [Documentation](https://mzmine.github.io/mzmine_documentation/index.html) and our [YouTube channel](https://www.youtube.com/@mzmineproject/playlists?view=1&sort=lad&flow=grid)

## License

mzmine source codes are distributed under the [MIT license](LICENSE.txt).


## Releases

[Releases](https://github.com/mzmine/mzmine3/releases?q=&expanded=true) are split into [stable releases](https://github.com/mzmine/mzmine3/releases/latest)
and
the [latest development build](https://github.com/mzmine/mzmine3/releases/tag/Development-release)
which reflects the current state of the master branch and is meant for testing purposes. Download
options include portable versions and installers for the Window, macOS, and Linux.

## Installation

mzmine should work on Windows, macOS, and Linux using either the installers or the portable versions. There are **NO** further requirements as mzmine packages a specific Java Virtual Machine. This means the local Java installation has **no** impact on mzmine. Windows users might be warned that mzmine is not signed or from a trusted source and have to click run anyways. 

Before creating your first project, we recommend to [set the preferences](#set-user-preferences).

### Installation on Linux

Download the latest version, install mzmine, login, and run mzmine. See mzmine [command-line interface](https://mzmine.github.io/mzmine_documentation/commandline_tool.html) as a reference.  
```bash
# with gh (github) installed, download of latest .deb installer is quite easy
# gh auth login
# sudo apt install gh
# gh release download --repo mzmine/mzmine --pattern "mzmine*.deb"

# or find installer at https://github.com/mzmine/mzmine/releases/latest 
wget https://github.com/mzmine/mzmine/releases/download/text-action-release/mzmine_4.3.1_amd64.deb

# create required dir and install mzmine
sudo mkdir -p /usr/share/desktop-directories/
sudo apt install mzmine*.deb

# potential dependencies that may be required 
# sudo apt-get install xdg-utils
# sudo apt-get install libgl1
# sudo apt-get install libgtk-3-0
# sudo apt-get install libxtst6

# run mzmine and print help. also check -login-console -batch
/opt/mzmine/bin/mzmine -help
```

## Development

### Tutorial

Please read our brief [tutorial](http://mzmine.github.io/development.html) on how to contribute new
code to mzmine.

### Java version

mzmine development requires Java Development Kit (JDK) version 21 or newer (http://jdk.java.net).


### Building

To build the mzmine package from the sources, run the following command:

    ./gradlew

or

    gradlew.bat

The final mzmine distribution will be placed in build/jpackage

If you encounter any problems, please contact the developers by posting an issue:
https://github.com/mzmine/mzmine3/issues

### Contribute code (or documentation)
Guides and more information is in the mzmine documentation:
https://mzmine.github.io/mzmine_documentation/contribute_intellij.html
