package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.ffmpeg.Av1Converter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class VideoToAv1MediaProcessor implements MediaProcessor {
    private static final List<String> CODECS = List.of("h264", "vp9", "wmv3", "mpeg2video", "hevc", "h265");

    @Override
    public boolean canHandle(@Nullable FFprobeResult probeResult, @NonNull Path file) {
        if (Objects.isNull(probeResult)) {
            return false;
        }
        return probeResult.getStreams().stream().anyMatch(stream -> isWantedCodec(stream.getCodecName()));
    }

    private boolean isWantedCodec(@NonNull String codecName) {
        return CODECS.contains(codecName);
    }

    @Override
    @NonNull
    public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier progressBarSupplier, boolean deleteInput, @Nullable Integer ffmpegThreads){
	    return new Av1Converter(ffmpeg, probeResult, input, output, temporary, progressBarSupplier, deleteInput, ffmpegThreads);
    }

    @Override
    @NonNull
    public String getDesiredExtension() {
        return "mkv";
    }
}
