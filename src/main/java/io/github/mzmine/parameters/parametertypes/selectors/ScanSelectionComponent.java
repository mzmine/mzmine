/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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


import com.google.common.base.Strings;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MobilityRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.util.ExitCode;
import java.text.NumberFormat;
import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;

public class ScanSelectionComponent extends FlowPane {

  private final Button setButton, clearButton;

  private final Label restrictionsList;

  private Range<Integer> scanNumberRange;
  private Integer baseFilteringInteger;
  private Range<Double> scanMobilityRange;
  private Range<Float> scanRTRange;
  private Integer msLevel;
  private PolarityType polarity;
  private MassSpectrumType spectrumType;
  private String scanDefinition;

  public ScanSelectionComponent() {

    // setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

    setHgap(5.0);

    restrictionsList = new Label();
    updateRestrictionList();

    // add(Box.createHorizontalStrut(10));

    setButton = new Button("Set filters");
    setButton.setOnAction(e -> {
      SimpleParameterSet paramSet;
      ExitCode exitCode;

      final IntRangeParameter scanNumParameter = new IntRangeParameter("Scan number",
          "Range of included scan numbers", false, scanNumberRange);
      final IntegerParameter baseFilteringIntegerParameter = new IntegerParameter(
          "Base Filtering Integer",
          "Enter an integer for which every multiple of that integer in the list will be filtered. (Every Nth element will be shown)",
          this.baseFilteringInteger, false);
      final RTRangeParameter rtParameter = new RTRangeParameter(false);
      // TODO: FloatRangeComponent
      if (scanRTRange != null) {
        rtParameter.setValue(Range.closed(scanRTRange.lowerEndpoint().doubleValue(),
            scanRTRange.upperEndpoint().doubleValue()));
      }
      final MobilityRangeParameter mobilityParameter = new MobilityRangeParameter(false);
      if (scanMobilityRange != null) {
        mobilityParameter.setValue(scanMobilityRange);
      }
      final IntegerParameter msLevelParameter =
          new IntegerParameter("MS level", "MS level", msLevel, false);
      final StringParameter scanDefinitionParameter = new StringParameter("Scan definition",
          "Include only scans that match this scan definition. You can use wild cards, e.g. *FTMS*",
          scanDefinition, false);
      final String polarityTypes[] = {"Any", "+", "-"};
      final ComboParameter<String> polarityParameter = new ComboParameter<>("Polarity",
          "Include only scans of this polarity", FXCollections.observableArrayList(polarityTypes));
      if ((polarity == PolarityType.POSITIVE) || (polarity == PolarityType.NEGATIVE)) {
        polarityParameter.setValue(polarity.asSingleChar());
      }
      final String spectraTypes[] = {"Any", "Centroided", "Profile", "Thresholded"};
      final ComboParameter<String> spectrumTypeParameter = new ComboParameter<>("Spectrum type",
          "Include only spectra of this type", FXCollections.observableArrayList(spectraTypes));
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

      paramSet = new SimpleParameterSet(new Parameter[]{scanNumParameter,
          baseFilteringIntegerParameter, rtParameter, mobilityParameter, msLevelParameter,
          scanDefinitionParameter, polarityParameter, spectrumTypeParameter});
      exitCode = paramSet.showSetupDialog(true);
      if (exitCode == ExitCode.OK) {
        scanNumberRange = paramSet.getParameter(scanNumParameter).getValue();
        this.baseFilteringInteger = paramSet.getParameter(baseFilteringIntegerParameter).getValue();
        scanMobilityRange = paramSet.getParameter(mobilityParameter).getValue();
        // TODO: FloatRangeComponent - causes npe -> wrapped in if. can be removed when float
        //  comp exists
//        scanRTRange = Range.closed(paramSet.getParameter(rtParameter).getValue().lowerEndpoint().floatValue(),
//            paramSet.getParameter(rtParameter).getValue().upperEndpoint().floatValue());
        if (paramSet.getParameter(rtParameter).getValue() != null) {
          scanRTRange = Range
              .closed(paramSet.getParameter(rtParameter).getValue().lowerEndpoint().floatValue(),
                  paramSet.getParameter(rtParameter).getValue().upperEndpoint().floatValue());
        } else {
          scanRTRange = null;
        }

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
      updateRestrictionList();

    });


    clearButton = new Button("Clear filters");
    clearButton.setOnAction(e -> {
      scanNumberRange = null;
      baseFilteringInteger = null;
      scanRTRange = null;
      scanMobilityRange = null;
      polarity = null;
      spectrumType = null;
      msLevel = null;
      scanDefinition = null;
      updateRestrictionList();

    });

    getChildren().addAll(restrictionsList, setButton, clearButton);

  }

  public void setValue(ScanSelection newValue) {
    scanNumberRange = newValue.getScanNumberRange();
    baseFilteringInteger = newValue.getBaseFilteringInteger();
    scanRTRange = newValue.getScanRTRange();
    scanMobilityRange = newValue.getScanMobilityRange();
    polarity = newValue.getPolarity();
    spectrumType = newValue.getSpectrumType();
    msLevel = newValue.getMsLevel();
    scanDefinition = newValue.getScanDefinition();

    updateRestrictionList();
  }

  public ScanSelection getValue() {
    return new ScanSelection(scanNumberRange, baseFilteringInteger, scanRTRange, scanMobilityRange,
        polarity, spectrumType, msLevel, scanDefinition);
  }


  public void setToolTipText(String toolTip) {
    restrictionsList.setTooltip(new Tooltip(toolTip));
  }

  private void updateRestrictionList() {

    if ((scanNumberRange == null) && (scanRTRange == null) && (polarity == null)
        && (spectrumType == null) && (msLevel == null) && Strings.isNullOrEmpty(scanDefinition)
        && (baseFilteringInteger == null)) {
      restrictionsList.setText("All");
      return;
    }

    StringBuilder newText = new StringBuilder();
    if (scanNumberRange != null) {
      int upperEndpoint = scanNumberRange.upperEndpoint();
      String maxCountText =
          upperEndpoint == Integer.MAX_VALUE ? "Max" : Integer.toString(upperEndpoint);
      newText
          .append("Scan number: " + scanNumberRange.lowerEndpoint() + " - " + maxCountText + "\n");
    }
    if (baseFilteringInteger != null) {
      newText.append("Base Filtering Integer: " + baseFilteringInteger + "\n");
    }
    if (scanRTRange != null) {
      NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
      newText.append("Retention time: " + rtFormat.format(scanRTRange.lowerEndpoint()) + " - "
          + rtFormat.format(scanRTRange.upperEndpoint()) + " min.\n");
    }
    if (scanMobilityRange != null) {
      NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
      newText.append("Mobility: " + mobilityFormat.format(scanMobilityRange.lowerEndpoint()) + " - "
          + mobilityFormat.format(scanMobilityRange.upperEndpoint()) + ".\n");
    }
    if (msLevel != null) {
      newText.append("MS level: " + msLevel + "\n");
    }
    if (!Strings.isNullOrEmpty(scanDefinition)) {
      newText.append("Scan definition: " + scanDefinition + "\n");
    }
    if (polarity != null) {
      newText.append("Polarity: " + polarity.asSingleChar() + "\n");
    }
    if (spectrumType != null) {
      newText.append("Spectrum type: " + spectrumType.toString().toLowerCase());
    }

    restrictionsList.setText(newText.toString());
  }
}
