## Mass calibration module


#### Short description

The purpose of the masscalibration module is to provide calibration to detected mass peaks. This can be done in multiple ways. Currently a list of PPM errors of mass peaks is obtained, then a systematic error is obtained based on the distribution of these errors and then the mass peaks are shifted to account against that bias estimate. The PPM errors are obtained by comparing the detected mass peaks against a specified list of standards compounds expected to appear in the dataset, also called a list of standards calibrants. The detected mass peaks are matched with appropriate compounds and the difference in their mz ratio is used to obtain the distribution of measurement errors.

`standardslist` package contains code needed for managing and reading lists of standard calibrants, currently spreadsheet files are supported,
check code and its documentation for details, including the format of the files

Check the [module help file](./help/help.html) for more details

Many potential updates could enhance mass calibration module, few of the very concrete are listed below:

- Performance speedup of mass peak matching with more efficient range searching, this can be done with a k-d tree for instance
- Performance speedup of the KNN trend, this can be done with precomputation of edge/critical points where the neighbors change, so that all the points within the consecutive intervals between these endpoints have the same KNN regression output (at least for the arithmetic mean prediction)
- Module codebase refactor, especially handling of universal calibrants vs standards calibrants could be more elegant, currently universal calibrants are made into standard calibrants by using retention time of -1 as a flag and using large retention time tolerance when the matching is set up. Many possible alternative ways to implement it, retention time tolerance could be overridden for instance to return null tolerance ranges which are ignored in standards list, all retention times get included then. Using nicely typed objects around the module codebase more could be an elegant refactor, when the module was in experimental stages and evolving quickly, we did not impose much OOP structure on the codebase to keep stuff flexible and able to change fast.
- Adding reporting capabilities to mzmine modules to display post run statistics and keep a historical queue of all tasks run with their post run reports. Even if the reports were just POJOs serialized into JSON, this would already provide stronger reporting capabilities than just plain log files and message boxes. Use case for mass calibration module, when running many samples in batches, it would be useful to check how many matches were made, errors extracted, see the global bias estimate, the variance of the trend used and so on. Module performance measure could be displayed too, even if based on such simple stats, it could be valuable. User could then check after processing many samples in batches, whether there was any suspicious change in the stats for different samples and recalibrate them separately.


This module was created during and is part of a GSoC 2020 project with MZmine, done by Łukasz Fiszer and MZmine team.

GSoC project link: https://summerofcode.withgoogle.com/projects/#6529966893694976

GSoC review link: https://github.com/lukasz-fiszer/mzmine3/tree/gsoc2020-review
