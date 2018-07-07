/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.id.sirius.ConstraintsGenerator;
import io.github.msdk.id.sirius.FingerIdWebMethod;
import io.github.msdk.id.sirius.SiriusIdentificationMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.msdk.util.IonTypeUtil;
import java.util.LinkedList;
import java.util.List;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.taskcontrol.TaskPriority;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodsExecution {
  private static final Logger logger = LoggerFactory.getLogger(MethodsExecution.class);


  public static SiriusIdentificationMethod generateSiriusMethod(List<MsSpectrum> ms1list, List<MsSpectrum> ms2list, MolecularFormulaRange range, Double ppm, IonizationType ionType, Double parentMass, Integer candidates) throws MSDKException {
    ConstraintsGenerator generator = new ConstraintsGenerator();
    FormulaConstraints constraints = generator.generateConstraint(range);
    IonType siriusIon = IonTypeUtil.createIonType(ionType.toString());

    SiriusIdentificationMethod siriusMethod = new SiriusIdentificationMethod(
        ms1list,
        ms2list,
        parentMass,
        siriusIon,
        candidates,
        constraints,
        ppm
    );

    return siriusMethod;
  }

  //TODO: update code and make it possible to use in PeakListIdentificationTask
  public static List<FingerIdWebMethodTask> generateFingerIdWebMethods(List<IonAnnotation> ions, Ms2Experiment experiment, Integer candidatesAmount, ResultWindow window) {
    List<FingerIdWebMethodTask> tasks = new LinkedList<>();
    try {
      for (IonAnnotation ia : ions) {
        SiriusIonAnnotation annotation = (SiriusIonAnnotation) ia;
        FingerIdWebMethodTask task = new FingerIdWebMethodTask(annotation, experiment,
            candidatesAmount, window);
        tasks.add(task);
        MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
      }
      Thread.sleep(1000);
    } catch (InterruptedException interrupt) {
      logger.error("Processing of FingerWebMethods were interrupted");
      interrupt.printStackTrace();
    }

    return tasks;
  }
}
