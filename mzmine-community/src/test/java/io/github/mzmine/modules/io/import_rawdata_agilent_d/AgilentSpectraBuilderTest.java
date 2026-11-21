package io.github.mzmine.modules.io.import_rawdata_agilent_d;

import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.auto.AutoMassDetectorParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.impl.RawDataFileImpl;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgilentSpectraBuilderTest {

  /**
   * Mass detection on import must receive the profile type of the spectrum as read: the auto mass
   * detector then finds one peak instead of keeping every profile point as a centroid.
   */
  @Test
  void massDetectionOnImportCentroidsProfileScans() {
    final ParameterSet auto = MassDetectors.AUTO.getModuleParameters().cloneParameterSet();
    auto.setParameter(AutoMassDetectorParameters.noiseLevel, 0d);
    final ScanImportProcessorConfig processor = AllSpectralDataImportModule.createSpectralProcessors(
        AdvancedSpectraImportParameters.create(MassDetectors.AUTO, auto, MassDetectors.AUTO, auto,
            null, ScanSelection.ALL_SCANS, false));
    final AgilentSpectraBuilder builder = new AgilentSpectraBuilder("test.d", processor);
    final RawDataFileImpl file = new RawDataFileImpl("test.d", null, null, Color.BLACK);

    // one zero-bounded profile peak around m/z 500.0
    final double[] mzs = {499.97, 499.98, 499.99, 500.00, 500.01, 500.02, 500.03};
    final double[] intensities = {0, 100, 500, 1000, 500, 100, 0};
    final SimpleScan scan = builder.buildScan(file, 1, 1, 1f, PolarityType.POSITIVE, 0, null, mzs,
        intensities, false);

    Assertions.assertNotNull(scan);
    Assertions.assertEquals(MassSpectrumType.CENTROIDED, scan.getSpectrumType());
    Assertions.assertEquals(1, scan.getNumberOfDataPoints(), "one centroid for one profile peak");
    Assertions.assertEquals(500.0, scan.getMzValue(0), 1e-3);
    // the scanning range is the range of the spectrum as read
    Assertions.assertEquals(499.97, scan.getScanningMZRange().lowerEndpoint(), 1e-9);
    Assertions.assertEquals(500.03, scan.getScanningMZRange().upperEndpoint(), 1e-9);
  }
}
