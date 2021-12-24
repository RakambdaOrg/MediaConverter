package fr.raksrinana.mediaconverter;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import fr.raksrinana.mediaconverter.config.Configuration;
import fr.raksrinana.mediaconverter.config.Conversion;
import fr.raksrinana.mediaconverter.file.FileFilter;
import fr.raksrinana.mediaconverter.file.FileProcessor;
import fr.raksrinana.mediaconverter.file.FileScanner;
import fr.raksrinana.mediaconverter.progress.ProgressBarSupplier;
import fr.raksrinana.mediaconverter.progress.ProgressExecutor;
import fr.raksrinana.mediaconverter.utils.CLIParameters;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

@Log4j2
public class Main{
	private static final Function<Integer, ProgressBar> PROGRESS_BAR_SUPPLIER = i -> new ProgressBarBuilder()
			.setTaskName("Converter " + (i + 1))
			.setUnit("frame", 1)
			.setSpeedUnit(ChronoUnit.SECONDS)
			.showSpeed()
			.build();
	
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
		List<Path> tempPaths = new ArrayList<>();
		
		try(var converterExecutor = ProgressExecutor.of(Executors.newFixedThreadPool(parameters.getThreadCount()));
				var scanningProgressBar = new ProgressBarBuilder().setTaskName("Scanning").setUnit("File", 1).build();
				var converterProgressBarSupplier = new ProgressBarSupplier(PROGRESS_BAR_SUPPLIER)){
			tempPaths.addAll(Configuration.loadConfiguration(parameters.getConfiguration())
					.stream()
					.flatMap(config -> config.getConversions().stream())
					.parallel()
					.map(conv -> {
						try{
							return Main.convert(conv, ffmpegSupplier, ffprobeSupplier, converterExecutor, scanningProgressBar, converterProgressBarSupplier);
						}
						catch(IOException e){
							log.error("Failed to perform conversion", e);
							return null;
						}
					})
					.filter(Objects::nonNull)
					.toList());
		}
		catch(InterruptedException e){
			log.error("Failed to process conversions", e);
		}
		
		tempPaths.forEach(path -> {
			try{
				Files.deleteIfExists(path);
			}
			catch(IOException e){
				log.error("Failed to delete temp directory", e);
			}
		});
	}
	
	@NotNull
	private static Path convert(@NotNull Conversion conversion, @NotNull Supplier<FFmpeg> ffmpegSupplier, @NotNull Supplier<FFprobe> ffprobeSupplier, @NotNull ExecutorService converterExecutor, @NotNull ProgressBar scanningProgressBar, ProgressBarSupplier converterProgressBarSupplier) throws IOException{
		var tempDirectory = conversion.createTempDirectory();
		try{
			if(Objects.isNull(conversion.getInput()) || !Files.exists(conversion.getInput())){
				throw new IllegalArgumentException("Input path " + conversion.getInput().toAbsolutePath() + " doesn't exists");
			}
			if(Objects.isNull(conversion.getOutput()) || !Files.exists(conversion.getOutput())){
				throw new IllegalArgumentException("Output path " + conversion.getOutput().toAbsolutePath() + " doesn't exists");
			}
			
			ExecutorService es = null;
			try(var storage = conversion.getStorage()){
				es = Executors.newCachedThreadPool();
				
				var fileScanner = new FileScanner(scanningProgressBar, storage, conversion.getAbsoluteExcluded());
				var fileFilter = new FileFilter(scanningProgressBar, storage, fileScanner.getQueue(), conversion.getExtensions());
				var fileProcessor = new FileProcessor(converterExecutor, storage, ffmpegSupplier, ffprobeSupplier, tempDirectory, conversion.getInput(), conversion.getOutput(), conversion.getProcessors(), fileFilter.getOutputQueue(), scanningProgressBar, converterProgressBarSupplier);
				
				es.submit(fileProcessor);
				es.submit(fileFilter);
				Files.walkFileTree(conversion.getInput(), fileScanner);
				fileFilter.shutdown();
				fileProcessor.shutdown();
			}
			finally{
				if(Objects.nonNull(es)){
					es.shutdownNow();
				}
			}
		}
		catch(Exception e){
			log.error("Failed to convert files", e);
		}
		return tempDirectory;
	}
}
