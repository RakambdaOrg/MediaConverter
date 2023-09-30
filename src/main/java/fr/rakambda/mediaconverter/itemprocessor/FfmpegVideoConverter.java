package fr.rakambda.mediaconverter.itemprocessor;

import com.github.kokorin.jaffree.ffmpeg.BaseOutput;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResultFuture;
import com.github.kokorin.jaffree.ffmpeg.Output;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import fr.rakambda.mediaconverter.progress.ConverterProgressBarNotifier;
import fr.rakambda.mediaconverter.progress.ProgressBarHandle;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
public abstract class FfmpegVideoConverter extends ConverterRunnable{
	private final FFmpeg ffmpeg;
	private final FFprobeResult probeResult;
	private final ProgressBarSupplier converterProgressBarSupplier;
	
	private ProgressBarHandle progressBar;
	private FFmpegResultFuture ffmpegResult;
	
	public FfmpegVideoConverter(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput){
		super(input, output, temporary, deleteInput);
		this.ffmpeg = ffmpeg;
		this.probeResult = probeResult;
		this.converterProgressBarSupplier = converterProgressBarSupplier;
	}
	
	@Override
	protected Future<?> convert(@NonNull ExecutorService executorService) throws InterruptedException{
		var filename = getOutput().getFileName().toString();
		
		var duration = Optional.ofNullable(probeResult.getFormat())
				.map(Format::getDuration)
				.map(Float::longValue)
				.map(Duration::ofSeconds)
				.orElse(Duration.ZERO);
		var frameCount = probeResult.getStreams().stream()
				.map(Stream::getNbFrames)
				.filter(Objects::nonNull)
				.mapToInt(i -> i)
				.max()
				.orElse(0);
		
		log.debug("Converting {} ({}) to {}", getInput(), duration, getOutput());
		
		progressBar = converterProgressBarSupplier.get();
		var progressListener = new ConverterProgressBarNotifier(filename, frameCount, duration, progressBar.getProgressBar());
		
		log.debug("Will convert to temp file {}", getTemporary());
		
		ffmpegResult = ffmpeg.addInput(UrlInput.fromPath(getInput()))
				.addOutput(buildOutput(UrlOutput.toPath(getTemporary())))
				.setOverwriteOutput(false)
				.setProgressListener(progressListener)
				.executeAsync(executorService);
		
		return ffmpegResult
				.toCompletableFuture()
				.exceptionally(t -> null)
				.thenAccept(r -> close());
	}
	
	protected abstract Output buildOutput(BaseOutput<?> output);
	
	@Override
	public void cancel(){
		if(Objects.nonNull(ffmpegResult) && !ffmpegResult.isCancelled() && !ffmpegResult.isDone()){
			ffmpegResult.forceStop();
		}
	}
	
	@Override
	public void close(){
		if(Objects.nonNull(progressBar)){
			progressBar.close();
		}
	}
}
