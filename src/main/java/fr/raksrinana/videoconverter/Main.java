package fr.raksrinana.videoconverter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import fr.raksrinana.videoconverter.itemprocessor.ItemProcessor;
import fr.raksrinana.videoconverter.utils.CLIParameters;
import fr.raksrinana.videoconverter.utils.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collector;

public class Main{
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args){
		final var parameters = new CLIParameters();
		try{
			JCommander.newBuilder().addObject(parameters).build().parse(args);
			if(Objects.isNull(parameters.getItemProcessor()) || !ItemProcessor.class.isAssignableFrom(parameters.getItemProcessor())){
				throw new ParameterException("Given item processor class doesn't implements ItemProcessor.");
			}
		}
		catch(final ParameterException e){
			LOGGER.error("Failed to parse arguments", e);
			e.usage();
			return;
		}
		try{
			final var lockFile = parameters.getDatabasePath().resolveSibling(parameters.getDatabasePath().getFileName().toString() + ".lock").normalize().toAbsolutePath();
			if(lockFile.toFile().exists()){
				LOGGER.error("Program is already running, lock file {} is present", lockFile.toFile());
				System.exit(1);
			}
			touch(lockFile.toFile());
			lockFile.toFile().deleteOnExit();
		}
		catch(Exception e){
			LOGGER.error("Failed to setup lock file", e);
			System.exit(1);
		}
		
		try{
			if(!(parameters.getInputClient().toFile().exists())){
				throw new IllegalArgumentException("Input client path " + parameters.getInputClient().toAbsolutePath().toString() + " doesn't exists");
			}
			try(Configuration conf = new Configuration(parameters.getDatabasePath())){
				var processStream = BatchProcessor.process(conf, parameters, parameters.getInputHost().normalize().toAbsolutePath(), parameters.getOutputHost().normalize().toAbsolutePath(), parameters.getBatchHost().normalize().toAbsolutePath(), parameters.getInputClient().normalize().toAbsolutePath(), parameters.getBatchClient().normalize().toAbsolutePath()).map(BatchProcessor::process);
				if(parameters.isRunningParallel()){
					processStream = processStream.parallel();
				}
				final var result = processStream.collect(Collector.of(BatchProcessorResult::newEmpty, BatchProcessorResult::add, BatchProcessorResult::add));
				LOGGER.info("Created {} batch files (handled {} files, scanned {} files)", result.getCreated(), result.getHandled(), result.getScanned());
			}
			catch(Exception e){
				LOGGER.error("Error running", e);
			}
		}
		catch(Exception e){
			LOGGER.error("Failed to start", e);
		}
	}
	
	private static void touch(final File file) throws IOException{
		if(!file.exists()){
			new FileOutputStream(file).close();
		}
	}
}
