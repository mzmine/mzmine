/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate;

import java.text.MessageFormat;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.FeatureList;
import net.sf.mzmine.modules.featurelistmethods.grouping.metacorrelate.correlation.FeatureShapeCorrelationParameters;
import net.sf.mzmine.modules.featurelistmethods.grouping.metacorrelate.correlation.InterSampleHeightCorrParameters;
import net.sf.mzmine.modules.featurelistmethods.grouping.metacorrelate.corrgrouping.CorrelateGroupingParameters;
import net.sf.mzmine.modules.featurelistmethods.grouping.metacorrelate.datastructure.CorrelationData.SimilarityMeasure;
import net.sf.mzmine.modules.featurelistmethods.grouping.metacorrelate.minfeaturefilter.MinimumFeaturesFilterParameters;
import net.sf.mzmine.modules.featurelistmethods.grouping.metacorrelate.msms.similarity.MS2SimilarityParameters;
import net.sf.mzmine.modules.featurelistmethods.identification.ionidentity.ionidnetworking.IonNetworkLibrary;
import net.sf.mzmine.modules.featurelistmethods.identification.ionidentity.ionidnetworking.IonNetworkingParameters;
import net.sf.mzmine.modules.featurelistmethods.identification.ionidentity.refinement.IonNetworkRefinementParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class SimpleMetaCorrelateTask extends MetaCorrelateTask {

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public SimpleMetaCorrelateTask(final MZmineProject project, final ParameterSet parameterSet,
      final FeatureList featureList) {
    super();
    this.project = project;
    this.featureList = featureList;
    parameters = parameterSet;

    String massListMS2 =
        parameterSet.getParameter(SimpleMetaCorrelateParameters.MS2_MASSLISTS).getValue();
    // sample groups parameter
    useGroups = parameters.getParameter(SimpleMetaCorrelateParameters.GROUPSPARAMETER).getValue();
    groupingParameter =
        (String) parameters.getParameter(SimpleMetaCorrelateParameters.GROUPSPARAMETER)
            .getEmbeddedParameter().getValue();

    // height and noise
    noiseLevelCorr = parameters.getParameter(SimpleMetaCorrelateParameters.NOISE_LEVEL).getValue();
    minHeight = parameters.getParameter(SimpleMetaCorrelateParameters.MIN_HEIGHT).getValue();

    // by min percentage of samples in a sample set that contain this feature MIN_SAMPLES
    minFeatureFilter = (MinimumFeaturesFilterParameters) parameterSet
        .getParameter(SimpleMetaCorrelateParameters.MIN_SAMPLES_FILTER).getEmbeddedParameters();
    minFFilter = minFeatureFilter.createFilterWithGroups(project, featureList.getRawDataFiles(),
        groupingParameter, minHeight);

    // tolerances
    rtTolerance = parameterSet.getParameter(SimpleMetaCorrelateParameters.RT_TOLERANCE).getValue();

    // FEATURE SHAPE CORRELATION
    groupByFShapeCorr =
        parameterSet.getParameter(SimpleMetaCorrelateParameters.MIN_FSHAPE_CORR).getValue();
    minShapeCorrR = parameterSet.getParameter(SimpleMetaCorrelateParameters.MIN_FSHAPE_CORR)
        .getEmbeddedParameter().getValue();
    shapeSimMeasure = SimilarityMeasure.PEARSON;
    minCorrelatedDataPoints = 5;
    minCorrDPOnFeatureEdge = 2;
    // total corr
    useTotalShapeCorrFilter = true;
    minTotalShapeCorrR = 0.5;

    // ADDUCTS
    MZTolerance mzTolerance =
        parameterSet.getParameter(SimpleMetaCorrelateParameters.MZ_TOLERANCE).getValue();
    MZTolerance mzTolMS2 =
        parameterSet.getParameter(SimpleMetaCorrelateParameters.MZ_TOLERANCE_MS2).getValue();

    searchAdducts =
        parameterSet.getParameter(SimpleMetaCorrelateParameters.ADDUCT_LIBRARY).getValue();
    annotationParameters = parameterSet.getParameter(SimpleMetaCorrelateParameters.ADDUCT_LIBRARY)
        .getEmbeddedParameters();
    annotationParameters =
        IonNetworkingParameters.createFullParamSet(annotationParameters, mzTolerance, minHeight);
    library = new IonNetworkLibrary(
        annotationParameters.getParameter(IonNetworkingParameters.LIBRARY).getEmbeddedParameters(),
        mzTolerance);

    // MS2 similarity
    checkMS2Similarity =
        parameterSet.getParameter(SimpleMetaCorrelateParameters.MS2_SIMILARITY_CHECK).getValue();

    ms2SimilarityCheckParam = new MS2SimilarityParameters();
    ms2SimilarityCheckParam.getParameter(MS2SimilarityParameters.MASS_LIST).setValue(massListMS2);
    ms2SimilarityCheckParam.getParameter(MS2SimilarityParameters.MAX_DP_FOR_DIFF).setValue(25);
    ms2SimilarityCheckParam.getParameter(MS2SimilarityParameters.MIN_DP).setValue(3);
    ms2SimilarityCheckParam.getParameter(MS2SimilarityParameters.MIN_MATCH).setValue(3);
    ms2SimilarityCheckParam.getParameter(MS2SimilarityParameters.MIN_HEIGHT).setValue(0d);
    ms2SimilarityCheckParam.getParameter(MS2SimilarityParameters.MZ_TOLERANCE).setValue(mzTolMS2);

    // refinement
    annotationParameters.getParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS)
        .setValue(true);
    IonNetworkRefinementParameters refineParam = annotationParameters
        .getParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS).getEmbeddedParameters();
    refineParam.getParameter(IonNetworkRefinementParameters.DELETE_WITHOUT_MONOMER).setValue(true);
    refineParam.getParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD).getEmbeddedParameter()
        .setValue(4);
    refineParam.getParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD).setValue(true);

    // END OF ADDUCTS AND REFINEMENT
    // intensity correlation across samples
    useHeightCorrFilter = parameterSet
        .getParameter(SimpleMetaCorrelateParameters.FILTER_FEATURE_HEIGHT_CORR).getValue();

    minHeightCorr = 0.5;
    minDPHeightCorr = 3;
    heightSimMeasure = SimilarityMeasure.PEARSON;

    // suffix
    autoSuffix = !parameters.getParameter(SimpleMetaCorrelateParameters.SUFFIX).getValue();

    if (autoSuffix)
      suffix = MessageFormat.format("corr {2} r>={0} dp>={1}, {3}", minShapeCorrR,
          minCorrelatedDataPoints, shapeSimMeasure, searchAdducts ? "MS annot" : "");
    else
      suffix = parameters.getParameter(SimpleMetaCorrelateParameters.SUFFIX).getEmbeddedParameter()
          .getValue();


    corrParam = new FeatureShapeCorrelationParameters(rtTolerance, noiseLevelCorr,
        minCorrelatedDataPoints, minCorrDPOnFeatureEdge, minShapeCorrR, useTotalShapeCorrFilter,
        minTotalShapeCorrR, shapeSimMeasure);
    heightCorrParam = new InterSampleHeightCorrParameters(minHeightCorr, minDPHeightCorr, minHeight,
        noiseLevelCorr, heightSimMeasure);

    // create grouping param
    groupParam = new CorrelateGroupingParameters(rtTolerance, useGroups, groupingParameter,
        minHeight, noiseLevelCorr, autoSuffix, suffix, minFeatureFilter, groupByFShapeCorr,
        useHeightCorrFilter, corrParam, heightCorrParam);
  }

}
