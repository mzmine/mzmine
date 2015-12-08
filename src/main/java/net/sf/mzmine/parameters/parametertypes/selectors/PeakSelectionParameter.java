/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes.selectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.XMLUtils;

public class PeakSelectionParameter
        implements UserParameter<List<PeakSelection>, PeakSelectionComponent> {

    private final String name, description;
    private List<PeakSelection> value;

    public PeakSelectionParameter() {
        this("Peaks", "Select peaks that should be included.", null);
    }

    public PeakSelectionParameter(String name, String description,
            List<PeakSelection> defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
    }

    @Override
    public List<PeakSelection> getValue() {
        return value;
    }

    @Override
    public void setValue(List<PeakSelection> newValue) {
        this.value = Lists.newArrayList(newValue);
    }

    @Override
    public PeakSelectionParameter cloneParameter() {
        PeakSelectionParameter copy = new PeakSelectionParameter(name,
                description, Lists.newArrayList(value));
        return copy;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean checkValue(Collection<String> errorMessages) {
        if ((value == null) || (value.size() == 0)) {
            errorMessages.add("No peaks selected");
            return false;
        }
        return true;
    }

    @Override
    public void loadValueFromXML(Element xmlElement) {

        List<PeakSelection> newValue = Lists.newArrayList();
        NodeList selItems = xmlElement.getElementsByTagName("selection");
        for (int i = 0; i < selItems.getLength(); i++) {
            Element selElement = (Element) selItems.item(i);
            Range<Integer> idRange = XMLUtils.parseIntegerRange(selElement,
                    "id");
            Range<Double> mzRange = XMLUtils.parseDoubleRange(selElement, "mz");
            Range<Double> rtRange = XMLUtils.parseDoubleRange(selElement, "rt");
            String name = XMLUtils.parseString(selElement, "name");
            PeakSelection ps = new PeakSelection(idRange, mzRange, rtRange,
                    name);
            newValue.add(ps);
        }
        this.value = newValue;
    }

    @Override
    public void saveValueToXML(Element xmlElement) {
        if (value == null)
            return;
        Document parentDocument = xmlElement.getOwnerDocument();

        for (PeakSelection ps : value) {
            Element selElement = parentDocument.createElement("selection");
            xmlElement.appendChild(selElement);
            XMLUtils.appendRange(selElement, "id", ps.getIDRange());
            XMLUtils.appendRange(selElement, "mz", ps.getMZRange());
            XMLUtils.appendRange(selElement, "rt", ps.getRTRange());
            XMLUtils.appendString(selElement, "name", ps.getName());
        }

    }

    @Override
    public PeakSelectionComponent createEditingComponent() {
        return new PeakSelectionComponent();
    }

    @Override
    public void setValueFromComponent(PeakSelectionComponent component) {
        value = component.getValue();
    }

    @Override
    public void setValueToComponent(PeakSelectionComponent component,
            List<PeakSelection> newValue) {
        component.setValue(newValue);
    }

    /**
     * Shortcut to set value based on peak list rows
     */
    public void setValue(PeakListRow rows[]) {
        List<PeakSelection> newValue = Lists.newArrayList();
        for (PeakListRow row : rows) {
            Range<Integer> idRange = Range.singleton(row.getID());
            Range<Double> mzRange = Range.singleton(row.getAverageMZ());
            Range<Double> rtRange = Range.singleton(row.getAverageRT());
            PeakSelection ps = new PeakSelection(idRange, mzRange, rtRange,
                    null);
            newValue.add(ps);
        }
        setValue(newValue);
    }

    public PeakListRow[] getMatchingRows(PeakList peakList) {

        final List<PeakListRow> matchingRows = new ArrayList<>();
        rows: for (PeakListRow row : peakList.getRows()) {
            for (PeakSelection ps : value) {
                if (ps.checkPeakListRow(row)) {
                    matchingRows.add(row);
                    continue rows;
                }
            }
        }
        return matchingRows.toArray(new PeakListRow[matchingRows.size()]);

    }

}
