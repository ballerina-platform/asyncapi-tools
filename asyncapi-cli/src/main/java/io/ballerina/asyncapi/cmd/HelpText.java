/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.asyncapi.cmd;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Prints the help in the layout of section 9 of the Ballerina CLI Tool Design Guide: a one-line description,
 * the usage, the options in two columns (required ones first, then alphabetical), and examples. The options come
 * from the flag definitions on {@link BaseCmd}, so the help can't drift from what the command accepts.
 */
final class HelpText {

    static final String DESCRIPTION =
            "Generate Ballerina code from AsyncAPI definitions, or definitions from code";
    static final int MAX_WIDTH = 80;
    private static final String[][] EXAMPLES = {
            {"Generate a WebSocket client into the package in the current directory",
                    "bal asyncapi chat.yaml"},
            {"Generate a webhook trigger into the package at ./github",
                    "bal asyncapi github.yaml -o ./github"},
            {"List the files that would be written, writing nothing",
                    "bal asyncapi chat.yaml --dry-run"},
            {"Generate a JSON definition from the WebSocket services of a package",
                    "bal asyncapi ./chat-service --spec-format json"},
    };
    private static final String VERSION_RESOURCE = "/asyncapi-tool.properties";

    private HelpText() {
    }

    /**
     * Renders the help.
     *
     * @param spec the command's parsed flag definitions
     * @return the help text, without a trailing newline
     */
    static String render(CommandSpec spec) {
        List<String[]> options = new ArrayList<>();
        List<OptionSpec> visible = new ArrayList<>(spec.options().stream().filter(option -> !option.hidden())
                .toList());
        visible.sort(Comparator.comparing((OptionSpec option) -> !option.required())
                .thenComparing(option -> option.longestName().replaceFirst("^-+", "")));
        for (OptionSpec option : visible) {
            options.add(new String[]{names(option), String.join(" ", option.description())});
        }
        int width = options.stream().mapToInt(option -> option[0].length()).max().orElse(0) + 2;
        StringBuilder help = new StringBuilder();
        help.append(DESCRIPTION).append("\n\n");
        help.append(CmdContext.SYNOPSIS).append("\n\n");
        help.append("Options:\n");
        for (String[] option : options) {
            help.append(String.format(Locale.ROOT, "%-" + width + "s%s", option[0], option[1])).append('\n');
        }
        help.append("\nExamples:\n");
        for (int i = 0; i < EXAMPLES.length; i++) {
            help.append("  # ").append(EXAMPLES[i][0]).append('\n');
            help.append("  ").append(EXAMPLES[i][1]).append('\n');
            if (i < EXAMPLES.length - 1) {
                help.append('\n');
            }
        }
        return help.toString().stripTrailing();
    }

    // "  -o, --output <dir>" for an option with a short form, "      --force" for one without
    private static String names(OptionSpec option) {
        String shortName = null;
        String longName = null;
        for (String name : option.names()) {
            if (name.startsWith("--")) {
                longName = name;
            } else {
                shortName = name;
            }
        }
        String label = option.typeInfo().isBoolean() ? "" : " " + option.paramLabel();
        return (shortName == null ? "      " : "  " + shortName + ", ") + longName + label;
    }

    /**
     * Returns the version of the tool this command is part of.
     *
     * @return the version, or {@code unknown} if the build did not record it
     */
    static String toolVersion() {
        try (InputStream in = HelpText.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }
}
