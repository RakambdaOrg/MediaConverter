package fr.mrcraftcod.videonormalizer.batch;

import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import java.nio.file.Path;

public interface BatchCreator{
	boolean create(FFmpegProbeResult probeResult, FFmpegStream stream, Path inputHost, Path outputHost, Path batchHost, Path batchClient);
}
