package fr.rakambda.mediaconverter.file;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessorTask;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Log4j2
public class FileProcessor implements Runnable{
	private final FileProber.ProbeResult probeResult;
	private final Supplier<FFmpeg> ffmpegSupplier;
	private final Path tempDirectory;
	private final Path baseInput;
	private final Path baseOutput;
	private final ProgressBar progressBar;
	private final ProgressBarSupplier converterProgressBarSupplier;
	private final boolean deleteInput;
	private final Integer ffmpegThreads;
	private final Consumer<MediaProcessorTask> callback;
	
	public FileProcessor(@NonNull FileProber.ProbeResult probeResult,
			@NonNull Supplier<FFmpeg> ffmpegSupplier,
			@NonNull Path tempDirectory,
			@NonNull Path baseInput,
			@NonNull Path baseOutput,
			@NonNull ProgressBar progressBar,
			@NonNull ProgressBarSupplier converterProgressBarSupplier,
			boolean deleteInput,
			@Nullable Integer ffmpegThreads,
			@NonNull Consumer<MediaProcessorTask> callback
	){
		this.probeResult = probeResult;
		this.ffmpegSupplier = ffmpegSupplier;
		this.tempDirectory = tempDirectory;
		this.baseInput = baseInput;
		this.baseOutput = baseOutput;
		this.progressBar = progressBar;
		this.converterProgressBarSupplier = converterProgressBarSupplier;
		this.deleteInput = deleteInput;
		this.ffmpegThreads = ffmpegThreads;
		this.callback = callback;
	}
	
	@Override
	public void run(){
		processFile(probeResult);
		progressBar.step();
	}
	
	private void processFile(@NonNull FileProber.ProbeResult probeResult){
		var file = probeResult.file();
		var ffProbeResult = probeResult.fFprobeResult();
		var processor = probeResult.processor();
		var outfile = buildOutFile(file, processor.getDesiredExtension());
		
		if(!deleteInput && Files.exists(outfile)){
			log.warn("Skipping {}, output already exists at {}", file, outfile);
			return;
		}
		if(!Files.exists(outfile.getParent())){
			try{
				Files.createDirectories(outfile.getParent());
			}
			catch(IOException e){
				log.error("Failed to create output folder for {}", outfile, e);
				return;
			}
		}
		
		var task = processor.createConvertTask(
				ffmpegSupplier.get(),
				ffProbeResult,
				file,
				outfile,
				tempDirectory.resolve("%d-%d-%s".formatted(System.nanoTime(), file.hashCode(), outfile.getFileName())),
				converterProgressBarSupplier,
				deleteInput,
				ffmpegThreads
		);
		callback.accept(task);
	}
	
	private Path buildOutFile(@NonNull Path file, @Nullable String desiredExtension){
		var relative = baseInput.relativize(file);
		var outFile = baseOutput.resolve(relative);
		
		if(Objects.nonNull(desiredExtension)){
			var filename = outFile.getFileName().toString();
			var dotIndex = filename.lastIndexOf('.');
			if(dotIndex > 0){
				filename = filename.substring(0, dotIndex);
			}
			outFile = outFile.resolveSibling(filename + '.' + desiredExtension);
		}
		
		return outFile;
	}
}
