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

package io.github.mzmine.datamodel.identities.iontype;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A row for IonType matching, This class makes the search independent of {@link FeatureListRow} and
 * search could be used also for other mz values like from mass spectra.
 * <p>
 * The charge state and polarity may be unknown. Internally the charge and polarity will be defined
 * if either signedCharge or both absCharge+polarity are provided.
 *
 * @param mz           the mz
 * @param signedCharge the signed charge like -1 used to filter potential adducts for this row
 * @param absCharge    the absolute charge Math.abs() to apply some filters
 * @param polarity     the polarity if known
 */
public record IonSearchRow(double mz, @Nullable Integer signedCharge, @Nullable Integer absCharge,
                           @Nullable PolarityType polarity) {

  public IonSearchRow(double mz, @Nullable Integer signedCharge, @Nullable Integer absCharge,
      @Nullable PolarityType polarity) {
    if (absCharge != null) {
      absCharge = Math.abs(absCharge); // just to make sure
    }
    if (!PolarityType.isDefined(polarity)) {
      polarity = null; // just to make sure it is not undefined
    }

    this.mz = mz;
    if (signedCharge != null && signedCharge != 0) {
      this.signedCharge = signedCharge;
      this.absCharge = Math.abs(signedCharge);
      this.polarity = PolarityType.fromInt(signedCharge);
    } else if (absCharge != null && absCharge != 0) {
      this.absCharge = absCharge;
      this.polarity = polarity;
      if (polarity != null) {
        this.signedCharge = polarity.getSign() * absCharge;
      } else {
        this.signedCharge = null;
      }
    } else {
      this.signedCharge = null;
      this.absCharge = null;
      this.polarity = polarity;
    }
  }

  public IonSearchRow(double mz) {
    this(mz, null);
  }

  public IonSearchRow(double mz, @Nullable Integer signedCharge) {
    final PolarityType polarity = PolarityType.fromInt(signedCharge, null);
    final Integer absCharge = signedCharge == null ? null : Math.abs(signedCharge);
    this(mz, signedCharge, absCharge, polarity);
  }

  public IonSearchRow(@NotNull FeatureListRow a) {
    final PolarityType polarity = a.getRepresentativePolarity();
    Integer absCharge = a.getRowCharge();
    absCharge = switch (absCharge) {
      case null -> null;
      case 0 -> null;
      default -> Math.abs(absCharge);
    };

    Integer signedCharge = null;
    if (absCharge != null) {
      if (PolarityType.isDefined(polarity)) {
        signedCharge = absCharge * polarity.getSign();
      }
    }

    this(a.getAverageMZ(), signedCharge, absCharge, polarity);
  }
}
