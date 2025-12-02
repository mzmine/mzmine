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

import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;

public class GlobalIonLibrariesTab extends SimpleTab {

  public static final String HEADER = "Ion libraries";

  // lazy init singleton
  private static class Holder {

    private static final GlobalIonLibrariesTab INSTANCE = new GlobalIonLibrariesTab();
  }

  private GlobalIonLibrariesTab() {
    super(HEADER);
    GlobalIonLibrariesController controller = GlobalIonLibrariesController.getInstance();
    setContent(controller.buildView());

    setOnClosed(_ -> {
      // not needed?
//      controller.close();
    });
  }

  public static GlobalIonLibrariesTab getInstance() {
    return Holder.INSTANCE;
  }

  public static GlobalIonLibrariesTab showTab() {
    var instance = getInstance();
    FxThread.runLater(() -> {
      var tabPane = instance.getTabPane();
      if (tabPane != null) {
        // show tab
        tabPane.getSelectionModel().select(instance);
      } else {
        MZmineCore.getDesktop().addTab(instance);
      }
    });
    return instance;
  }
}
