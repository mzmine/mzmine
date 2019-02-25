/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import java.util.List;
import javax.annotation.Nonnull;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingQueue;

/**
 * Combines the processing queues information and the corresponding plot.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class PlotModuleCombo {
  SpectraPlot plot;
  DataPointProcessingQueue steps;

  public PlotModuleCombo(DataPointProcessingQueue steps, SpectraPlot plot) {
    setPlot(plot);
    setSteps(steps);
  }

  public PlotModuleCombo() {
    steps = new DataPointProcessingQueue();
  }

  public boolean plotValid() {
    return (plot != null);
  }

  public SpectraPlot getPlot() {
    if (!plotValid())
      throw new MSDKRuntimeException("getPlot() called, although plot == null");
    return plot;
  }

  @Nonnull
  public List<MZmineProcessingStep<DataPointProcessingModule>> getsteps() {
    return steps;
  }

  public void setPlot(SpectraPlot plot) {
    this.plot = plot;
  }

  public void setSteps(DataPointProcessingQueue steps) {
    this.steps = steps;
  }

//  public Class<? extends ParameterSet> getModuleParameterSetClass(MZmineProcessingStep<DataPointProcessingModule> module){
//    return parameters.get(steps.indexOf(module));
//  }

  /**
   * 
   * @return Returns true if the module list is initialized and > 0.
   */
  public boolean stepsValid() {
    if(steps != null && steps.size() > 0)
      return true;
    return false;
  }

  public boolean addStep(MZmineProcessingStep<DataPointProcessingModule> module) {
    return this.steps.add(module);
  }

  public boolean removeTask(MZmineProcessingStep<DataPointProcessingModule> module) {
    return this.steps.remove(module);
  }

  /**
   * 
   * @param current A pointer to the current module. 
   * @return Returns true if there is one or more steps, false if not.
   */
  public boolean hasNextStep(MZmineProcessingStep<DataPointProcessingModule> current) {
    if (steps.contains(current)) {
      int index = steps.indexOf(current);
      if (index + 1 < steps.size()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param current A pointer to the current module.
   * @return Returns the next module in this PlotModuleCombo. If this pmc has no next module the
   *         return is null. Use hasNextModule to check beforehand.
   */
  public MZmineProcessingStep<DataPointProcessingModule> getNextStep(MZmineProcessingStep<DataPointProcessingModule> current) {
    if (hasNextStep(current))
      return steps.get(steps.indexOf(current) + 1);
    return null;
  }

  /**
   * 
   * @return Returns the first module in this PlotModuleCombo. If the list of steps is not
   *         initialised, the return is null.
   */
  public MZmineProcessingStep<DataPointProcessingModule> getFirstStep() {
    if (steps.size() > 0) {
      return steps.get(0);
    }
    return null;
  }
  
}
