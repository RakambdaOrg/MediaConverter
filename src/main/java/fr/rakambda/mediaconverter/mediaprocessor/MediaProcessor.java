package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.nio.file.Path;

public interface MediaProcessor{
	boolean canHandle(@Nullable FFprobeResult probeResult, @NonNull Path file);

	@NonNull
	MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput, @Nullable Integer ffmpegThreads);

	@NonNull
	String getDesiredExtension();
}
