![logo300_mzmine.png](mzmine-community%2Flogo%2Flogo300_mzmine.png)

![GitHub all releases](https://img.shields.io/github/downloads/mzmine/mzmine3/total)
![GitHub all releases](https://img.shields.io/github/downloads/mzmine/mzmine3/latest/total)
![GitHub contributors](https://img.shields.io/github/contributors/mzmine/mzmine3)
[![Development Build Release](https://github.com/mzmine/mzmine3/actions/workflows/dev_build_release.yml/badge.svg)](https://github.com/mzmine/mzmine3/actions/workflows/dev_build_release.yml)
![Static Badge](https://img.shields.io/badge/JDK%20version-21-blue)
![Static Badge](https://img.shields.io/badge/JavaFX%20version-21-%2391219c)

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

## Development

### Tutorial

Please read our brief [tutorial](http://mzmine.github.io/development.html) on how to contribute new
code to mzmine.

### Java version

mzmine development requires Java Development Kit (JDK) version 21 or newer (http://jdk.java.net).

### Moving old codebase to new modular build system

If there are still changes pending apply those moves first and then create a PR. Create a backup branch.

```bash
git mv -k -v ./src ./mzmine-community/
git mv -v ./log ./mzmine-community/    
git mv -v ./logo ./mzmine-community/    
git mv -v ./Ontologies ./mzmine-community/    
```



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
