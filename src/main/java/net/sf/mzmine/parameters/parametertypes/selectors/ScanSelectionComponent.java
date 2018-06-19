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
     * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
     * Fifth Floor, Boston, MA 02110-1301 USA
     */

    package net.sf.mzmine.parameters.parametertypes.selectors;

    import java.awt.Window;
    import java.awt.event.ActionEvent;
    import java.awt.event.ActionListener;
    import java.text.NumberFormat;
    import java.util.Arrays;

    import javax.swing.BorderFactory;
    import javax.swing.Box;
    import javax.swing.BoxLayout;
    import javax.swing.JButton;
    import javax.swing.JLabel;
    import javax.swing.JPanel;
    import javax.swing.SwingUtilities;

    import com.google.common.base.Strings;
    import com.google.common.collect.Range;

    import net.sf.mzmine.datamodel.MassSpectrumType;
    import net.sf.mzmine.datamodel.PolarityType;
    import net.sf.mzmine.main.MZmineCore;
    import net.sf.mzmine.parameters.Parameter;
    import net.sf.mzmine.parameters.impl.SimpleParameterSet;
    import net.sf.mzmine.parameters.parametertypes.ComboParameter;
    import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
    import net.sf.mzmine.parameters.parametertypes.StringParameter;
    import net.sf.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
    import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
    import net.sf.mzmine.util.ExitCode;
    import net.sf.mzmine.util.GUIUtils;

    public class ScanSelectionComponent extends JPanel implements ActionListener {

        private static final long serialVersionUID = 1L;

        private final JButton setButton, clearButton;

        private final JLabel restrictionsList;

        private Range<Integer> scanNumberRange;
        private Integer baseFilteringInteger;
        private Range<Double> scanRTRange;
        private Integer msLevel;
        private PolarityType polarity;
        private MassSpectrumType spectrumType;
        private String scanDefinition;

        public ScanSelectionComponent() {

            BoxLayout layout = new BoxLayout(this, BoxLayout.X_AXIS);
            setLayout(layout);

            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

            restrictionsList = new JLabel();
            add(restrictionsList);
            updateRestrictionList();

            add(Box.createHorizontalStrut(10));

            setButton = GUIUtils.addButton(this, "Set filters", null, this);
            clearButton = GUIUtils.addButton(this, "Clear filters", null, this);

        }

        void setValue(ScanSelection newValue) {
            scanNumberRange = newValue.getScanNumberRange();
            baseFilteringInteger = newValue.getBaseFilteringInteger();
            scanRTRange = newValue.getScanRTRange();
            polarity = newValue.getPolarity();
            spectrumType = newValue.getSpectrumType();
            msLevel = newValue.getMsLevel();
            scanDefinition = newValue.getScanDefinition();

            updateRestrictionList();
        }

        public ScanSelection getValue() {
            return new ScanSelection(scanNumberRange, baseFilteringInteger, scanRTRange, polarity, spectrumType, msLevel,
                    scanDefinition);
        }

        public void actionPerformed(ActionEvent event) {

            Object src = event.getSource();

            if (src == setButton) {

                SimpleParameterSet paramSet;
                ExitCode exitCode;
                Window parent = (Window) SwingUtilities.getAncestorOfClass(Window.class, this);

                final IntRangeParameter scanNumParameter = new IntRangeParameter("Scan number",
                        "Range of included scan numbers", false, scanNumberRange);
                final IntegerParameter baseFilteringIntegerParameter = new IntegerParameter("Base Filtering Integer",
                        "Enter an integer for which every multiple of that integer in the list will be filtered. (Every Nth element will be shown)", this.baseFilteringInteger, false);
                final RTRangeParameter rtParameter = new RTRangeParameter(false);
                if (scanRTRange != null)
                    rtParameter.setValue(scanRTRange);
                final IntegerParameter msLevelParameter = new IntegerParameter("MS level", "MS level", msLevel, false);
                final StringParameter scanDefinitionParameter = new StringParameter("Scan definition",
                        "Include only scans that match this scan definition. You can use wild cards, e.g. *FTMS*",
                        scanDefinition, false);
                final String polarityTypes[] = {"Any", "+", "-"};
                final ComboParameter<String> polarityParameter = new ComboParameter<>("Polarity",
                        "Include only scans of this polarity", polarityTypes);
                if ((polarity == PolarityType.POSITIVE) || (polarity == PolarityType.NEGATIVE))
                    polarityParameter.setValue(polarity.asSingleChar());
                final String spectraTypes[] = {"Any", "Centroided", "Profile", "Thresholded"};
                final ComboParameter<String> spectrumTypeParameter = new ComboParameter<>("Spectrum type",
                        "Include only spectra of this type", spectraTypes);
                if (spectrumType != null) {
                    switch (spectrumType) {
                        case CENTROIDED:
                            spectrumTypeParameter.setValue(spectraTypes[1]);
                            break;
                        case PROFILE:
                            spectrumTypeParameter.setValue(spectraTypes[2]);
                            break;
                        case THRESHOLDED:
                            spectrumTypeParameter.setValue(spectraTypes[3]);
                            break;
                    }
                }

                paramSet = new SimpleParameterSet(new Parameter[]{scanNumParameter, baseFilteringIntegerParameter, rtParameter, msLevelParameter,
                        scanDefinitionParameter, polarityParameter, spectrumTypeParameter});
                exitCode = paramSet.showSetupDialog(parent, true);
                if (exitCode == ExitCode.OK) {
                    scanNumberRange = paramSet.getParameter(scanNumParameter).getValue();
                    this.baseFilteringInteger = paramSet.getParameter(baseFilteringIntegerParameter).getValue();
                    scanRTRange = paramSet.getParameter(rtParameter).getValue();
                    msLevel = paramSet.getParameter(msLevelParameter).getValue();
                    scanDefinition = paramSet.getParameter(scanDefinitionParameter).getValue();
                    final int selectedPolarityIndex = Arrays.asList(polarityTypes)
                            .indexOf(paramSet.getParameter(polarityParameter).getValue());
                    switch (selectedPolarityIndex) {
                        case 1:
                            polarity = PolarityType.POSITIVE;
                            break;
                        case 2:
                            polarity = PolarityType.NEGATIVE;
                            break;
                        default:
                            polarity = null;
                            break;
                    }
                    final int selectedSpectraTypeIndex = Arrays.asList(spectraTypes)
                            .indexOf(paramSet.getParameter(spectrumTypeParameter).getValue());
                    switch (selectedSpectraTypeIndex) {
                        case 1:
                            spectrumType = MassSpectrumType.CENTROIDED;
                            break;
                        case 2:
                            spectrumType = MassSpectrumType.PROFILE;
                            break;
                        case 3:
                            spectrumType = MassSpectrumType.THRESHOLDED;
                            break;
                        default:
                            spectrumType = null;
                            break;
                    }

                }
            }

            if (src == clearButton) {
                scanNumberRange = null;
                baseFilteringInteger = null;
                scanRTRange = null;
                polarity = null;
                spectrumType = null;
                msLevel = null;
                scanDefinition = null;
            }

            updateRestrictionList();

        }

        @Override
        public void setToolTipText(String toolTip) {
            restrictionsList.setToolTipText(toolTip);
        }

        private void updateRestrictionList() {

            if ((scanNumberRange == null) && (scanRTRange == null) && (polarity == null) && (spectrumType == null)
                    && (msLevel == null) && Strings.isNullOrEmpty(scanDefinition) && (baseFilteringInteger == null)) {
                restrictionsList.setText("All");
                return;
            }

            StringBuilder newText = new StringBuilder("<html>");
            if (scanNumberRange != null) {
                int upperEndpoint = scanNumberRange.upperEndpoint();
                String maxCountText = upperEndpoint == Integer.MAX_VALUE ? "Max" : Integer.toString(upperEndpoint);
                newText.append("Scan number: " + scanNumberRange.lowerEndpoint() + " - " + maxCountText
                        + "<br>");
            }
            if (baseFilteringInteger != null) {
                newText.append("Base Filtering Integer: " + baseFilteringInteger + "<br>");
            }
            if (scanRTRange != null) {
                NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
                newText.append("Retention time: " + rtFormat.format(scanRTRange.lowerEndpoint()) + " - "
                        + rtFormat.format(scanRTRange.upperEndpoint()) + " min.<br>");
            }
            if (msLevel != null) {
                newText.append("MS level: " + msLevel + "<br>");
            }
            if (!Strings.isNullOrEmpty(scanDefinition)) {
                newText.append("Scan definition: " + scanDefinition + "<br>");
            }
            if (polarity != null) {
                newText.append("Polarity: " + polarity.asSingleChar() + "<br>");
            }
            if (spectrumType != null) {
                newText.append("Spectrum type: " + spectrumType.toString().toLowerCase());
            }

            restrictionsList.setText(newText.toString());
        }
    }
