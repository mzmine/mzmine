/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_addmanualcomp;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.util.annotations.ConnectedTypeCalculation;
import io.mzio.users.user.CurrentUserService;
import java.util.List;
import javafx.collections.ObservableMap;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

public class CompoundAnnotationController extends FxController<CompoundAnnotationModel> {

  private final CompoundAnnotationViewBuilder builder;
  private final FeatureTableFX featureTable;
  private Stage stage = new Stage();

  public CompoundAnnotationController(@NotNull FeatureListRow row, FeatureTableFX featureTable) {
    super(new CompoundAnnotationModel());
    this.featureTable = featureTable;
    model.setRow(row);
    builder = new CompoundAnnotationViewBuilder(model, this::onSave, this::onCancel);
    stage.setOnCloseRequest(e -> onCancel());
  }

  @Override
  protected @NotNull FxViewBuilder<CompoundAnnotationModel> getViewBuilder() {
    return builder;
  }

  public void showWindow() {
    final Scene scene = new Scene(builder.build(), 500, 400);
    ConfigService.getConfiguration().getTheme().apply(scene.getStylesheets());

    stage.setScene(scene);
    stage.setTitle("Annotate manually - %s".formatted(
        ConfigService.getGuiFormats().mz(model.getRow().getAverageMZ())));
    stage.show();
  }

  public void onCancel() {
    stage.hide();
  }

  public void onSave() {
    final ObservableMap<DataType, Object> dataModel = model.getDataModel();
    final SimpleCompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
    final ModularFeatureListRow row = (ModularFeatureListRow) model.getRow();

    dataModel.entrySet().forEach(e -> annotation.put(e.getKey(), e.getValue()));
    ConnectedTypeCalculation.LIST.forEach(ctc -> ctc.calculateIfAbsent(row, annotation));

    annotation.put(DataTypes.get(DatabaseNameType.class), "Annotated manually by %s".formatted(
        CurrentUserService.getUser() != null ? CurrentUserService.getUser().getNickname()
            : "unknown user."));

    ManualCompoundAnnotationModule.annotate(row, List.of(annotation), true);
    if (featureTable != null) {
      featureTable.rebuild();
    }
    stage.hide();
  }
}
