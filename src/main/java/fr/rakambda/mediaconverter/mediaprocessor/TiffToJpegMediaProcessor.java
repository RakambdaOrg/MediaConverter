package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.command.JpgConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class TiffToJpegMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("tiff");
	
	@Override
	public boolean canHandle(@Nullable FFprobeResult probeResult, @NonNull Path file) {
		if(Objects.isNull(probeResult)){
			return false;
		}
		return probeResult.getStreams().stream()
				.anyMatch(stream -> CODECS.contains(stream.getCodecName()));
	}
	
	@Override
	@NonNull
	public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput, @Nullable Integer ffmpegThreads){
		return new JpgConverter(input, output, temporary, deleteInput, converterProgressBarSupplier);
	}
	
	@Override
	@NonNull
	public String getDesiredExtension(){
		return "jpg";
	}
}
