package fr.raksrinana.videoconverter;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import fr.raksrinana.videoconverter.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collector;

@Slf4j
public class Main {
    public static void main(String[] args) {
        final var parameters = new CLIParameters();
        var cli = new CommandLine(parameters);
        cli.registerConverter(Path.class, Paths::get);
        cli.setUnmatchedArgumentsAllowed(true);
        try {
            cli.parseArgs(args);
        } catch (final CommandLine.ParameterException e) {
            log.error("Failed to parse arguments", e);
            cli.usage(System.out);
            return;
        }

        try {
            if (!Files.exists(parameters.getInputClient())) {
                throw new IllegalArgumentException("Input client path " + parameters.getInputClient().toAbsolutePath().toString() + " doesn't exists");
            }
            try (Configuration conf = new Configuration(parameters.getDatabasePath())) {
                var processStream = BatchProcessor.process(conf, parameters, parameters.getInputHost().normalize().toAbsolutePath(), parameters.getOutputHost().normalize().toAbsolutePath(), parameters.getBatchHost().normalize().toAbsolutePath(), parameters.getInputClient().normalize().toAbsolutePath(), parameters.getBatchClient().normalize().toAbsolutePath()).map(BatchProcessor::process);
                if (parameters.isRunningParallel()) {
                    processStream = processStream.parallel();
                }
                final var result = processStream.collect(Collector.of(BatchProcessorResult::newEmpty, BatchProcessorResult::add, BatchProcessorResult::add));
                log.info("Created {} batch files (handled {} files, scanned {} files)", result.getCreated(), result.getHandled(), result.getScanned());
            } catch (Exception e) {
                log.error("Error running", e);
            }
        } catch (Exception e) {
            log.error("Failed to start", e);
        }
    }
}
