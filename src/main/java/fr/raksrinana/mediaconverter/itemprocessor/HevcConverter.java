package fr.raksrinana.mediaconverter.itemprocessor;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import fr.raksrinana.mediaconverter.progress.ProgressBarNotifier;
import fr.raksrinana.mediaconverter.progress.ProgressBarSupplier;
import lombok.extern.log4j.Log4j2;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class HevcConverter extends ConverterRunnable{
	private final FFmpeg ffmpeg;
	private final FFprobeResult probeResult;
	private final Path temporary;
	private final ProgressBarSupplier converterProgressBarSupplier;
	
	public HevcConverter(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary, ProgressBarSupplier converterProgressBarSupplier){
		super(input, output);
		this.ffmpeg = ffmpeg;
		this.probeResult = probeResult;
		this.temporary = temporary.toAbsolutePath().normalize();
		this.converterProgressBarSupplier = converterProgressBarSupplier;
	}
	
	@Override
	protected Optional<Path> getTempPath(){
		return Optional.ofNullable(temporary);
	}
	
	@Override
	protected void convert(){
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
			
			log.debug("Will convert to temp file {}", temporary);
			ffmpeg.addInput(UrlInput.fromPath(getInput()))
					.addOutput(UrlOutput.toPath(temporary)
							.setCodec(StreamType.AUDIO, "aac")
							.addArguments("-b:a", "128000")
							.setCodec(StreamType.VIDEO, "libx265")
							.addArguments("-preset", "medium")
							.addArguments("-crf", "23")
							.addArguments("-movflags", "use_metadata_tags")
							.addArguments("-map_metadata", "0")
							.addArguments("-map", "0")
							.addArguments("-max_muxing_queue_size", "512")
					)
					.setOverwriteOutput(false)
					.setProgressListener(progressListener)
					.execute();
			
			if(Files.exists(temporary)){
				Files.move(temporary, getOutput());
				
				copyFileAttributes(getInput(), getOutput());
				trashFile(getInput());
				
				log.debug("Converted {} to {}", getInput(), getOutput());
			}
			else{
				log.warn("Output file {} not found, something went wrong", getOutput());
			}
		}
		catch(IOException | InterruptedException e){
			log.error("Failed to run ffmpeg on {}", getInput(), e);
		}
	}
}
