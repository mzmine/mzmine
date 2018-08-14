# GSOC 2018 MS/MS identification module for MZmine
This document represents a final report for the GSOC 2018 project. It briefly describes what have I achieved during Summer of Code.
Link to the [project page](https://summerofcode.withgoogle.com/projects/#4793376075939840).
## Steps of this project
The main goal of this project was to add an MS/MS identification module to MZmine2 application.
The goal consisted of several big steps:
1. Wrapping the functionality of [Sirius library](https://github.com/boecker-lab/sirius) into MSDK-library. 
   1. Wrap the predictor of chemical formula using provided Sirius-API
   2. Implement an approach to access CSI:FingerId Web API on Böcker lab server.
2. Creation of a new GUI module in MZmine application.
    1. Create and configure a new functioning user-friendly table
    2. Construct preview images of identified compounds
    3. Add functionality to look up for possible chemical data bases per compound processed by FingerId.

## What was achieved
- It was not a part of my project, but I also implemented a module of [MGF File Importing](https://github.com/msdk/msdk/pull/256) for MSDK-library.
- Fully functioning Sirius identification module for MSDK-library. [link](https://github.com/msdk/msdk/pull/255).
- Integration of Sirius functionality into MZmine (currently not merged). [link](https://github.com/mzmine/mzmine2/pull/439)

## How to use it
You cannot just test MSDK library, only if it is integrated into another module (like MZmine2). That is why there will be no explanation for MGF-Import module. 
This instruction will describe how to use new MZmine Identification module called Sirius.
1. Fetch code from [pull-request](https://github.com/mzmine/mzmine2/pull/439)
2. Download an mzmine [sample](https://www.dropbox.com/s/j2enjua3lobkeo1/sample-2.3.mzmine?dl=0).
3. Build a project using `mvn package` or run MZmineCore class.
- Generated application will be stored in `target/` folder, extract a zip file and open suitable for your _OS_ script.
  - `startMZmine_Linux.sh`
  - `startMZmine_MacOSX.command`
  - `startMZmine_Windows.bat`
- Generated application is configured to show `Help pages`. You can call it by clicking appropriate button `Help`.
- If questions related to GUI appears, please get familiar with manual files, it can be found [here](http://mzmine.github.io/documentation.html).
4. When you started an application, open a project (Ctrl + O) and select downloaded file. After some time all the peak lists will be loaded.
5. Sirius module is located in `Peak list methods -> Identification -> SIRIUS Structure prediction`. Use it in order to process a Peak List.
Do not forget to fulfill parameters.
- In order to process a _Single Peak List row_:
  1. Open one of the peak lists on the right side of the main menu
  2. Click with the right button of your mouse on one of the rows of appeared window.
  3. Select `Search -> SIRIUS Structure prediction` and fulfill the parameters.
6. Also I suggest you to take a look at a `Help page`.


## What remains to be done
- Merge pull-request to MZmine2.
- Major bug: Reimplement the FingerId Web method in order to process larger data sets. Right now it has a limitation of maximum size of POST request.
- Minor bug: Fix the content of multilined cell after sorting. The origin of a bug I suppose is hidden somewhere internally, probably need to get rid of reference type.
- Check the stability of an application of Linux machine.

## What problems I met
- Problem with dynamic loading of native libraries from .jar files.
- Problem with maven dependencies in inherited _pom_ files. (Exclusion of inherited dependency without altering the parent pom)

### Important links
- Link to the [MSDK-library](https://github.com/msdk/msdk).
- Link to the [MZmine2](https://github.com/mzmine/mzmine2).
- Link to [my fork](https://github.com/evgerher/mzmine2) of MZmine2.
- Link to [my fork](https://github.com/evgerher/msdk) of MSDK.
