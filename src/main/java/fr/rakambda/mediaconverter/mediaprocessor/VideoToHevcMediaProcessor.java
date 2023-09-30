package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.ffmpeg.HevcConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class VideoToHevcMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("h264", "vp9", "wmv3", "mpeg2video");
	
	@Override
	public boolean canHandle(@Nullable FFprobeResult probeResult, @NonNull Path file) {
		if(Objects.isNull(probeResult)){
			return false;
		}
		return probeResult.getStreams().stream()
				.anyMatch(stream -> isWantedCodec(stream.getCodecName()) || isOtherHevc(stream.getCodecName(), stream.getCodecTagString()));
	}

	private boolean isWantedCodec(@NonNull String codecName) {
		return CODECS.contains(codecName);
	}

	private boolean isOtherHevc(@NonNull String codecName, @NonNull String codecTagString) {
		return "hevc".equals(codecName) && "hvc1".equals(codecTagString);
	}
	
	@Override
	@NonNull
	public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier progressBarSupplier, boolean deleteInput){
		return new HevcConverter(ffmpeg, probeResult, input, output, temporary, progressBarSupplier, deleteInput);
	}
	
	@Override
	@NonNull
	public String getDesiredExtension(){
		return "mkv";
	}
}
