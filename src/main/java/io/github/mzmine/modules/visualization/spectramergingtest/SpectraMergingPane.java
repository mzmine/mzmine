package io.github.mzmine.modules.visualization.spectramergingtest;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.ImsMsMsInfo;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.FastColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleXYLabelGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithPreview;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.SpectraMerging;
import io.github.mzmine.util.scans.SpectraMerging.MergingType;
import java.text.NumberFormat;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class SpectraMergingPane extends ParameterSetupDialogWithPreview {

  private static final MemoryMapStorage storage = new MemoryMapStorage();

  private final SimpleXYChart<MassSpectrumProvider> plot;
  private final NumberFormat mzFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;
  private final ComboBox<ImsMsMsInfo> msMsInfoComboBox;
  private final SimpleParameterSet parameterSet;

  public SpectraMergingPane(SimpleParameterSet parameterSet) {
    super(true, parameterSet);
    this.parameterSet = parameterSet;

    plot = new SimpleXYChart<>();
    plot.setDomainAxisLabel("m/z");
    plot.setRangeAxisLabel("Intensity");

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    previewWrapperPane.setCenter(plot);

    final GridPane controlPane = new GridPane();
    previewWrapperPane.setBottom(controlPane);

    RawDataFile[] files = MZmineCore.getProjectManager().getCurrentProject().getDataFiles();

    ComboBox<RawDataFile> fileComboBox = new ComboBox<>(FXCollections.observableArrayList(files));
    ComboBox<Frame> frameComboBox = new ComboBox<>();
    msMsInfoComboBox = new ComboBox<>();

    fileComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue instanceof IMSRawDataFile) {
        frameComboBox
            .setItems(FXCollections.observableArrayList(((IMSRawDataFile) newValue).getFrames()));
      }
    });

    frameComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        Set<ImsMsMsInfo> infos = newValue.getImsMsMsInfos();
        msMsInfoComboBox.setItems(FXCollections.observableArrayList(infos));
      }
    });

    msMsInfoComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
      parametersChanged();
    });

    controlPane.add(new Label("Raw data file"), 0, 0);
    controlPane.add(fileComboBox, 1, 0);
    controlPane.add(new Label("Frame"), 0, 1);
    controlPane.add(frameComboBox, 1, 1);
    controlPane.add(new Label("MS/MS info"), 0, 2);
    controlPane.add(msMsInfoComboBox, 1, 2);

  }

  @Override
  protected void parametersChanged() {
    updateParameterSetFromComponents();
    double noiseLevel = parameterSet.getParameter(SpectraMergingTestParameters.noiseLevel)
        .getValue();
    MergingType mergingType = parameterSet.getParameter(SpectraMergingTestParameters.mergingType)
        .getValue();
    MZTolerance mzTolerance = parameterSet.getParameter(SpectraMergingTestParameters.mzTolerance)
        .getValue();

    if(mergingType == null || mzTolerance == null) {
      return;
    }

    MergedMsMsSpectrum merged = SpectraMerging
        .getMergedMsMsSpectrumForPASEF(msMsInfoComboBox.getValue(), noiseLevel, mzTolerance,
            mergingType, storage);

    if (merged == null) {
      return;
    }

    ColoredXYBarRenderer coloredXYBarRenderer = new ColoredXYBarRenderer(false);
    coloredXYBarRenderer.setDefaultItemLabelGenerator(new SimpleXYLabelGenerator(plot));

    EStandardChartTheme theme = MZmineCore.getConfiguration().getDefaultChartTheme();
    plot.removeAllDatasets();
    plot.addDataset(new FastColoredXYDataset(new MassSpectrumProvider(merged, msMsInfoComboBox.getValue().toString())),
        coloredXYBarRenderer);
  }
}
