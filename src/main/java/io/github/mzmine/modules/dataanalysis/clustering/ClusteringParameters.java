/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.dataanalysis.clustering;

import java.util.Arrays;

import io.github.mzmine.modules.dataanalysis.clustering.em.EMClusterer;
import io.github.mzmine.modules.dataanalysis.clustering.farthestfirst.FarthestFirstClusterer;
import io.github.mzmine.modules.dataanalysis.clustering.hierarchical.HierarClusterer;
import io.github.mzmine.modules.dataanalysis.clustering.simplekmeans.SimpleKMeansClusterer;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakSelection;
import io.github.mzmine.parameters.parametertypes.selectors.PeakSelectionParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.PeakMeasurementType;

public class ClusteringParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final ComboParameter<PeakMeasurementType> peakMeasurementType = new ComboParameter<PeakMeasurementType>(
            "Peak measurement type", "Measure peaks using",
            PeakMeasurementType.values());

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(
            new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES));

    public static final PeakSelectionParameter rows = new PeakSelectionParameter(
            "Feature list rows", "Feature list rows to include in calculation",
            Arrays.asList(new PeakSelection[] {
                    new PeakSelection(null, null, null, null) }));

    private static ClusteringAlgorithm algorithms[] = new ClusteringAlgorithm[] {
            new EMClusterer(), new FarthestFirstClusterer(),
            new SimpleKMeansClusterer(), new HierarClusterer() };

    public static final ModuleComboParameter<ClusteringAlgorithm> clusteringAlgorithm = new ModuleComboParameter<ClusteringAlgorithm>(
            "Clustering algorithm",
            "Select the algorithm you want to use for clustering", algorithms);

    public static final ComboParameter<ClusteringDataType> typeOfData = new ComboParameter<ClusteringDataType>(
            "Type of data",
            "Specify the type of data used for the clustering: samples or variables",
            ClusteringDataType.values());

    public ClusteringParameters() {
        super(new Parameter[] { peakLists, peakMeasurementType, dataFiles, rows,
                clusteringAlgorithm, typeOfData });
    }

}
