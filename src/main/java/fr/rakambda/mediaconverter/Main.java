package fr.rakambda.mediaconverter;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import fr.rakambda.mediaconverter.config.Configuration;
import fr.rakambda.mediaconverter.config.Conversion;
import fr.rakambda.mediaconverter.ffmpeg.CustomFFmpeg;
import fr.rakambda.mediaconverter.file.FileFilter;
import fr.rakambda.mediaconverter.file.FileProber;
import fr.rakambda.mediaconverter.file.FileProberFilter;
import fr.rakambda.mediaconverter.file.FileProcessor;
import fr.rakambda.mediaconverter.file.FileScanner;
import fr.rakambda.mediaconverter.progress.ConversionProgressExecutor;
import fr.rakambda.mediaconverter.progress.ConverterProgressBarGenerator;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import fr.rakambda.mediaconverter.progress.ReuseProgressBarSupplier;
import fr.rakambda.mediaconverter.utils.CLIParameters;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import picocli.CommandLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
			Path ffmpegPath = Optional.ofNullable(parameters.getFfmpegPath())
					.map(p -> p.resolve("ffmpeg"))
					.orElse(Paths.get("ffmpeg"));
			FFmpeg ffmpeg = new CustomFFmpeg(ffmpegPath, parameters.getAffinityMask());
			if(Objects.nonNull(parameters.getFfmpegThreadCount())){
				ffmpeg = ffmpeg.addArguments("-threads", Integer.toString(parameters.getFfmpegThreadCount()));
			}
			return ffmpeg;
		};
		Supplier<FFprobe> ffprobeSupplier = () -> FFprobe.atPath(parameters.getFfprobePath());
		List<Path> tempPaths;
		
		var progressBarGenerator = new ConverterProgressBarGenerator();
		try(var converterExecutor = ConversionProgressExecutor.of(Executors.newFixedThreadPool(parameters.getThreadCount()));
				var scanningProgressBar = new ProgressBarBuilder().setTaskName("Scanning").setUnit("File", 1).build();
				var converterProgressBarSupplier = new ReuseProgressBarSupplier(progressBarGenerator);
				var consoleHandler = new ConsoleHandler()){
			tempPaths = new ArrayList<>(Configuration.loadConfiguration(parameters.getConfiguration())
					.stream()
					.flatMap(config -> config.getConversions().stream())
					.filter(Conversion::isEnabled)
					.parallel()
					.map(conv -> {
						try{
							return Main.convert(conv, ffmpegSupplier, ffprobeSupplier, converterExecutor, scanningProgressBar, converterProgressBarSupplier, parameters.isDryRun(), parameters.getFfmpegThreadCount(), consoleHandler);
						}
						catch(IOException e){
							log.error("Failed to perform conversion", e);
							return null;
						}
					})
					.filter(Objects::nonNull)
					.toList());
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
	private static Path convert(@NotNull Conversion conversion, @NotNull Supplier<FFmpeg> ffmpegSupplier, @NotNull Supplier<FFprobe> ffprobeSupplier, @NotNull ExecutorService converterExecutor, @NotNull ProgressBar scanningProgressBar, @NotNull ProgressBarSupplier converterProgressBarSupplier, boolean dryRun, @Nullable Integer ffmpegThreads, @NotNull ConsoleHandler consoleHandler) throws IOException{
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
				var fileFilter = new FileFilter(scanningProgressBar, storage, fileScanner.getOutputQueue(), conversion.getExtensions());
				var fileProber = new FileProber(scanningProgressBar, storage, fileFilter.getOutputQueue(), ffprobeSupplier, conversion.getProcessors());
				var fileProberFilter = new FileProberFilter(scanningProgressBar, fileProber.getOutputQueue(), conversion.getFilters());
				var fileProcessor = new FileProcessor(converterExecutor, ffmpegSupplier, tempDirectory, conversion.getInput(), conversion.getOutput(), fileProberFilter.getOutputQueue(), scanningProgressBar, converterProgressBarSupplier, conversion.isDeleteInput(), ffmpegThreads, dryRun);
				
				consoleHandler.add(fileScanner);
				consoleHandler.add(fileFilter);
				consoleHandler.add(fileProber);
				consoleHandler.add(fileProberFilter);
				consoleHandler.add(fileProcessor);
				
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					fileScanner.close();
					fileFilter.close();
					fileProber.close();
					fileProberFilter.close();
					fileProcessor.close();
					
					converterExecutor.shutdownNow();
					fileProcessor.cancel();
				}));
				
				es.submit(fileProcessor);
				es.submit(fileProberFilter);
				es.submit(fileProber);
				es.submit(fileFilter);
				Files.walkFileTree(conversion.getInput(), fileScanner);
				fileScanner.close();
				fileFilter.close();
				fileProber.close();
				fileProberFilter.close();
				fileProcessor.close();
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
