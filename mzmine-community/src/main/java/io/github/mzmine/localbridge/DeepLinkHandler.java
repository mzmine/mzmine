/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.localbridge;

import org.jetbrains.annotations.NotNull;

/** Handles a validated deep link on the JavaFX application thread. */
@FunctionalInterface
public interface DeepLinkHandler {

  void navigate(@NotNull DeepLink deepLink);
}
