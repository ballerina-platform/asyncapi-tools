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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collects what a generator writes in a temporary directory and applies the overwrite rule when copying it to
 * the real output location, so every protocol and direction behaves the same without the generators knowing
 * about it. An existing file is left alone unless {@code --force} is given, and {@code --dry-run} reports the
 * plan without writing anything.
 */
final class OutputStage implements AutoCloseable {

    private static final String DRY_RUN_HEADER = "Dry run: no files were written.";
    private static final String DRY_RUN_LINE = "  %-10s %s";
    private static final String DRY_RUN_SKIP_NOTE = "  (already exists, use --force to overwrite)";
    private static final String DRY_RUN_SUMMARY = "%d to create, %d to overwrite, %d to skip, %d unchanged.";

    /** What happens to one generated file. */
    enum Action {
        /** The file does not exist yet. */
        CREATE,
        /** The file exists and differs, and {@code --force} was given. */
        OVERWRITE,
        /** The file exists and differs, and {@code --force} was not given. */
        SKIP,
        /** The file exists with exactly the generated content. */
        UNCHANGED;

        String label() {
            return name().toLowerCase(Locale.ROOT);
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
    private final boolean force;
    private final boolean dryRun;
    private final CmdContext ctx;

    private OutputStage(Path destination, Path staging, CmdOptions opts, CmdContext ctx) {
        this.destination = destination;
        this.staging = staging;
        this.force = opts.isForce();
        this.dryRun = opts.isDryRun();
        this.ctx = ctx;
    }

    /**
     * Opens a stage for an output location. Nothing is created at the location until {@link #commit}.
     *
     * @param destination the real output directory
     * @param opts        the options carrying {@code --force} and {@code --dry-run}
     * @param ctx         streams for reporting
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
     * Applies the overwrite rule to everything the generator wrote, then reports. A dry run only reports.
     *
     * @param successMessage printed when at least one file was written
     * @throws IOException if a file cannot be copied to the output location
     */
    void commit(String successMessage) throws IOException {
        List<PlannedFile> plan = plan();
        if (dryRun) {
            printDryRun(plan);
            return;
        }
        List<PlannedFile> written = new ArrayList<>();
        for (PlannedFile file : plan) {
            switch (file.action()) {
                case CREATE, OVERWRITE -> {
                    Files.createDirectories(file.target().getParent());
                    Files.copy(staging.resolve(file.relativePath()), file.target(),
                            StandardCopyOption.REPLACE_EXISTING);
                    written.add(file);
                }
                case SKIP -> ctx.printWarning(String.format(ErrorMessages.WARN_FILE_SKIPPED, displayPath(file)));
                case UNCHANGED -> {
                }
            }
        }
        report(plan, written, successMessage);
    }

    private void report(List<PlannedFile> plan, List<PlannedFile> written, String successMessage) {
        if (!written.isEmpty()) {
            ctx.printInfo(successMessage);
            ctx.printInfo(ErrorMessages.FILES_WRITTEN);
            written.forEach(file -> ctx.printInfo("-- " + file.name()));
        } else if (!plan.isEmpty() && plan.stream().allMatch(file -> file.action() == Action.UNCHANGED)) {
            ctx.printInfo(ErrorMessages.FILES_UP_TO_DATE);
        } else {
            ctx.printInfo(ErrorMessages.NO_FILES_WRITTEN);
        }
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
        if (!Files.exists(target)) {
            return Action.CREATE;
        }
        if (Files.mismatch(staging.resolve(relative), target) == -1) {
            return Action.UNCHANGED;
        }
        return force ? Action.OVERWRITE : Action.SKIP;
    }

    private void printDryRun(List<PlannedFile> plan) {
        ctx.printInfo(DRY_RUN_HEADER);
        ctx.printInfo("");
        for (PlannedFile file : plan) {
            String line = String.format(DRY_RUN_LINE, file.action().label(), displayPath(file));
            ctx.printInfo(file.action() == Action.SKIP ? line + DRY_RUN_SKIP_NOTE : line);
        }
        ctx.printInfo("");
        ctx.printInfo(String.format(DRY_RUN_SUMMARY, count(plan, Action.CREATE), count(plan, Action.OVERWRITE),
                count(plan, Action.SKIP), count(plan, Action.UNCHANGED)));
    }

    private static long count(List<PlannedFile> plan, Action action) {
        return plan.stream().filter(file -> file.action() == action).count();
    }

    // A path can only be shown relative to the current directory when both are on the same drive (Windows).
    private static Path displayPath(PlannedFile file) {
        Path currentDir = Path.of("").toAbsolutePath();
        Path absolute = file.target().toAbsolutePath();
        return currentDir.getRoot().equals(absolute.getRoot()) ? currentDir.relativize(absolute) : absolute;
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
