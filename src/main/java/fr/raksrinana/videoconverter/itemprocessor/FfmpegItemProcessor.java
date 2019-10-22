package fr.raksrinana.videoconverter.itemprocessor;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Requires the Recycle module to be installed: https://www.powershellgallery.com/packages/Recycle/1.0.2
 */
public class FfmpegItemProcessor implements ItemProcessor{
	private static final Logger LOGGER = LoggerFactory.getLogger(FfmpegItemProcessor.class);
	private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();
	
	@Override
	public boolean create(CLIParameters params, FFmpegProbeResult probeResult, FFmpegStream stream, Path inputHost, Path outputHost, Path batchHost, Path batchClient){
		final var filename = outputHost.getFileName().toString();
		final var cut = filename.lastIndexOf(".");
		outputHost = outputHost.getParent().resolve((cut >= 0 ? filename.substring(0, cut) : filename) + ".mp4");
		final var duration = Duration.ofSeconds((long) probeResult.format.duration);
		final var durationStr = String.format("%dh%dm%s", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
		LOGGER.info("Converting {} ({}) to {}", inputHost, durationStr, outputHost);
		if(!outputHost.getParent().toFile().exists()){
			outputHost.getParent().toFile().mkdirs();
		}
		try{
			final var lock = locks.computeIfAbsent(outputHost.getFileName().toString(), name -> new Object());
			synchronized(lock){
				final var tempFile = params.getTempDirectory().resolve(outputHost.getFileName());
				LOGGER.debug("Will convert to temp file {}", tempFile);
				final var ffmpeg = new FFmpeg(params.getFfmpegPath());
				final var ffmpegOptions = ffmpeg.builder().addInput(probeResult).overrideOutputFiles(false).addOutput(tempFile.toAbsolutePath().normalize().toString()).setAudioBitRate(128000).setAudioCodec("aac").setVideoCodec("libx265").setPreset("medium").setConstantRateFactor(23d).setVideoMovFlags("use_metadata_tags").addExtraArgs("-map_metadata", "0").done();
				ffmpeg.run(ffmpegOptions, progress -> LOGGER.info("{} - {} / {} frames - {} fps - {} / {}", filename, progress.frame, probeResult.getStreams().stream().mapToLong(s -> s.nb_frames).max().orElse(0), progress.fps, Duration.ofNanos(progress.out_time_ns), durationStr));
				if(tempFile.toFile().exists()){
					Files.move(tempFile, outputHost);
					final var baseAttributes = Files.getFileAttributeView(inputHost, BasicFileAttributeView.class).readAttributes();
					final var attributes = Files.getFileAttributeView(outputHost, BasicFileAttributeView.class);
					attributes.setTimes(baseAttributes.lastModifiedTime(), baseAttributes.lastAccessTime(), baseAttributes.creationTime());
					var trashed = false;
					if(Desktop.isDesktopSupported()){
						final var desktop = Desktop.getDesktop();
						if(desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)){
							if(trashed = desktop.moveToTrash(inputHost.toFile())){
								LOGGER.info("Moved input file {} to trash", inputHost);
							}
						}
					}
					if(!trashed && Files.deleteIfExists(inputHost)){
						LOGGER.info("Deleted input file {}", inputHost);
					}
					LOGGER.info("Converted {} to {}", inputHost, outputHost);
					return true;
				}
				else{
					LOGGER.warn("Output file {} not found, something went wrong", outputHost);
					return false;
				}
			}
		}
		catch(IOException e){
			LOGGER.error("Failed to run ffmpeg", e);
		}
		return false;
	}
}
