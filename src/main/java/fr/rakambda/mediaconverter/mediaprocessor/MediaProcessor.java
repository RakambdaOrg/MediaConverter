package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import java.nio.file.Path;

public interface MediaProcessor{
	boolean canHandle(FFprobeResult probeResult);
	
	MediaProcessorTask createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary, ProgressBarSupplier converterProgressBarSupplier);
	
	String getDesiredExtension();
}
