package fr.rakambda.mediaconverter.itemprocessor;

import com.github.kokorin.jaffree.ffmpeg.BaseOutput;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.FFmpegResultFuture;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import fr.rakambda.mediaconverter.ffmpeg.CustomFFmpeg;
import fr.rakambda.mediaconverter.progress.ConverterProgressBarNotifier;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import org.jspecify.annotations.NonNull;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Log4j2
public abstract class FfmpegVideoConverter extends ConverterRunnable{
	private final FFmpeg ffmpeg;
	private final FFprobeResult probeResult;
	private final ProgressBarSupplier converterProgressBarSupplier;
	private final Integer ffmpegThreads;
	
	private ConverterProgressBarNotifier progressListener;
	private FFmpegResultFuture ffmpegResult;
	private boolean cancelled;
	
	public FfmpegVideoConverter(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput, @Nullable Integer ffmpegThreads){
		super(input, output, temporary, deleteInput);
		this.ffmpeg = ffmpeg;
		this.probeResult = probeResult;
		this.converterProgressBarSupplier = converterProgressBarSupplier;
		this.ffmpegThreads = ffmpegThreads;
		this.cancelled = false;
	}
	
	protected abstract BaseOutput<?> buildOutput(BaseOutput<?> output);
	
	@Override
	protected Future<?> convert(@NonNull ExecutorService executorService, boolean dryRun){
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
		progressListener = new ConverterProgressBarNotifier(filename, frameCount, duration, converterProgressBarSupplier);
		
		log.debug("Will convert to temp file {}", getTemporary());
		
		var output = buildOutput(UrlOutput.toPath(getTemporary()));
		if(Objects.nonNull(ffmpegThreads) && ffmpegThreads >= 0){
			output.addArguments("-threads", Integer.toString(ffmpegThreads));
		}
		
		var ffmpegAction = ffmpeg.addInput(UrlInput.fromPath(getInput()))
				.addOutput(output)
				.setOverwriteOutput(false)
				.setProgressListener(progressListener);
		
		if(cancelled){
			return executorService.submit(() -> {});
		}
		if(dryRun){
			return executorService.submit(() -> {
				if(ffmpeg instanceof CustomFFmpeg customFFmpeg){
					log.info("Dry run: would have run ffmpeg with args `{}`", customFFmpeg.buildArguments().stream().map("\"%s\""::formatted).collect(Collectors.joining(" ")));
				}
				else{
					log.info("Dry run: would have run ffmpeg for {}", getInput());
				}
			});
		}
		
		ffmpegResult = ffmpegAction.executeAsync(executorService);
		
		return ffmpegResult
				.toCompletableFuture()
				.exceptionally(t -> null)
				.thenAccept(r -> close());
	}
	
	@Override
	public void cancel(){
		cancelled = true;
		if(Objects.nonNull(ffmpegResult) && !ffmpegResult.isCancelled() && !ffmpegResult.isDone()){
			ffmpegResult.forceStop();
		}
	}
	
	@Override
	public void close(){
		if(Objects.nonNull(progressListener)){
			progressListener.close();
		}
	}
}
