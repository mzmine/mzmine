package io.github.mzmine.modules.visualization.masst;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.io.File;
import java.net.MalformedURLException;
import javafx.scene.layout.Region;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.jetbrains.annotations.NotNull;

public class MasstVisualizerViewBuilder extends FxViewBuilder<MasstVisualizerModel> {

  public MasstVisualizerViewBuilder(final @NotNull MasstVisualizerModel model) {
    super(model);
  }

  @Override
  public Region build() {
    var web = new WebView();
    model.masstFileProperty().subscribe(file -> {
      try {
        if (file != null) {
          web.getEngine().load(new File(file).toURI().toURL().toExternalForm());
        }
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    });
    var main = FxLayout.newBorderPane(web);
    main.setTop(FxLayout.newHBox(
        FxTextFields.newTextField(30, model.masstFileProperty(), "Local MASST file"),
        FxButtons.createButton("Select", "", () -> {
          var file = new FileChooser().showOpenDialog(main.getScene().getWindow());
          if (file != null) {
            model.setMasstFile(file.getAbsolutePath());
          }
        })));
    return main;
  }
}
