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
import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessorTask;
import fr.rakambda.mediaconverter.progress.ConversionProgressExecutor;
import fr.rakambda.mediaconverter.progress.ConverterProgressBarGenerator;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import fr.rakambda.mediaconverter.progress.ReuseProgressBarSupplier;
import fr.rakambda.mediaconverter.utils.CLIParameters;
import fr.rakambda.mediaconverter.utils.Continue;
import fr.rakambda.mediaconverter.utils.PausableThreadPoolExecutor;
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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
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
		var aContinue = new Continue();
		try(var converterExecutor = ConversionProgressExecutor.of(new PausableThreadPoolExecutor(parameters.getThreadCount(), aContinue));
				var scanningProgressBar = new ProgressBarBuilder()
						.setTaskName("Scanning")
						.setUnit("File", 1)
						.hideEta()
						.showSpeed()
						.build();
				var converterProgressBarSupplier = new ReuseProgressBarSupplier(progressBarGenerator);
				var consoleHandler = new ConsoleHandler(aContinue)){
			tempPaths = new ArrayList<>(Configuration.loadConfiguration(parameters.getConfiguration())
					.stream()
					.flatMap(config -> config.getConversions().stream())
					.filter(Conversion::isEnabled)
					.parallel()
					.map(conv -> {
						try{
							return Main.convert(
									conv,
									ffmpegSupplier,
									ffprobeSupplier,
									converterExecutor,
									scanningProgressBar,
									converterProgressBarSupplier,
									parameters.isDryRun(),
									parameters.getFfmpegThreadCount(),
									consoleHandler
							);
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
	private static Path convert(
			@NotNull Conversion conversion,
			@NotNull Supplier<FFmpeg> ffmpegSupplier,
			@NotNull Supplier<FFprobe> ffprobeSupplier,
			@NotNull ExecutorService converterExecutor,
			@NotNull ProgressBar scanningProgressBar,
			@NotNull ProgressBarSupplier converterProgressBarSupplier,
			boolean dryRun,
			@Nullable Integer ffmpegThreads,
			@NotNull ConsoleHandler consoleHandler
	) throws IOException{
		var tempDirectory = conversion.createTempDirectory();
		try{
			if(Objects.isNull(conversion.getInput()) || !Files.exists(conversion.getInput())){
				throw new IllegalArgumentException("Input path " + conversion.getInput().toAbsolutePath() + " doesn't exists");
			}
			if(Objects.isNull(conversion.getOutput()) || !Files.exists(conversion.getOutput())){
				throw new IllegalArgumentException("Output path " + conversion.getOutput().toAbsolutePath() + " doesn't exists");
			}
			
			var converters = new ConcurrentLinkedDeque<MediaProcessorTask>();
			consoleHandler.registerTasks(converters);
			
			try(var storage = conversion.getStorage();
					var virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()){
				consoleHandler.registerExecutor(virtualThreadExecutor);
				
				Consumer<MediaProcessorTask> converterRunner = converter -> {
					converters.add(converter);
					converter.execute(converterExecutor, dryRun);
				};
				Consumer<FileProber.ProbeResult> fileProcessor = probeResult -> new FileProcessor(probeResult, ffmpegSupplier, tempDirectory, conversion.getInput(), conversion.getOutput(), scanningProgressBar, converterProgressBarSupplier, conversion.isDeleteInput(), ffmpegThreads, converterRunner).run();
				Consumer<FileProber.ProbeResult> fileProberFilter = probeResult -> new FileProberFilter(probeResult, scanningProgressBar, conversion.getFilters(), fileProcessor).run();
				Consumer<Path> fileProber = file -> new FileProber(file, scanningProgressBar, storage, ffprobeSupplier, conversion.getProcessors(), fileProberFilter).run();
				Consumer<Path> fileFilter = file -> virtualThreadExecutor.execute(new FileFilter(file, scanningProgressBar, storage, conversion.getExtensions(), fileProber));
				
				var fileScanner = new FileScanner(scanningProgressBar, storage, conversion.getAbsoluteExcluded(), fileFilter);
				
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					virtualThreadExecutor.shutdownNow();
					converterExecutor.shutdownNow();
					
					converters.forEach(MediaProcessorTask::cancel);
				}));
				
				Files.walkFileTree(conversion.getInput(), fileScanner);
			}
		}
		catch(Exception e){
			log.error("Failed to convert files", e);
		}
		return tempDirectory;
	}
}
