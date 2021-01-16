package fr.raksrinana.mediaconverter;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import fr.raksrinana.mediaconverter.utils.CLIParameters;
import fr.raksrinana.mediaconverter.utils.Storage;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

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
			
			Storage storage = new Storage(parameters.getDatabasePath());
			
			Supplier<FFmpeg> ffmpegSupplier = () -> FFmpeg.atPath(parameters.getFfmpegPath());
			Supplier<FFprobe> ffprobeSupplier = () -> FFprobe.atPath(parameters.getFfprobePath());
			
			var tempDirectory = Files.createTempDirectory("VideoConverter");
			var executor = Executors.newFixedThreadPool(3);
			
			var fileProcessor = new FileProcessor(executor, storage, ffmpegSupplier, ffprobeSupplier, tempDirectory, parameters.getInput(), parameters.getOutput());
			Files.walkFileTree(parameters.getInput(), fileProcessor);
			executor.shutdown();
		}
		catch(Exception e){
			log.error("Failed to start", e);
		}
	}
}
