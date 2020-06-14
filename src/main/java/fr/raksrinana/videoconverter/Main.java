package fr.raksrinana.videoconverter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import fr.raksrinana.videoconverter.itemprocessor.ItemProcessor;
import fr.raksrinana.videoconverter.utils.CLIParameters;
import fr.raksrinana.videoconverter.utils.Configuration;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;
import java.util.stream.Collector;

@Slf4j
public class Main{
	public static void main(String[] args){
		final var parameters = new CLIParameters();
		try{
			JCommander.newBuilder().addObject(parameters).build().parse(args);
			if(Objects.isNull(parameters.getItemProcessor()) || !ItemProcessor.class.isAssignableFrom(parameters.getItemProcessor())){
				throw new ParameterException("Given item processor class doesn't implements ItemProcessor.");
			}
		}
		catch(final ParameterException e){
			log.error("Failed to parse arguments", e);
			e.usage();
			return;
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
				log.info("Created {} batch files (handled {} files, scanned {} files)", result.getCreated(), result.getHandled(), result.getScanned());
			}
			catch(Exception e){
				log.error("Error running", e);
			}
		}
		catch(Exception e){
			log.error("Failed to start", e);
		}
	}
}
