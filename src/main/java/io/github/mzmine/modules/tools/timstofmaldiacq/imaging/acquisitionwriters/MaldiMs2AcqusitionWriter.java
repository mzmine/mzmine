package io.github.mzmine.modules.tools.timstofmaldiacq.imaging.acquisitionwriters;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.ImagingSpot;
import io.github.mzmine.parameters.ParameterSet;
import java.io.File;
import java.util.List;
import java.util.function.BooleanSupplier;

public interface MaldiMs2AcqusitionWriter extends MZmineModule {

  boolean writeAcqusitionFile(File acquisitionFile, ParameterSet parameters,
      List<ImagingSpot> spots, File savePathDir, BooleanSupplier isCanceled);
}
