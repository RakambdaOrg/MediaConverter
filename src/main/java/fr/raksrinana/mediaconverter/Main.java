package fr.raksrinana.mediaconverter;

import fr.raksrinana.mediaconverter.utils.CLIParameters;
import fr.raksrinana.mediaconverter.utils.Storage;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import picocli.CommandLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

@Slf4j
public class Main{
	public static void main(String[] args){
		var parameters = new CLIParameters();
		var cli = new CommandLine(parameters);
		cli.registerConverter(Path.class, Paths::get);
		cli.setUnmatchedArgumentsAllowed(true);
		try{
			cli.parseArgs(args);
		}
		catch(final CommandLine.ParameterException e){
			log.error("Failed to parse arguments", e);
			cli.usage(System.out);
			return;
		}
		
		try{
			if(!Files.exists(parameters.getInput())){
				throw new IllegalArgumentException("Input path " + parameters.getInput().toAbsolutePath().toString() + " doesn't exists");
			}
			if(!Files.exists(parameters.getOutput())){
				throw new IllegalArgumentException("Output path " + parameters.getOutput().toAbsolutePath().toString() + " doesn't exists");
			}
			
			try(Storage storage = new Storage(parameters.getDatabasePath())){
				var tempDirectory = Files.createTempDirectory("VideoConverter");
				var ffmpeg = new FFmpeg(parameters.getFfmpegPath());
				var ffprobe = new FFprobe(parameters.getFfprobePath());
				var executor = Executors.newFixedThreadPool(3);
				
				var fileProcessor = new FileProcessor(executor, storage, ffmpeg, ffprobe, tempDirectory, parameters.getInput(), parameters.getOutput());
				Files.walkFileTree(parameters.getInput(), fileProcessor);
				executor.shutdown();
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
