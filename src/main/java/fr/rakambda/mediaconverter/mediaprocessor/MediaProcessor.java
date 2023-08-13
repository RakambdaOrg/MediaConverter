package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;

public interface MediaProcessor{
	boolean canHandle(@Nullable FFprobeResult probeResult, @NotNull Path file);

	@NotNull
	MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NotNull Path input, @NotNull Path output, @NotNull Path temporary, @NotNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput);

	@NotNull
	String getDesiredExtension();
}
