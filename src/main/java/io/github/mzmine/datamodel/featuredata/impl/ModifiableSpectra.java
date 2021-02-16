package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.MassSpectrum;
import java.util.List;

interface ModifiableSpectra<T extends MassSpectrum> {

  List<T> getSpectraModifiable();
}
