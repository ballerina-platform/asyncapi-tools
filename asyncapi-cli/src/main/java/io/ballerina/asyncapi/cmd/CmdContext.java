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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ballerina.asyncapi.cmd.AsyncApiCmdToolException.InputDiagnostic;
import io.ballerina.asyncapi.core.SpecWarning;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Everything one run prints, and how it ends. Results go to stdout and everything else to stderr, as text or as
 * one JSON object per line (section 7 of the Ballerina CLI Tool Design Guide).
 */
final class CmdContext {

    /** How the run prints its reports. */
    enum Format {
        TEXT, JSON;

        static Optional<Format> parse(String value) {
            for (Format format : values()) {
                if (format.name().equalsIgnoreCase(value.trim())) {
                    return Optional.of(format);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * One file in the result of a run.
     *
     * @param path   the file, relative to the current directory where possible, with forward slashes
     * @param action {@code created}, {@code overwritten}, {@code merged} or {@code unchanged}
     */
    record FileReport(String path, String action) {
    }

    static final String PREFIX = "bal asyncapi: ";
    static final String SYNOPSIS = "Usage: bal asyncapi [OPTIONS] <input>";
    static final String HELP_POINTER = "Try 'bal asyncapi --help' for more information.";
    private static final String HELP_SUGGESTION = "Run 'bal asyncapi --help' for the usage.";
    private static final String UNCHANGED = "unchanged";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int STDIN = 0;
    private static final int STDOUT = 1;
    private static final int STDERR = 2;

    private final PrintStream infoStream;
    private final PrintStream errStream;
    private final BufferedReader input;
    private final boolean interactive;
    private final Path executionPath;
    private boolean exitWhenFinish;
    private Format format;
    private int exitCode = ExitCodes.SUCCESS;
    private Path specPath;
    private SpecLocator specLocator;

    /**
     * Creates a context that prints text and never prompts, for tests.
     *
     * @param infoStream     stream for results
     * @param errStream      stream for warnings, prompts and errors
     * @param exitWhenFinish {@code false} to keep the JVM running when the command fails
     */
    CmdContext(PrintStream infoStream, PrintStream errStream, boolean exitWhenFinish) {
        this(infoStream, errStream, new ByteArrayInputStream(new byte[0]), exitWhenFinish, Format.TEXT, false);
    }

    /**
     * Creates a context.
     *
     * @param infoStream     stream for results
     * @param errStream      stream for warnings, prompts and errors
     * @param input          where an answer to the overwrite prompt is read from
     * @param exitWhenFinish {@code false} to keep the JVM running when the command fails
     * @param format         how reports are printed until {@link #setFormat} changes it
     * @param interactive    whether the user can answer a prompt
     */
    CmdContext(PrintStream infoStream, PrintStream errStream, InputStream input, boolean exitWhenFinish,
               Format format, boolean interactive) {
        this.infoStream = infoStream;
        this.errStream = errStream;
        this.input = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        this.exitWhenFinish = exitWhenFinish;
        this.format = format;
        this.interactive = interactive;
        this.executionPath = Path.of(System.getProperty("user.dir"));
    }

    /**
     * Creates the context for a real run: text on a terminal, JSON otherwise.
     *
     * @return the context
     */
    static CmdContext forTerminal() {
        boolean console = consoleIsTerminal();
        // OUT-2: the format follows stdout alone; FF-6: a prompt needs stdin and stderr
        boolean textOutput = isTerminal(STDOUT).orElse(console);
        boolean canPrompt = isTerminal(STDIN).orElse(console) && isTerminal(STDERR).orElse(console);
        return new CmdContext(System.out, System.err, System.in, true, textOutput ? Format.TEXT : Format.JSON,
                canPrompt);
    }

    // Linux shows where each stream goes; a terminal is a /dev/pts or /dev/tty device. Elsewhere this is unknown.
    private static Optional<Boolean> isTerminal(int fd) {
        try {
            String target = Files.readSymbolicLink(Path.of("/proc/self/fd/" + fd)).toString();
            return Optional.of(target.startsWith("/dev/pts/") || target.startsWith("/dev/tty"));
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            return Optional.empty();
        }
    }

    // Java can only tell whether stdin and stdout are both a terminal, which stands in where /proc is missing.
    private static boolean consoleIsTerminal() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            // From Java 22 a console exists even when redirected, and isTerminal tells the two apart.
            return (Boolean) Console.class.getMethod("isTerminal").invoke(console);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }

    PrintStream infoStream() {
        return infoStream;
    }

    PrintStream errStream() {
        return errStream;
    }

    Path executionPath() {
        return executionPath;
    }

    Format format() {
        return format;
    }

    void setFormat(Format format) {
        this.format = format;
    }

    void setExitWhenFinish(boolean exitWhenFinish) {
        this.exitWhenFinish = exitWhenFinish;
    }

    /**
     * Returns the code the run ended with, {@link ExitCodes#SUCCESS} unless {@link #fail} was called.
     *
     * @return the exit code
     */
    int exitCode() {
        return exitCode;
    }

    /**
     * Prints text that is the whole result, such as the help, to stdout.
     *
     * @param text the text
     */
    void printInfo(String text) {
        infoStream.println(text);
    }

    /**
     * Prints a warning about the command itself, such as an option that does not apply.
     *
     * @param message what the user should know, in one line
     */
    void printWarning(String message) {
        printWarning(message, null);
    }

    /**
     * Prints a warning about the command itself, with how to avoid it.
     *
     * @param message    what the user should know, in one line
     * @param suggestion how to avoid it, or {@code null}
     */
    void printWarning(String message, String suggestion) {
        if (format == Format.JSON) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("severity", "warning");
            report.put("message", message);
            putIfPresent(report, "suggestion", suggestion);
            errStream.println(json(report));
            return;
        }
        errStream.println(PREFIX + "warning: " + message);
        if (suggestion != null) {
            printIndented(suggestion);
        }
    }

    /**
     * Sets the definition that spec warnings point into, so they can name its line and column.
     *
     * @param spec the definition, as the user gave it
     */
    void setSpecSource(Path spec) {
        this.specPath = spec;
        this.specLocator = null;
    }

    /**
     * Prints a problem in the definition that the tool worked around, at its place in the file.
     *
     * @param warning the problem and how to fix the definition
     */
    void printSpecWarning(SpecWarning warning) {
        String path = specPath == null ? null : specPath.toString().replace('\\', '/');
        int[] position = position(warning.location());
        String suggestion = warning.suggestion() == null || warning.suggestion().isBlank()
                ? null : warning.suggestion();
        if (format == Format.JSON) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("severity", "warning");
            report.put("message", warning.message());
            putIfPresent(report, "suggestion", suggestion);
            putIfPresent(report, "path", path);
            if (position != null) {
                report.put("line", position[0]);
                report.put("column", position[1]);
            }
            putIfPresent(report, "location", warning.location() == null || warning.location().isEmpty()
                    ? null : warning.location());
            errStream.println(json(report));
            return;
        }
        String where = path == null ? PREFIX : position == null ? path + ": "
                : path + ":" + position[0] + ":" + position[1] + ": ";
        errStream.println(where + "warning: " + warning.message());
        if (suggestion != null) {
            printIndented(suggestion);
        }
    }

    private int[] position(String pointer) {
        if (specPath == null || pointer == null || pointer.isEmpty()) {
            return null;
        }
        if (specLocator == null) {
            specLocator = SpecLocator.of(specPath);
        }
        return specLocator.locate(pointer).orElse(null);
    }

    /**
     * Prints the run's result: the files it wrote, or on a dry run the files it would write.
     *
     * @param files         the files with what happened to each
     * @param dryRun        whether nothing was written
     * @param createdModule the module the run creates, or {@code null}
     */
    void printFiles(List<FileReport> files, boolean dryRun, String createdModule) {
        if (format == Format.JSON) {
            Map<String, Object> result = new LinkedHashMap<>();
            if (dryRun) {
                result.put("dryRun", true);
            }
            if (createdModule != null) {
                Map<String, String> module = new LinkedHashMap<>();
                module.put("name", createdModule);
                module.put("action", "created");
                result.put("module", module);
            }
            List<Map<String, String>> entries = new ArrayList<>();
            for (FileReport file : files) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("path", file.path());
                entry.put("action", file.action());
                entries.add(entry);
            }
            result.put("files", entries);
            infoStream.println(json(result));
            return;
        }
        List<String[]> lines = new ArrayList<>();
        if (createdModule != null) {
            lines.add(new String[]{label("created", dryRun), "module " + createdModule});
        }
        int changed = 0;
        for (FileReport file : files) {
            if (!UNCHANGED.equals(file.action())) {
                lines.add(new String[]{label(file.action(), dryRun), file.path()});
                changed++;
            }
        }
        int width = lines.stream().mapToInt(line -> line[0].length()).max().orElse(0);
        for (String[] line : lines) {
            infoStream.println(String.format(Locale.ROOT, "%-" + width + "s  %s", line[0], line[1]));
        }
        if (dryRun) {
            infoStream.println("Dry run: nothing was written.");
        } else if (changed > 0) {
            infoStream.println("Generated " + changed + (changed == 1 ? " file." : " files."));
        } else if (!files.isEmpty()) {
            infoStream.println("No changes: the files are already up to date.");
        } else {
            infoStream.println("No files were generated.");
        }
    }

    private static String label(String action, boolean dryRun) {
        if (!dryRun) {
            return action;
        }
        return "would " + switch (action) {
            case "created" -> "create";
            case "overwritten" -> "overwrite";
            case "merged" -> "merge";
            default -> action;
        };
    }

    /**
     * Tells whether the user can be asked before files are overwritten.
     *
     * @return true when both input and output are a terminal
     */
    boolean canPrompt() {
        return interactive;
    }

    /**
     * Lists the files that would be replaced and asks once whether to go ahead. No is the default.
     *
     * @param files the files, as shown to the user
     * @return true if the user answered yes
     */
    boolean confirmOverwrite(List<String> files) {
        files.forEach(file -> errStream.println("  " + file));
        errStream.print("Overwrite these file(s)? [y/N] ");
        errStream.flush();
        try {
            String answer = input.readLine();
            return answer != null && (answer.trim().equalsIgnoreCase("y") || answer.trim().equalsIgnoreCase("yes"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Prints the error that ends the run and exits with its code.
     *
     * @param error the error
     */
    void fail(AsyncApiCmdToolException error) {
        boolean usage = error.getKind() == AsyncApiCmdToolException.Kind.USAGE;
        if (format == Format.JSON) {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("severity", "error");
            report.put("kind", error.getKind().label());
            report.put("message", error.getMessage());
            putIfPresent(report, "suggestion", error.getSuggestion() == null && usage
                    ? HELP_SUGGESTION : error.getSuggestion());
            putIfPresent(report, "command", error.getCommand());
            if (!error.getDiagnostics().isEmpty()) {
                List<Map<String, Object>> diagnostics = new ArrayList<>();
                for (InputDiagnostic diagnostic : error.getDiagnostics()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("path", diagnostic.path());
                    if (diagnostic.line() > 0) {
                        entry.put("line", diagnostic.line());
                        entry.put("column", diagnostic.column());
                    }
                    entry.put("severity", diagnostic.severity());
                    entry.put("message", diagnostic.message());
                    diagnostics.add(entry);
                }
                report.put("diagnostics", diagnostics);
            }
            errStream.println(json(report));
        } else {
            errStream.println(PREFIX + "error: " + error.getMessage());
            for (InputDiagnostic diagnostic : error.getDiagnostics()) {
                String where = diagnostic.line() > 0
                        ? diagnostic.path() + ":" + diagnostic.line() + ":" + diagnostic.column()
                        : diagnostic.path();
                errStream.println(where + ": " + diagnostic.severity() + ": " + diagnostic.message());
            }
            if (error.getSuggestion() != null) {
                printIndented(error.getSuggestion());
            }
            if (error.getCommand() != null) {
                errStream.println("  Try: " + error.getCommand());
            }
            if (usage) {
                errStream.println(SYNOPSIS);
                errStream.println(HELP_POINTER);
            }
        }
        exit(error.exitCode());
    }

    // A suggestion can hold an example over several lines, such as YAML to add; each keeps its own indentation
    private void printIndented(String text) {
        text.lines().forEach(line -> errStream.println("  " + line));
    }

    private void exit(int code) {
        exitCode = code;
        errStream.flush();
        infoStream.flush();
        if (exitWhenFinish) {
            Runtime.getRuntime().exit(code);
        }
    }

    private static void putIfPresent(Map<String, Object> report, String key, Object value) {
        if (value != null) {
            report.put(key, value);
        }
    }

    private static String json(Map<String, ?> report) {
        try {
            return JSON.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            // Only maps of strings, numbers and lists are written, which always serialise
            throw new IllegalStateException(e);
        }
    }
}
