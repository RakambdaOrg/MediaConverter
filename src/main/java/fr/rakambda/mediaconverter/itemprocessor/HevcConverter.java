package fr.rakambda.mediaconverter.itemprocessor;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import fr.rakambda.mediaconverter.progress.ConverterProgressBarNotifier;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class HevcConverter extends ConverterRunnable{
	private final FFmpeg ffmpeg;
	private final FFprobeResult probeResult;
	private final ProgressBarSupplier converterProgressBarSupplier;
	
	public HevcConverter(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput){
		super(input, output, temporary, deleteInput);
		this.ffmpeg = ffmpeg;
		this.probeResult = probeResult;
		this.converterProgressBarSupplier = converterProgressBarSupplier;
	}
	
	@Override
	protected void convert() throws InterruptedException{
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
		try(var progressBar = converterProgressBarSupplier.get()){
			var progressListener = new ConverterProgressBarNotifier(filename, frameCount, duration, progressBar.getProgressBar());
			
			log.debug("Will convert to temp file {}", getTemporary());
			ffmpeg.addInput(UrlInput.fromPath(getInput()))
					.addOutput(UrlOutput.toPath(getTemporary())
							.setCodec(StreamType.AUDIO, "aac")
							.addArguments("-b:a", "128000")
							.setCodec(StreamType.VIDEO, "libx265")
							.addArguments("-preset", "medium")
							.addArguments("-crf", "21")
							.addArguments("-vf", "format=yuv420p10le")
							.addArguments("-movflags", "use_metadata_tags")
							.addArguments("-map_metadata", "0")
							.addArguments("-map", "0")
							.addArguments("-map", "-0:d")
							.addArguments("-max_muxing_queue_size", "512")
					)
					.setOverwriteOutput(false)
					.setProgressListener(progressListener)
					.execute();
		}
	}
}
