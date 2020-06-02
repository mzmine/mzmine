## Mass calibration module

This module was created during and is part of a GSoC 2020 project with MZmine, done by Łukasz Fiszer and MZmine team.

#### Short description

The purpose of the masscalibration module is to provide calibration to detected mass peaks. This can be done in multiple ways. Currently a list of PPM errors of mass peaks is obtained, then a systematic error is obtained based on the distribution of these errors and then the mass peaks are shifted to account against that bias estimate. The PPM errors are obtained by comparing the detected mass peaks against a specified list of standards compounds expected to appear in the dataset, also called a list of standards calibrants. The detected mass peaks are matched with appropriate compounds and the difference in their mz ratio is used to obtain the distribution of measurement errors.

`standardslist` package contains code needed for managing and reading lists of standard calibrants, currently spreadsheet files are supported,
check code and its documentation for details, including the format of the files

