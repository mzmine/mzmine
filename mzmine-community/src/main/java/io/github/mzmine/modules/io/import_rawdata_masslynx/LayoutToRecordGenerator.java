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

package io.github.mzmine.modules.io.import_rawdata_masslynx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility to dynamically generate a Java record class from a String
 * representation of a MemoryLayout.
 */
public class LayoutToRecordGenerator {

    // Regex to find the name of the struct, e.g., ".withName("ScanInfo")"
    private static final Pattern STRUCT_NAME_PATTERN = Pattern.compile("\\.withName\\(\"([a-zA-Z0-9_]+)\"\\)");

    // Regex to find individual field definitions.
    // It captures:
    // 1. The full type constant (e.g., "MassLynxLib.C_INT" or "ValueLayout.JAVA_FLOAT")
    // 2. The field name (e.g., "msLevel")
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "([a-zA-Z0-9_\\.]+)\\.withName\\(\"([a-zA-Z0-9_]+)\"\\)"
    );

    // Maps the suffix of a native type constant to a Java type.
    // This allows flexibility (e.g., both "C_INT" and "JAVA_INT" map to "int").
    private static final Map<String, String> NATIVE_TO_JAVA_TYPE_MAP = Map.ofEntries(
            Map.entry("C_INT", "int"),
            Map.entry("JAVA_INT", "int"),
            Map.entry("C_FLOAT", "float"),
            Map.entry("JAVA_FLOAT", "float"),
            Map.entry("C_LONG", "long"),
            Map.entry("JAVA_LONG", "long"),
            Map.entry("C_DOUBLE", "double"),
            Map.entry("JAVA_DOUBLE", "double"),
            Map.entry("C_SHORT", "short"),
            Map.entry("JAVA_SHORT", "short"),
            Map.entry("C_CHAR", "char"),
            Map.entry("JAVA_CHAR", "char"),
            Map.entry("C_BOOL", "boolean"),
            Map.entry("JAVA_BOOLEAN", "boolean")
    );

    /**
     * Generates a Java record source file from a layout definition string.
     *
     * @param layoutDefinition The string containing the MemoryLayout code.
     * @throws IOException If there is an error writing the file.
     * @throws IllegalStateException If the layout definition cannot be parsed.
     */
    public void generateRecordClass(String layoutDefinition) throws IOException {
        // 1. Parse the record name
        Matcher nameMatcher = STRUCT_NAME_PATTERN.matcher(layoutDefinition);
        String recordName;
        if (nameMatcher.find()) {
            recordName = nameMatcher.group(1);
        } else {
            throw new IllegalStateException("Could not find record name with .withName(\"...\") in the layout definition.");
        }

        System.out.println("Found record name: " + recordName);

        // 2. Parse the fields
        List<String> recordComponents = new ArrayList<>();
        Matcher fieldMatcher = FIELD_PATTERN.matcher(layoutDefinition);

        while (fieldMatcher.find()) {
            String nativeType = fieldMatcher.group(1); // e.g., "MassLynxLib.C_INT"
            String fieldName = fieldMatcher.group(2);  // e.g., "msLevel"

            // Get the last part of the native type (e.g., "C_INT")
            String typeSuffix = nativeType.substring(nativeType.lastIndexOf('.') + 1);

            String javaType = NATIVE_TO_JAVA_TYPE_MAP.get(typeSuffix);
            if (javaType == null) {
                System.err.println("Warning: Unmapped native type '" + typeSuffix + "'. Skipping field '" + fieldName + "'.");
                continue;
            }

            System.out.println("  -> Found field: " + javaType + " " + fieldName);
            recordComponents.add(javaType + " " + fieldName);
        }

        if (recordComponents.isEmpty()) {
            throw new IllegalStateException("No valid fields found in the layout definition.");
        }

        // 3. Assemble the Java source code
        String parameters = String.join(", ", recordComponents);
        String recordSourceCode = String.format(
            "/**%n" +
            " * This record was automatically generated by LayoutToRecordGenerator.%n" +
            " */%n" +
            "public record %s(%s) {}%n",
            recordName, parameters
        );

        System.out.println("\n--- Generated Java Code ---\n" + recordSourceCode);

        // 4. Write the source code to a .java file
        Path outputPath = Paths.get(recordName + ".java");
        Files.writeString(outputPath, recordSourceCode);

        System.out.println("\nSuccessfully created file: " + outputPath.toAbsolutePath());
    }

    public static void main(String[] args) {
        // The input string provided by the user
        String layoutString = """
            private static final GroupLayout $LAYOUT = MemoryLayout.structLayout(
                                                                          MassLynxLib.C_INT.withName("msLevel"), MassLynxLib.C_INT.withName("polarity"),
                                                                          MassLynxLib.C_INT.withName("driftScanCount"), MassLynxLib.C_INT.withName("isProfile"),
                                                                          MassLynxLib.C_FLOAT.withName("precursorMz"),
                                                                          MassLynxLib.C_FLOAT.withName("quadIsolationStart"),
                                                                          MassLynxLib.C_FLOAT.withName("quadIsolationEnd"),
                                                                          MassLynxLib.C_FLOAT.withName("collisionEnergy"), MassLynxLib.C_FLOAT.withName("rt"),
                                                                          MassLynxLib.C_FLOAT.withName("laserXPos"), MassLynxLib.C_FLOAT.withName("laserYPos"))
                                                                      .withName("ScanInfo");
            """;

        LayoutToRecordGenerator generator = new LayoutToRecordGenerator();
        try {
            generator.generateRecordClass(layoutString);
        } catch (Exception e) {
            System.err.println("Error generating record class: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
