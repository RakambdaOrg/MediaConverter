package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.ffmpeg.HevcConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public abstract class VideoToHevcMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("h264", "vp9", "wmv3", "mpeg2video");
	
	@NotNull
	protected abstract String getDesiredFormat();
	
	@Override
	public boolean canHandle(@Nullable FFprobeResult probeResult, @NonNull Path file){
		if(Objects.isNull(probeResult)){
			return false;
		}
		
		if(isNotHevc(probeResult)){
			return true;
		}
		
		return isOtherContainer(probeResult);
	}
	
	private boolean isOtherContainer(@NotNull FFprobeResult probeResult){
		return !Objects.equals(probeResult.getFormat().getFormatName(), getDesiredFormat());
	}
	
	private boolean isNotHevc(@NotNull FFprobeResult probeResult){
		return probeResult.getStreams().stream()
				.anyMatch(stream -> isWantedCodec(stream.getCodecName()) || isOtherHevc(stream.getCodecName(), stream.getCodecTagString()));
	}
	
	private boolean isWantedCodec(@NonNull String codecName){
		return CODECS.contains(codecName);
	}
	
	private boolean isOtherHevc(@NonNull String codecName, @NonNull String codecTagString){
		return "hevc".equals(codecName) && "hvc1".equals(codecTagString);
	}
	
	@Override
	@NonNull
	public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier progressBarSupplier, boolean deleteInput, @Nullable Integer ffmpegThreads){
		return new HevcConverter(ffmpeg, probeResult, input, output, temporary, progressBarSupplier, deleteInput, ffmpegThreads, !isNotHevc(probeResult));
	}
}
