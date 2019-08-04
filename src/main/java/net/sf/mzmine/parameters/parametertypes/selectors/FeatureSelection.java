package net.sf.mzmine.parameters.parametertypes.selectors;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;

public class FeatureSelection {

    private Feature feature;
    private PeakListRow peakListRow;
    private PeakList peakList;
    private RawDataFile rawDataFile;

    FeatureSelection(PeakList peakList, Feature feature,
            PeakListRow peakListRow, RawDataFile rawDataFile) {
        this.peakList = peakList;
        this.feature = feature;
        this.peakListRow = peakListRow;
        this.rawDataFile = rawDataFile;
    }

    public Feature getFeature() {
        return feature;
    }

    public PeakListRow getPeakListRow() {
        return peakListRow;
    }

    public PeakList getPeakList() {
        return peakList;
    }

    public RawDataFile getRawDataFile() {
        return rawDataFile;
    }

}
