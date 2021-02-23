package fr.raksrinana.mediaconverter;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import fr.raksrinana.mediaconverter.storage.IStorage;
import fr.raksrinana.mediaconverter.storage.NoOpStorage;
import fr.raksrinana.mediaconverter.storage.Storage;
import fr.raksrinana.mediaconverter.utils.CLIParameters;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
				throw new IllegalArgumentException("Input path " + parameters.getInput().toAbsolutePath() + " doesn't exists");
			}
			if(!Files.exists(parameters.getOutput())){
				throw new IllegalArgumentException("Output path " + parameters.getOutput().toAbsolutePath() + " doesn't exists");
			}
			
			try(var storage = getStorage(parameters)){
				
				Supplier<FFmpeg> ffmpegSupplier = () -> FFmpeg.atPath(parameters.getFfmpegPath());
				Supplier<FFprobe> ffprobeSupplier = () -> FFprobe.atPath(parameters.getFfprobePath());
				
				var tempDirectory = parameters.createTempDirectory();
				var executor = Executors.newFixedThreadPool(3);
				
				var fileProcessor = new FileProcessor(executor, storage, ffmpegSupplier, ffprobeSupplier, tempDirectory, parameters.getInput(), parameters.getOutput());
				Files.walkFileTree(parameters.getInput(), fileProcessor);
				executor.shutdown();
				executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
				
				Files.deleteIfExists(tempDirectory);
			}
		}
		catch(Exception e){
			log.error("Failed to convert files", e);
		}
	}
	
	private static IStorage getStorage(CLIParameters parameters) throws SQLException, IOException{
		if(Objects.isNull(parameters.getDatabasePath())){
			return new NoOpStorage();
		}
		return new Storage(parameters.getDatabasePath());
	}
}
