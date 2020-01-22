package fr.raksrinana.videoconverter.itemprocessor;

import fr.raksrinana.videoconverter.utils.CLIParameters;
import lombok.NonNull;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegStream;
import java.nio.file.Path;

public interface ItemProcessor{
	boolean create(@NonNull CLIParameters params, @NonNull FFmpegProbeResult probeResult, @NonNull FFmpegStream stream, @NonNull Path inputHost, @NonNull Path outputHost, @NonNull Path batchHost, @NonNull Path batchClient);
}
