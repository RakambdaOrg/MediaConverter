package fr.raksrinana.mediaconverter;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import fr.raksrinana.mediaconverter.config.Configuration;
import fr.raksrinana.mediaconverter.config.Conversion;
import fr.raksrinana.mediaconverter.utils.CLIParameters;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Log4j2
public class Main{
	public static void main(String[] args){
		var parameters = new CLIParameters();
		var cli = new CommandLine(parameters);
		cli.registerConverter(Path.class, Paths::get);
		cli.setUnmatchedArgumentsAllowed(true);
		try{
			cli.parseArgs(args);
		}
		catch(CommandLine.ParameterException e){
			log.error("Failed to parse arguments", e);
			cli.usage(System.out);
			return;
		}
		
		Supplier<FFmpeg> ffmpegSupplier = () -> {
			var ffmpeg = FFmpeg.atPath(parameters.getFfmpegPath());
			if(Objects.nonNull(parameters.getFfmpegThreadCount())){
				ffmpeg = ffmpeg.addArguments("-threads", Integer.toString(parameters.getFfmpegThreadCount()));
			}
			return ffmpeg;
		};
		Supplier<FFprobe> ffprobeSupplier = () -> FFprobe.atPath(parameters.getFfprobePath());
		
		var executor = Executors.newFixedThreadPool(parameters.getThreadCount());
		
		Configuration.loadConfiguration(parameters.getConfiguration()).ifPresentOrElse(
				configuration -> configuration.getConversions().forEach(conv -> Main.convert(conv, ffmpegSupplier, ffprobeSupplier, executor)),
				() -> log.error("Failed to load configuration"));
		
		executor.shutdown();
		try{
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		}
		catch(InterruptedException e){
			log.error("Error while waiting for jobs to finish", e);
		}
	}
	
	private static void convert(@NotNull Conversion conversion, @NotNull Supplier<FFmpeg> ffmpegSupplier, @NotNull Supplier<FFprobe> ffprobeSupplier, @NotNull ExecutorService executor){
		try{
			if(Objects.isNull(conversion.getInput()) || !Files.exists(conversion.getInput())){
				throw new IllegalArgumentException("Input path " + conversion.getInput().toAbsolutePath() + " doesn't exists");
			}
			if(Objects.isNull(conversion.getOutput()) || !Files.exists(conversion.getOutput())){
				throw new IllegalArgumentException("Output path " + conversion.getOutput().toAbsolutePath() + " doesn't exists");
			}
			
			try(var storage = conversion.getStorage()){
				var tempDirectory = conversion.createTempDirectory();
				try(var proxyExecutor = ProgressExecutor.of(executor)){
					try(var fileProcessor = new FileProcessor(proxyExecutor,
							storage,
							ffmpegSupplier,
							ffprobeSupplier,
							tempDirectory,
							conversion.getInput(),
							conversion.getOutput(),
							conversion.getAbsoluteExcluded(),
							conversion.getProcessors(),
							conversion.getExtensions())){
						Files.walkFileTree(conversion.getInput(), fileProcessor);
					}
				}
				
				Files.deleteIfExists(tempDirectory);
			}
		}
		catch(Exception e){
			log.error("Failed to convert files", e);
		}
	}
}
