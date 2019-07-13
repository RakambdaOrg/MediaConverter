package fr.mrcraftcod.videonormalizer.batch;

import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import java.nio.file.Path;

public interface BatchCreator{
	boolean create(FFmpegProbeResult probeResult, Path inputHost, Path outputHost, Path batchHost, Path batchClient);
}
