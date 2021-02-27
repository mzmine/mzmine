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

  /*public static final IonMobilitySeries EMPTY_MOBILOGRAM = new IonMobilitySeries() {
    private final DoubleBuffer emptyBuffer = DoubleBuffer.wrap(new double[0]);

    @Override
    public List<MobilityScan> getSpectra() {
      return Collections.emptyList();
    }

    @Override
    public double getIntensityForSpectrum(MobilityScan spectrum) {
      return 0;
    }

    @Override
    public IonSpectrumSeries<MobilityScan> subSeries(@Nullable MemoryMapStorage storage,
        @Nonnull List<MobilityScan> subset) {
      return this;
    }

    @Override
    public IonSpectrumSeries<MobilityScan> copyAndReplace(@Nullable MemoryMapStorage storage,
        @Nonnull double[] newMzValues, @Nonnull double[] newIntensityValues) {
      return this;
    }

    @Override
    public IonSeries copy(MemoryMapStorage storage) {
      return this;
    }

    @Override
    public DoubleBuffer getIntensityValues() {
      return emptyBuffer;
    }

    @Override
    public DoubleBuffer getMZValues() {
      return emptyBuffer;
    }
  };*/
}
