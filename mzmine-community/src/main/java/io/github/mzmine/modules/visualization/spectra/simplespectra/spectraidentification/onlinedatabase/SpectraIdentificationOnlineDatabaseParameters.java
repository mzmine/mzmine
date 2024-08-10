/*
 * Copyright (c) 2004-2024 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.onlinedatabase;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * Parameters for identifying peaks by searching on-line databases.
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 * @deprecated because of old API usage. Hard to maintain. This was removed from the interfaces and
 * is only here as reference point
 */
@Deprecated
public class SpectraIdentificationOnlineDatabaseParameters extends SimpleParameterSet {

  public static final ModuleOptionsEnumComboParameter<OnlineDatabases> database = new ModuleOptionsEnumComboParameter<>(
      "Database", "Database to search", OnlineDatabases.PubChem);

  public static final ComboParameter<IonizationType> ionizationType = new ComboParameter<IonizationType>(
      "Ionization type", "Ionization type", IonizationType.values());

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final DoubleParameter noiseLevel = new DoubleParameter("Noise level",
      "Set a noise level");

  public SpectraIdentificationOnlineDatabaseParameters() {
    super(database, ionizationType, mzTolerance, noiseLevel);
  }

}
