package net.sf.mzmine.parameters.parametertypes.selectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Element;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.UserParameter;

public class FeaturesParameter
        implements UserParameter<List<Feature>, FeaturesComponent> {

    private String name = "Features";
    private List<Feature> featureList;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Feature> getValue() {
        featureList = new ArrayList<Feature>();
        PeakList allPeakLists[] = MZmineCore.getProjectManager()
                .getCurrentProject().getPeakLists();
        for (int i = 0; i < allPeakLists.length; i++) {
            int files = allPeakLists[i].getNumberOfRawDataFiles();
            for (int j = 0; j < files; j++) {
                RawDataFile dataFile = allPeakLists[i].getRawDataFile(j);
                int rows = allPeakLists[i].getNumberOfRows();
                for (int k = 0; k < rows; k++) {
                    featureList.add(allPeakLists[i].getPeak(k, dataFile));
                }
            }
        }
        return featureList;
    }

    @Override
    public void setValue(List<Feature> newValue) {
        this.featureList = newValue;
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {
        // TODO Auto-generated method stub

    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getDescription() {
        return "Features that this module will take as its input.";
    }

    @Override
    public FeaturesComponent createEditingComponent() {
        FeaturesComponent featuresComponent = new FeaturesComponent();
        return featuresComponent;
    }

    @Override
    public void setValueFromComponent(FeaturesComponent component) {
        featureList = component.getValue();
    }

    @Override
    public void setValueToComponent(FeaturesComponent component,
            List<Feature> newValue) {
        component.setValue(newValue);
    }

    @Override
    public UserParameter<List<Feature>, FeaturesComponent> cloneParameter() {
        // TODO Auto-generated method stub
        return null;
    }

}
