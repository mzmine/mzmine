/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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
    private List<Feature> featuresList;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Feature> getValue() {
        featuresList = new ArrayList<Feature>();
        PeakList allPeakLists[] = MZmineCore.getProjectManager()
                .getCurrentProject().getPeakLists();
        for (int i = 0; i < allPeakLists.length; i++) {
            int files = allPeakLists[i].getNumberOfRawDataFiles();
            for (int j = 0; j < files; j++) {
                RawDataFile dataFile = allPeakLists[i].getRawDataFile(j);
                int rows = allPeakLists[i].getNumberOfRows();
                for (int k = 0; k < rows; k++) {
                    featuresList.add(allPeakLists[i].getPeak(k, dataFile));
                }
            }
        }
        return featuresList;
    }

    @Override
    public void setValue(List<Feature> newValue) {
        this.featuresList = newValue;
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
        featuresList = component.getValue();
    }

    @Override
    public void setValueToComponent(FeaturesComponent component,
            List<Feature> newValue) {
        component.setValue(newValue);
    }

    @Override
    public UserParameter<List<Feature>, FeaturesComponent> cloneParameter() {
        return null;
    }

}
