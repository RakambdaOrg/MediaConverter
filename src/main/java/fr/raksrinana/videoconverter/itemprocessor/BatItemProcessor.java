package fr.raksrinana.videoconverter.itemprocessor;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

public class BatItemProcessor implements ItemProcessor{
	private static final Logger LOGGER = LoggerFactory.getLogger(BatItemProcessor.class);
	
	@Override
	public boolean create(CLIParameters params, FFmpegProbeResult probeResult, FFmpegStream stream, Path inputHost, Path outputHost, Path batchHost, Path batchClient){
		final var filename = outputHost.getFileName().toString();
		final var cut = filename.lastIndexOf(".");
		outputHost = outputHost.getParent().resolve((cut >= 0 ? filename.substring(0, cut) : filename) + ".mp4");
		final var duration = Duration.ofSeconds((long) probeResult.format.duration);
		final var batFilename = String.format("%dh%dm%ds %s %s %s %f.bat", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), inputHost.getParent().getFileName().toString(), inputHost.getFileName().toString(), stream.codec_name, stream.avg_frame_rate.doubleValue());
		final var batHostPath = batchHost.resolve(batFilename);
		final var batClientPath = batchClient.resolve(batFilename);
		if(!batClientPath.getParent().toFile().exists()){
			batClientPath.getParent().toFile().mkdirs();
		}
		if(batClientPath.toFile().exists())
			return false;
		try(final var pw = new PrintWriter(new FileOutputStream(batClientPath.toFile()), false, StandardCharsets.UTF_8)){
			pw.printf("title %s\r\n", batFilename);
			pw.printf("mkdir \"%s\"\r\n", outputHost.getParent().toString());
			pw.printf("ffmpeg -n -i \"%s\" -c:v libx265 -preset medium -crf 23 -c:a aac -b:a 128k -map_metadata 0 -map_metadata:s:v 0:s:v -map_metadata:s:a 0:s:a \"%s\"\r\n", inputHost.toString(), outputHost.toString());
			pw.printf("if exist \"%s\" trash \"%s\"\r\n", outputHost.toString(), inputHost.toString());
			pw.printf("if exist \"%s\" trash \"%s\"\r\n", batHostPath.toString(), batHostPath.toString());
		}
		catch(FileNotFoundException e){
			LOGGER.error("Failed to write bat into {}", batClientPath, e);
			return false;
		}
		LOGGER.info("Wrote bat file for {}", inputHost);
		return true;
	}
}
