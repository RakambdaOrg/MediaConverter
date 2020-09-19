package fr.raksrinana.videoconverter.itemprocessor;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;

@Slf4j
public class BatItemProcessor implements ItemProcessor{
	@Override
	public boolean create(@NonNull CLIParameters params, @NonNull FFmpegProbeResult probeResult, @NonNull FFmpegStream stream, @NonNull Path inputHost, @NonNull Path outputHost, @NonNull Path batchHost, @NonNull Path batchClient){
		final var filename = outputHost.getFileName().toString();
		final var cut = filename.lastIndexOf(".");
		outputHost = outputHost.getParent().resolve((cut >= 0 ? filename.substring(0, cut) : filename) + ".mp4");
		final var duration = Duration.ofSeconds((long) probeResult.format.duration);
		final var batFilename = String.format("%dh%dm%ds %s %s %s %f.bat", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), inputHost.getParent().getFileName().toString(), inputHost.getFileName().toString(), stream.codec_name, stream.avg_frame_rate.doubleValue());
		final var batHostPath = batchHost.resolve(batFilename);
		final var batClientPath = batchClient.resolve(batFilename);
		if(!Files.exists(batchClient)){
			try{
				Files.createDirectories(batchClient);
			}
			catch(IOException e){
				log.error("Failed to create directory {}", batchClient, e);
			}
		}
		if(Files.exists(batClientPath)){
			return false;
		}
		final var lines = List.of(
				String.format("title %s", batFilename),
				String.format("mkdir \"%s\"", outputHost.getParent().toString()),
				String.format("ffmpeg -n -i \"%s\" -c:v libx265 -preset medium -crf 23 -c:a aac -b:a 128k -map_metadata 0 -map_metadata:s:v 0:s:v -map_metadata:s:0 0:s:a \"%s\"", inputHost.toString(), outputHost.toString()),
				String.format("if exist \"%s\" trash \"%s\"", outputHost.toString(), inputHost.toString()),
				String.format("if exist \"%s\" trash \"%s\"", batHostPath.toString(), batHostPath.toString()));
		try{
			Files.write(batClientPath, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		}
		catch(Exception e){
			log.error("Failed to write bat into {}", batClientPath, e);
			return false;
		}
		log.info("Wrote bat file for {}", inputHost);
		return true;
	}
}
