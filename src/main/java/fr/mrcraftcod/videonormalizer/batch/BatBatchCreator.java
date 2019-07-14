package fr.mrcraftcod.videonormalizer.batch;

import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;

public class BatBatchCreator implements BatchCreator{
	private static final Logger LOGGER = LoggerFactory.getLogger(BatBatchCreator.class);
	
	@Override
	public boolean create(FFmpegProbeResult probeResult, FFmpegStream stream, Path inputHost, Path outputHost, Path batchHost, Path batchClient){
		final var duration = Duration.ofSeconds((long) probeResult.format.duration);
		final var batFilename = String.format("%dh%dm%ds %s %s %s %f.bat", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart(), inputHost.getParent().getFileName().toString(), inputHost.getFileName().toString(), stream.codec_name, stream.avg_frame_rate.doubleValue());
		final var batHostPath = batchHost.resolve(batFilename);
		final var batClientPath = batchClient.resolve(batFilename);
		if(!batClientPath.getParent().toFile().exists())
			batClientPath.getParent().toFile().mkdirs();
		if(batClientPath.toFile().exists())
			return false;
		try(final var pw = new PrintWriter(batClientPath.toFile())){
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
