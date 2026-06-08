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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.gui.framework.listener.DelayedDocumentListener;
import io.github.mzmine.parameters.ValueChangeDecorator;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class StringParameterComponent extends TextField implements ValueChangeDecorator {

  private final List<Runnable> valueChangeListeners = new ArrayList<>();
  private final PauseTransition delay = new PauseTransition(Duration.millis(500));

  public StringParameterComponent() {
    super();
    textProperty().addListener((_, _, _) -> delay.playFromStart());
    delay.setOnFinished(_ -> {
      for (Runnable lst : valueChangeListeners) {
        lst.run();
      }
    });
  }

  @Override
  public void addValueChangedListener(Runnable onChange) {
    valueChangeListeners.add(onChange);
  }
}
