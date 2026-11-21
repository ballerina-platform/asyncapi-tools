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

import io.ballerina.asyncapi.cmd.AsyncApiCmdToolException.Kind;
import io.ballerina.asyncapi.cmd.CmdContext.FileReport;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects what a generator writes in a temporary directory, then copies it to the output location through the
 * fail-fast pipeline of the Ballerina CLI Tool Design Guide (FF-4): plan every file, check every destination can be
 * written, ask once before replacing files, stop there on a dry run, and only then write. Nothing reaches the
 * output location until the whole run is known to be possible, and the generators don't need to know about it.
 */
final class OutputStage implements AutoCloseable {

    /** What happens to one generated file. */
    enum Action {
        /** The file does not exist yet. */
        CREATE("created"),
        /** The file exists and differs. */
        OVERWRITE("overwritten"),
        /** An existing file the tool updates, keeping what the user wrote in it (FF-3). */
        MERGE("merged"),
        /** The file exists with exactly the generated content, so it is not written. */
        UNCHANGED("unchanged");

        private final String label;

        Action(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }

        boolean replaces() {
            return this == OVERWRITE || this == MERGE;
        }
    }

    /**
     * One generated file and what will happen to it.
     *
     * @param action       what happens to the file
     * @param relativePath the file's path relative to the output location
     * @param target       where the file goes in the output location
     */
    record PlannedFile(Action action, Path relativePath, Path target) {

        /** The path relative to the output location, with forward slashes. */
        String name() {
            return relativePath.toString().replace('\\', '/');
        }
    }

    private final Path destination;
    private final Path staging;
    private final CmdOptions opts;
    private final CmdContext ctx;
    private final Set<Path> merged = new HashSet<>();
    private String createdModule;

    private OutputStage(Path destination, Path staging, CmdOptions opts, CmdContext ctx) {
        this.destination = destination;
        this.staging = staging;
        this.opts = opts;
        this.ctx = ctx;
    }

    /**
     * Opens a stage for an output location. Nothing is created at the location until {@link #commit}.
     *
     * @param destination the real output directory
     * @param opts        the options carrying {@code --force} and {@code --dry-run}
     * @param ctx         where the result is printed
     * @return the stage, to be closed when done
     * @throws IOException if the temporary directory cannot be created
     */
    static OutputStage open(Path destination, CmdOptions opts, CmdContext ctx) throws IOException {
        return new OutputStage(destination, Files.createTempDirectory("asyncapi-"), opts, ctx);
    }

    /**
     * The directory the generator should write into.
     *
     * @return the temporary directory
     */
    Path stagingDir() {
        return staging;
    }

    /**
     * Marks a staged file as an update of an existing file that keeps the user's own content in it.
     *
     * @param relative the file's path relative to the output location
     */
    void markMerged(Path relative) {
        merged.add(relative);
    }

    /**
     * Records that the output location is a module the run creates, so the result says so (MOD-2).
     *
     * @param module the module name
     */
    void createsModule(String module) {
        this.createdModule = module;
    }

    /**
     * Copies everything the generator wrote to the output location, after every check has passed, and prints the
     * result. A dry run runs the checks, never prompts, and prints what it would write.
     *
     * @throws AsyncApiCmdToolException if a destination can't be written, or files would be replaced and the user
     *                                  declined or could not be asked
     * @throws IOException              if a file can't be copied
     */
    void commit() throws IOException {
        List<PlannedFile> plan = plan();
        checkWritable(plan);
        List<PlannedFile> replaced = plan.stream().filter(file -> file.action().replaces()).toList();
        if (!replaced.isEmpty() && !opts.isForce() && !opts.isDryRun()) {
            confirmOverwrite(replaced);
        }
        if (!opts.isDryRun()) {
            List<PlannedFile> written = new ArrayList<>();
            for (PlannedFile file : plan) {
                if (file.action() != Action.UNCHANGED) {
                    write(file, written);
                    written.add(file);
                }
            }
        }
        ctx.printFiles(plan.stream().map(file -> new FileReport(displayPath(file), file.action().label())).toList(),
                opts.isDryRun(), createdModule);
    }

    // FF-5 and FF-6: one prompt for the whole list, and no prompt at all without a terminal.
    private void confirmOverwrite(List<PlannedFile> replaced) {
        List<String> names = replaced.stream().map(OutputStage::displayPath).toList();
        String message = replaced.size() == 1
                ? names.get(0) + " already exists"
                : replaced.size() + " files already exist: " + String.join(", ", names);
        if (ctx.canPrompt() && ctx.confirmOverwrite(names)) {
            return;
        }
        throw AsyncApiCmdToolException.of(Kind.OVERWRITE_REFUSED, message)
                .withSuggestion("Re-run with --force to overwrite.")
                .withCommand(opts.getCommandLine() + " --force");
    }

    // FF-4 phase 5: every destination, and the folders it needs, must be writable before anything is written.
    private void checkWritable(List<PlannedFile> plan) throws IOException {
        for (PlannedFile file : plan) {
            if (file.action() == Action.UNCHANGED) {
                continue;
            }
            Path target = writePath(file.target());
            if (Files.exists(target) && !Files.isWritable(target)) {
                throw unwritable(displayPath(file), "the file is read-only");
            }
            Path folder = target.getParent();
            while (folder != null && !Files.exists(folder)) {
                folder = folder.getParent();
            }
            if (folder != null && (!Files.isDirectory(folder) || !Files.isWritable(folder))) {
                throw unwritable(displayPath(file), Files.isDirectory(folder)
                        ? "'" + folder + "' is read-only" : "'" + folder + "' is a file, not a folder");
            }
        }
    }

    private static AsyncApiCmdToolException unwritable(String file, String reason) {
        return AsyncApiCmdToolException.of(Kind.RUNTIME, "cannot write " + file + ": " + reason)
                .withSuggestion("Make the output location writable, or pass another one with --output.");
    }

    // A failure here happens after every check passed, so it is an I/O problem; it says which files were written.
    private void write(PlannedFile file, List<PlannedFile> written) throws IOException {
        try {
            Path target = writePath(file.target());
            Files.createDirectories(target.getParent());
            Files.copy(staging.resolve(file.relativePath()), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (!written.isEmpty()) {
                ctx.printWarning(String.format(ErrorMessages.WARN_WRITTEN_BEFORE_FAILURE,
                        written.stream().map(PlannedFile::name).collect(Collectors.joining(", "))));
            }
            throw e;
        }
    }

    // A symbolic link is written through, so the file it points to gets the content, not the link itself.
    private static Path writePath(Path target) throws IOException {
        return Files.isSymbolicLink(target)
                ? target.resolveSibling(Files.readSymbolicLink(target)).normalize()
                : target;
    }

    private List<PlannedFile> plan() throws IOException {
        List<Path> generated;
        try (Stream<Path> files = Files.walk(staging)) {
            generated = files.filter(Files::isRegularFile).map(staging::relativize)
                    .sorted(Comparator.comparing(path -> path.toString().replace('\\', '/')))
                    .collect(Collectors.toList());
        }
        List<PlannedFile> plan = new ArrayList<>();
        for (Path relative : generated) {
            Path target = destination.resolve(relative);
            plan.add(new PlannedFile(actionFor(relative, target), relative, target));
        }
        return plan;
    }

    private Action actionFor(Path relative, Path target) throws IOException {
        Path existing = writePath(target);
        if (Files.isDirectory(existing)) {
            throw AsyncApiCmdToolException.of(Kind.RUNTIME, String.format(ErrorMessages.TARGET_IS_DIRECTORY,
                    existing)).withSuggestion("Move the folder away, or pass another output location with --output.");
        }
        if (!Files.exists(existing)) {
            return Action.CREATE;
        }
        if (Files.mismatch(staging.resolve(relative), existing) == -1) {
            return Action.UNCHANGED;
        }
        return merged.contains(relative) ? Action.MERGE : Action.OVERWRITE;
    }

    // A path can only be shown relative to the current directory when both are on the same drive (Windows).
    private static String displayPath(PlannedFile file) {
        Path currentDir = Path.of("").toAbsolutePath();
        Path absolute = file.target().toAbsolutePath();
        Path shown = currentDir.getRoot().equals(absolute.getRoot()) ? currentDir.relativize(absolute) : absolute;
        return shown.toString().replace('\\', '/');
    }

    @Override
    public void close() {
        try (Stream<Path> files = Files.walk(staging)) {
            files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException e) {
            // the staging directory is temporary, so leaving it behind is harmless
        }
    }
}
