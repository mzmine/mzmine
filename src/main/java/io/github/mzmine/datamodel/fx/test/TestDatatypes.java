/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.fx.test;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.CommentType;
import io.github.mzmine.datamodel.data.types.DetectionType;
import io.github.mzmine.datamodel.data.types.modifiers.StringParser;
import io.github.mzmine.datamodel.data.types.numbers.AreaType;
import io.github.mzmine.datamodel.data.types.numbers.HeightType;
import io.github.mzmine.datamodel.data.types.numbers.MZType;
import io.github.mzmine.datamodel.data.types.numbers.RTType;
import io.github.mzmine.util.color.ColorsFX;
import javafx.scene.paint.Color;

public class TestDatatypes {

  public static void main(String[] args) {
    System.out.println(CommentType.class.isAssignableFrom(StringParser.class));
    System.out.println(StringParser.class.isAssignableFrom(CommentType.class));
    System.out.println(CommentType.class.isNestmateOf(StringParser.class));
    System.out.println(StringParser.class.isNestmateOf(CommentType.class));

    testColor(Color.BLACK);
    testColor(Color.WHITE);
    testColor(Color.RED);
    testColor(Color.GREEN);
    testColor(Color.BLUE);
    testColor(Color.MAGENTA);


    ModularFeatureListRow data = new ModularFeatureListRow();

    System.out.println(data.getDetectionType().toString());
    System.out.println(data.getFormattedString(RTType.class).orElse("No RT"));

    data.set(MZType.class, (50d));
    data.set(RTType.class, (10f));
    data.set(DetectionType.class, (FeatureStatus.DETECTED));
    data.set(AreaType.class, (1.2E4f));

    System.out.println(data.getDetectionType().toString());
    System.out.println(data.getFormattedString(RTType.class).orElse("NONE"));
    System.out.println(data.getFormattedString(AreaType.class).orElse("NONE"));

    System.out.println("Should throw an error");
    data.set(HeightType.class, (50d));
  }

  private static void testColor(Color color) {
    System.out.println(ColorsFX.toHexString(color) + "   " + ColorsFX.toHexOpacityString(color));
  }

}
