package fr.raksrinana.mediaconverter.itemprocessor;

import fr.raksrinana.mediaconverter.utils.ProgressBarNotifier;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.Duration;

/**
 * Requires the Recycle module to be installed: https://www.powershellgallery.com/packages/Recycle/1.0.2
 */
@Slf4j
public class HevcConverter implements Runnable{
	private final FFmpeg ffmpeg;
	private final FFmpegProbeResult probeResult;
	private final Path input;
	private final Path output;
	private final Path temporary;
	
	public HevcConverter(FFmpeg ffmpeg, FFmpegProbeResult probeResult, Path input, Path output, Path temporary){
		this.ffmpeg = ffmpeg;
		this.probeResult = probeResult;
		this.input = input;
		this.output = output;
		this.temporary = temporary;
	}
	
	@Override
	public void run(){
		var filename = output.getFileName().toString();
		
		var duration = Duration.ofSeconds((long) probeResult.format.duration);
		var durationStr = String.format("%dh%dm%s", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
		
		log.info("Converting {} ({}) to {}", input, durationStr, output);
		try{
			log.debug("Will convert to temp file {}", temporary);
			var ffmpegOptions = ffmpeg.builder()
					.addInput(probeResult)
					.overrideOutputFiles(false)
					.addOutput(temporary.toAbsolutePath().normalize().toString())
					.setAudioBitRate(128000)
					.setAudioCodec("aac")
					.setVideoCodec("libx265")
					.setPreset("medium")
					.setConstantRateFactor(23d)
					.setVideoMovFlags("use_metadata_tags")
					.addExtraArgs("-map_metadata", "0")
					.addExtraArgs("-max_muxing_queue_size", "512")
					.done();
			
			var frameCount = probeResult.getStreams().stream().mapToLong(s -> s.nb_frames).max().orElse(0);
			ffmpeg.run(ffmpegOptions, new ProgressBarNotifier(filename, frameCount, durationStr));
			
			if(Files.exists(temporary)){
				Files.move(temporary, output);
				
				copyFileAttributes();
				trashInput();
				
				log.info("Converted {} to {}", input, output);
			}
			else{
				log.warn("Output file {} not found, something went wrong", output);
			}
		}
		catch(IOException e){
			log.error("Failed to run ffmpeg on {}", input, e);
		}
	}
	
	private void copyFileAttributes() throws IOException{
		var baseAttributes = Files.getFileAttributeView(input, BasicFileAttributeView.class).readAttributes();
		var attributes = Files.getFileAttributeView(output, BasicFileAttributeView.class);
		attributes.setTimes(baseAttributes.lastModifiedTime(), baseAttributes.lastAccessTime(), baseAttributes.creationTime());
	}
	
	private void trashInput() throws IOException{
		if(Desktop.isDesktopSupported()){
			var desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)){
				if(desktop.moveToTrash(input.toFile())){
					log.info("Moved input file {} to trash", input);
					return;
				}
			}
		}
		
		if(Files.deleteIfExists(input)){
			log.info("Deleted input file {}", input);
		}
	}
}
