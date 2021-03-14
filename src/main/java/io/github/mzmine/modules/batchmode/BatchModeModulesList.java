/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ADAPpeakpicking.AdapResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_gridmass.GridMassModule;
import io.github.mzmine.modules.dataprocessing.featdet_ionmobilitytracebuilder.IonMobilityTraceBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_masscalibration.MassCalibrationModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerModule;
import io.github.mzmine.modules.dataprocessing.featdet_shoulderpeaksfilter.ShoulderPeaksFilterModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.FeatureFilterModule;
import io.github.mzmine.modules.dataprocessing.filter_isotopegrouper.IsotopeGrouperModule;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterModule;
import io.github.mzmine.modules.dataprocessing.id_camera.CameraSearchModule;
import io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSCalcModule;
import io.github.mzmine.modules.dataprocessing.id_cliquems.CliqueMSModule;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSResultsImportModule;
import io.github.mzmine.modules.dataprocessing.id_sirius.SiriusIdentificationModule;
import io.github.mzmine.modules.io.export_gnps.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.modules.io.export_mztabm.MZTabmExportModule;
import io.github.mzmine.modules.io.export_sirius.SiriusExportModule;
import io.github.mzmine.modules.io.import_bruker_tdf.TDFImportModule;
import io.github.mzmine.modules.io.import_mzml_jmzml.MzMLImportModule;
import io.github.mzmine.modules.io.import_mztabm.MZTabmImportModule;
import io.github.mzmine.modules.io.projectload.ProjectLoadModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveAsModule;
import io.github.mzmine.modules.io.projectsave.ProjectSaveModule;
import java.util.Collections;
import java.util.List;

public class BatchModeModulesList {

  public static final List<Class<? extends MZmineProcessingModule>> MODULES = Collections
      .unmodifiableList(List.of(

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#PROJECT}
           */
          ProjectLoadModule.class, //
          ProjectSaveModule.class, //
          ProjectSaveAsModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#SPECTRAL_DATA}
           */
          TDFImportModule.class, //
          MzMLImportModule.class, //
          MassDetectionModule.class, //
          MassCalibrationModule.class, //
          MobilityScanMergerModule.class, //
          ShoulderPeaksFilterModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_DETECTION}
           */
          ModularADAPChromatogramBuilderModule.class, //
          IonMobilityTraceBuilderModule.class, //
          GridMassModule.class, //
          SmoothingModule.class, //
          MinimumSearchFeatureResolverModule.class, //
          AdapResolverModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_FILTERING}
           */
          FeatureFilterModule.class,
          RowsFilterModule.class,
          IsotopeGrouperModule.class,

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_PROCESSING}
           */

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_ANNOTATION}
           */
          CCSCalcModule.class, //
          CameraSearchModule.class, //
          SiriusIdentificationModule.class, //
          SiriusExportModule.class, //
          CliqueMSModule.class, //

          /**
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#FEATURE_IO}
           */
          GnpsFbmnExportAndSubmitModule.class, //
          GNPSResultsImportModule.class, //
          MZTabmExportModule.class, //
          MZTabmImportModule.class //

          /**
           * needed in batch mode?
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#VISUALIZATION}
           */

          /**
           * needed in batch mode?
           * {@link io.github.mzmine.modules.MZmineModuleCategory.MainCategory#OTHER}
           */

      ));

  private BatchModeModulesList() {
  }
}
