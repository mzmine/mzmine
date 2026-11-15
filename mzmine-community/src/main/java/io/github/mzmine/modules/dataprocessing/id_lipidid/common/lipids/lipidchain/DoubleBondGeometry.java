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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;

/**
 * Geometric configuration of one localized carbon-carbon double bond.
 * <p>
 * {@link #UNKNOWN} has an empty shorthand symbol, so an OAD localization without stereochemical
 * evidence renders as {@code 18:1(9)} while known geometry renders as {@code 18:1(9Z)} or
 * {@code 18:1(9E)}.
 */
public enum DoubleBondGeometry implements UniqueIdSupplier {
  Z("Z"), E("E"), UNKNOWN("");

  private final @NotNull String symbol;

  DoubleBondGeometry(final @NotNull String symbol) {
    this.symbol = symbol;
  }

  public @NotNull String getSymbol() {
    return symbol;
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case Z -> "z";
      case E -> "e";
      case UNKNOWN -> "unknown";
    };
  }
}
