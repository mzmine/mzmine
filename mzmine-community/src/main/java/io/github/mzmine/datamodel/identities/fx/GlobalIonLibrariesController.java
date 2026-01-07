/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.identities.fx;

import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import javafx.beans.property.ReadOnlyListProperty;
import org.jetbrains.annotations.NotNull;

/**
 * This class is the controller of the ion libraries tab that holds the observable instances of
 * global ion list of {@link IonLibrary}, {@link IonType}, and {@link IonPart}. The properties are
 * bound to javafx so they can only be modified on the javafx thread.
 * <p>
 * The {@link GlobalIonLibraryService} holds the thread safe original instances of global ions. This
 * is used for parsing etc while this controller class is used for visualization and modifications.
 */
public class GlobalIonLibrariesController extends FxController<GlobalIonLibrariesModel> {

  // lazy init singleton
  private static class Holder {

    private static final GlobalIonLibrariesController INSTANCE = new GlobalIonLibrariesController();
  }

  private final GlobalIonLibrariesViewBuilder viewBuilder;
  private final GlobalIonLibrariesInteractor interactor;

  private GlobalIonLibrariesController() {
    super(new GlobalIonLibrariesModel());

    interactor = new GlobalIonLibrariesInteractor(model);

    viewBuilder = new GlobalIonLibrariesViewBuilder(model, interactor::handleEvent);
  }

  public ReadOnlyListProperty<IonLibrary> librariesProperty() {
    return model.librariesProperty().getReadOnlyProperty();
  }

  public ReadOnlyListProperty<IonType> ionTypesProperty() {
    return model.ionTypesProperty().getReadOnlyProperty();
  }

  public ReadOnlyListProperty<IonPart> partsProperty() {
    return model.partsProperty().getReadOnlyProperty();
  }

  @Override
  protected @NotNull FxViewBuilder<GlobalIonLibrariesModel> getViewBuilder() {
    return viewBuilder;
  }

  /**
   * Uses a static singleton instance so that there is only one source of truth for ions
   */
  public static GlobalIonLibrariesController getInstance() {
    return Holder.INSTANCE;
  }
}
