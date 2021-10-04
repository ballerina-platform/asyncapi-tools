package io.ballerina.asyncapi.cmd;

import io.ballerina.asyncapi.codegenerator.application.CodeGenerator;
import io.ballerina.asyncapi.codegenerator.configuration.BallerinaAsyncApiException;
import io.ballerina.cli.BLauncherCmd;
import picocli.CommandLine;

import java.util.List;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(
        name = "asyncapi",
        description = "Generates Ballerina service/client for AsyncAPI contract and AsyncAPI contract for Ballerina" +
                "Service."
)
public class AsyncApiCmd implements BLauncherCmd {
    private static final String CMD_NAME = "asyncapi";
    private PrintStream outStream;
    private boolean exitWhenFinish;
    private Path executionPath = Paths.get(System.getProperty("user.dir"));

    @CommandLine.Option(names = {"-h", "--help"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"-i", "--input"}, description = "Generating the client and service both files")
    private boolean inputPath;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Location of the generated Ballerina service, " +
            "client and model files.")
    private String outputPath;

    @CommandLine.Parameters
    private List<String> argList;

    public AsyncApiCmd() {
        this.outStream = System.err;
        this.exitWhenFinish = true;
    }

    public AsyncApiCmd(PrintStream outStream, Path executionDir) {
        new AsyncApiCmd(outStream, executionDir, true);
    }

    public AsyncApiCmd(PrintStream outStream, Path executionDir, boolean exitWhenFinish) {
        this.outStream = outStream;
        this.executionPath = executionDir;
        this.exitWhenFinish = exitWhenFinish;
    }

    @Override
    public void execute() {

        if (helpFlag) {
            String commandUsageInfo = BLauncherCmd.getCommandUsageInfo(CMD_NAME);
            outStream.println(commandUsageInfo);
            return;
        }

        if (inputPath) {
            if (argList == null) {
                outStream.println("Missing Input");
                exitError(this.exitWhenFinish);
                return;
            }
            String fileName = argList.get(0);
            var codeGenerator = new CodeGenerator(fileName, (outputPath == null) ? String.valueOf(executionPath) : outputPath );
            try {
                codeGenerator.generate();
            } catch (BallerinaAsyncApiException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void printLongDesc(StringBuilder stringBuilder) {

    }

    @Override
    public void printUsage(StringBuilder stringBuilder) {

    }

    @Override
    public void setParentCmdParser(picocli.CommandLine commandLine) {

    }

    /**
     * Exit with error code 1.
     *
     * @param exit Whether to exit or not.
     */
    private static void exitError(boolean exit) {
        if (exit) {
            Runtime.getRuntime().exit(1);
        }
    }
}
