package fr.mrcraftcod.videonormalizer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import fr.mrcraftcod.videonormalizer.utils.CLIParameters;
import fr.mrcraftcod.videonormalizer.utils.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collector;

public class Main{
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws InterruptedException, ClassNotFoundException{
		final var parameters = new CLIParameters();
		try{
			JCommander.newBuilder().addObject(parameters).build().parse(args);
		}
		catch(final ParameterException e){
			LOGGER.error("Failed to parse arguments", e);
			e.usage();
			return;
		}
		
		try{
			if(!(parameters.getInputClient().toFile().exists())){
				throw new IllegalArgumentException("Input client path " + parameters.getInputClient().toAbsolutePath().toString() + " doesn't exists");
			}
			try(Configuration conf = new Configuration(parameters.getConfigPath().toFile())){
				final var result = BatchProcessor.process(conf, parameters, parameters.getInputHost().normalize().toAbsolutePath(), parameters.getOutputHost().normalize().toAbsolutePath(), parameters.getBatchHost().normalize().toAbsolutePath(), parameters.getInputClient().normalize().toAbsolutePath(), parameters.getBatchClient().normalize().toAbsolutePath()).parallel().map(BatchProcessor::process).collect(Collector.of(BatchProcessorResult::newEmpty, BatchProcessorResult::add, BatchProcessorResult::add));
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
}
