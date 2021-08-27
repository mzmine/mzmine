/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.import_rawdata_all;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.localmaxima.LocalMaxMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.recursive.RecursiveMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.wavelet.WaveletMassDetector;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class MassDetectionSubParameters extends SimpleParameterSet {

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  public static final MassDetector massDetectors[] = {new CentroidMassDetector(),
      new ExactMassDetector(), new LocalMaxMassDetector(), new RecursiveMassDetector(),
      new WaveletMassDetector()};

  public static final ModuleComboParameter<MassDetector> massDetector = new ModuleComboParameter<MassDetector>(
      "Mass detector", "Algorithm to use for mass detection and its parameters", massDetectors,
      massDetectors[0]);

  public MassDetectionSubParameters() {
    super(new Parameter[]{massDetector});
  }

  @NotNull
  @Override
  public IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
