package fr.raksrinana.videoconverter.itemprocessor;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import java.nio.file.Path;

public interface ItemProcessor{
	boolean create(CLIParameters params, FFmpegProbeResult probeResult, FFmpegStream stream, Path inputHost, Path outputHost, Path batchHost, Path batchClient);
}
