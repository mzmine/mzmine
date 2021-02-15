package io.github.mzmine.datamodel.featuredata;

import io.github.mzmine.datamodel.MobilityScan;

/**
 * Tag interface for mobilograms.
 */
public interface IonMobilitySeries extends IonSpectrumSeries<MobilityScan>, MobilitySeries {

  @Override
  default double getMobility(int index) {
    return getSpectrum(index).getMobility();
  }
}
