package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.ffmpeg.AacConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class Mp3ToAacMediaProcessor implements MediaProcessor {
    private static final List<String> CODECS = List.of("mp3");

    @Override
    public boolean canHandle(@Nullable FFprobeResult probeResult, @NonNull Path file) {
        if (Objects.isNull(probeResult)) {
            return false;
        }
        return probeResult.getStreams().size() == 1
                && probeResult.getStreams().stream()
                .allMatch(stream -> CODECS.contains(stream.getCodecName()));
    }

    @Override
    @NonNull
    public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput, @Nullable Integer ffmpegThreads){
	    return new AacConverter(ffmpeg, probeResult, input, output, temporary, converterProgressBarSupplier, deleteInput, ffmpegThreads);
    }

    @Override
    @NonNull
    public String getDesiredExtension() {
        return "m4a";
    }
}
