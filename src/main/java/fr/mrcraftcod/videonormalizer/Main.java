package fr.mrcraftcod.videonormalizer;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import fr.mrcraftcod.videonormalizer.utils.CLIParameters;
import fr.mrcraftcod.videonormalizer.utils.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Objects;

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
		Configuration conf = null;
		try{
			conf = new Configuration(new File(".", "videonormalizer.db"));
			final var result = new BatchProcessor(conf, parameters, parameters.getInputHost().normalize().toAbsolutePath(), parameters.getOutputHost().normalize().toAbsolutePath(), parameters.getBatchHost().normalize().toAbsolutePath(), parameters.getInputClient().normalize().toAbsolutePath(), parameters.getBatchClient().normalize().toAbsolutePath()).process();
			LOGGER.info("Created {} batch files (scanned {} files)", result.getCreated(), result.getScanned());
			
		}
		catch(Exception e){
			LOGGER.error("Failed to run", e);
		}
		finally{
			if(Objects.nonNull(conf)){
				conf.close();
			}
		}
	}
}
