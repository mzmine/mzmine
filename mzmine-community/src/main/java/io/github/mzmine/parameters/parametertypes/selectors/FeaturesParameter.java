/*
 * Copyright (c) 2004-2024 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.project.ProjectService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author akshaj This class represents the parameter Features in the parameter setup dialog of the
 * Fx3DVisualizer.
 */
public class FeaturesParameter implements UserParameter<List<Feature>, FeaturesComponent> {

  private String name = "Features";
  private List<Feature> value;
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Feature> getValue() {
    return value;
  }

  @Override
  public void setValue(List<Feature> newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return true;
  }

  /*
   * @see io.github.mzmine.parameters.Parameter#loadValueFromXML(org.w3c.dom. Element)
   */
  @Override
  public void loadValueFromXML(Element xmlElement) {

    FeatureList[] allPeakLists = ProjectService.getProjectManager().getCurrentProject()
        .getCurrentFeatureLists().toArray(FeatureList[]::new);

    List<Feature> newValues = new ArrayList<>();

    NodeList items = xmlElement.getElementsByTagName("feature");
    for (int i = 0; i < items.getLength(); i++) {
      Node doc = items.item(i);
      if (doc instanceof Element) {
        Element docElement = (Element) doc;
        for (FeatureList peakList : allPeakLists) {
          FeatureListRow[] rows = peakList.getRows().toArray(FeatureListRow[]::new);
          RawDataFile[] dataFiles = peakList.getRawDataFiles().toArray(RawDataFile[]::new);
          if (peakList.getName()
              .equals(docElement.getElementsByTagName("peaklist_name").item(0).getNodeValue())) {
            int rownum = 0;
            for (FeatureListRow row : rows) {
              if (row.toString().equals(
                  docElement.getElementsByTagName("peaklist_row_id").item(0).getNodeValue())) {
                for (RawDataFile dataFile : dataFiles) {
                  if (dataFile.getName().equals(
                      docElement.getElementsByTagName("rawdatafile_name").item(0).getNodeValue())) {
                    Feature feature = peakList.getFeature(rownum, dataFile);
                    if (feature != null) {
                      newValues.add(feature);
                    }
                  }
                }
              }
              rownum++;
            }
          }
        }
      }
    }
    this.value = newValues;
    logger.finest("Values have been loaded from XML");
  }

  /*
   * @see io.github.mzmine.parameters.Parameter#saveValueToXML(org.w3c.dom.Element)
   */
  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }
    Document parentDocument = xmlElement.getOwnerDocument();

    for (Feature item : value) {
      Element featureElement = parentDocument.createElement("feature");

      Element peakListElement = parentDocument.createElement("peaklist_name");
      if (item.getFeatureList() != null) {
        peakListElement.setNodeValue(item.getFeatureList().getName());
        featureElement.appendChild(peakListElement);

        FeatureListRow row = item.getFeatureList().getFeatureRow(item);
        Element peakListRowElement = parentDocument.createElement("peaklist_row_id");
        if (row != null) {
          peakListRowElement.setNodeValue(row.toString());
        }
        featureElement.appendChild(peakListRowElement);
      }

      Element rawDataFileElement = parentDocument.createElement("rawdatafile_name");
      if (item.getRawDataFile() != null) {
        rawDataFileElement.setNodeValue(item.getRawDataFile().getName());
      }
      featureElement.appendChild(rawDataFileElement);

      xmlElement.appendChild(featureElement);
    }
    logger.finest("Values are saved to XML");
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
    value = component.getValue();
  }

  @Override
  public void setValueToComponent(FeaturesComponent component, @Nullable List<Feature> newValue) {
    component.setValue(newValue);
  }

  /*
   * @see io.github.mzmine.parameters.UserParameter#cloneParameter()
   */
  @Override
  public FeaturesParameter cloneParameter() {
    FeaturesParameter copy = new FeaturesParameter();
    if (value != null) {
      copy.value = new ArrayList<Feature>(value);
    }
    return copy;
  }

}
