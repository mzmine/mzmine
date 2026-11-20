/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 */
package io.github.mzmine.modules.tools.batchwizard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/** Startup registration of optional wizard UI contributions; community registers none. */
public final class WizardExtensions {

  private static final Map<String, Function<BatchWizardTab, WizardExtension>> factories =
      new LinkedHashMap<>();

  private WizardExtensions() {
  }

  public static synchronized void register(final @NotNull String id,
      final @NotNull Function<BatchWizardTab, WizardExtension> factory) {
    factories.put(id, factory);
  }

  static synchronized @NotNull List<WizardExtension> create(final @NotNull BatchWizardTab tab) {
    return factories.values().stream().map(factory -> factory.apply(tab)).toList();
  }
}
