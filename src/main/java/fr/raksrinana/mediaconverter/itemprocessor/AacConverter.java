package fr.raksrinana.mediaconverter.itemprocessor;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Format;
import com.github.kokorin.jaffree.ffprobe.Stream;
import fr.raksrinana.mediaconverter.utils.ProgressBarNotifier;
import lombok.extern.log4j.Log4j2;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class AacConverter extends ConverterRunnable{
	private final FFmpeg ffmpeg;
	private final FFprobeResult probeResult;
	private final Path temporary;
	
	public AacConverter(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary){
		super(input, output);
		this.ffmpeg = ffmpeg;
		this.probeResult = probeResult;
		this.temporary = temporary.toAbsolutePath().normalize();
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
		
		log.info("Converting {} ({}) to {}", getInput(), duration, getOutput());
		try(var progressListener = new ProgressBarNotifier(filename, frameCount, duration)){
			log.debug("Will convert to temp file {}", temporary);
			
			ffmpeg.addInput(UrlInput.fromPath(getInput()))
					.addOutput(UrlOutput.toPath(temporary)
							.setCodec(StreamType.AUDIO, "libfdk_aac")
							.addArguments("-vbr", "4")
					)
					.setOverwriteOutput(false)
					.setProgressListener(progressListener)
					.execute();
			
			if(Files.exists(temporary)){
				Files.move(temporary, getOutput());
				
				copyFileAttributes(getInput(), getOutput());
				trashFile(getInput());
				
				log.info("Converted {} to {}", getInput(), getOutput());
			}
			else{
				log.warn("Output file {} not found, something went wrong", getOutput());
			}
		}
		catch(IOException e){
			log.error("Failed to run ffmpeg on {}", getInput(), e);
		}
	}
}
