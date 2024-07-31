package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class DialogController extends FxController<Object> {

  protected DialogController() {
    super(new Object());
  }

  @Override
  protected @NotNull FxViewBuilder<Object> getViewBuilder() {
    return new FxViewBuilder<>(model) {
      @Override
      public Region build() {
        return null;
      }
    };
  }
}
