package fr.rakambda.mediaconverter.itemprocessor;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import fr.rakambda.mediaconverter.progress.ProgressBarNotifier;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.extern.log4j.Log4j2;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class AacConverter extends ConverterRunnable{
	private final FFmpeg ffmpeg;
	private final FFprobeResult probeResult;
	private final ProgressBarSupplier converterProgressBarSupplier;
	
	public AacConverter(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary, ProgressBarSupplier converterProgressBarSupplier){
		super(input, output, temporary);
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
			var progressListener = new ProgressBarNotifier(filename, frameCount, duration, progressBar.getProgressBar());
			
			log.debug("Will convert to temp file {}", getTemporary());
			
			ffmpeg.addInput(UrlInput.fromPath(getInput()))
					.addOutput(UrlOutput.toPath(getTemporary())
							.setCodec(StreamType.AUDIO, "aac")
							.addArguments("-b:a", "192k")
					)
					.setOverwriteOutput(false)
					.setProgressListener(progressListener)
					.execute();
		}
	}
}
