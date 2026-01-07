/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.javafx.components.animations;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.beans.binding.BooleanExpression;
import javafx.scene.Node;
import javafx.util.Duration;
import javafx.util.Subscription;

public class FxFlashingAnimation {

  /**
   * Returns the subscription to the flashing property
   *
   * @param node             that should flash
   * @param flashingProperty true if flashing is on
   * @return the subscription to the flashing property
   */
  public static Subscription animate(final Node node, final BooleanExpression flashingProperty) {
    return animate(node, flashingProperty, Duration.millis(900), 0.35);
  }

  /**
   * Returns the subscription to the flashing property
   *
   * @param node             that should flash
   * @param flashingProperty true if flashing is on
   * @param duration         duration of animation
   * @param fromOpacity      start opacity between 0..1 end is fixed to 1
   * @return the subscription to the flashing property
   */
  public static Subscription animate(final Node node, final BooleanExpression flashingProperty,
      final Duration duration, final double fromOpacity) {
    FadeTransition fade = new FadeTransition(duration, node);
    fade.setFromValue(fromOpacity);
    fade.setToValue(1);
    fade.setCycleCount(Animation.INDEFINITE);
    fade.setAutoReverse(true);
    fade.setInterpolator(Interpolator.EASE_BOTH);
//    fade.setRate();
    return FxConditionalTransitions.startConditionally(fade, flashingProperty);
  }

}
