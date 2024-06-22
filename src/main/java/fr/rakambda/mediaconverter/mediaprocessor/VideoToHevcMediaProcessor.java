package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
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
		
		if(isOtherVideoType(probeResult)){
			return true;
		}
		if(isOtherHevcType(probeResult)){
			return true;
		}
		
		return isTargetWithOtherContainer(probeResult);
	}
	
	private boolean isOtherVideoType(@NotNull FFprobeResult probeResult){
		return probeResult.getStreams().stream().anyMatch(stream -> CODECS.contains(stream.getCodecName()));
	}
	
	private boolean isOtherHevcType(@NotNull FFprobeResult probeResult){
		return probeResult.getStreams().stream()
				.anyMatch(stream -> isOtherHevcType(stream.getCodecName(), stream.getCodecTagString()));
	}
	
	private boolean isOtherHevcType(@NonNull String codecName, @NonNull String codecTagString){
		return "hevc".equals(codecName) && "hvc1".equals(codecTagString);
	}
	
	private boolean isTargetWithOtherContainer(@NotNull FFprobeResult probeResult){
		return !Objects.equals(probeResult.getFormat().getFormatName(), getDesiredFormat())
				&& probeResult.getStreams().stream().map(Stream::getCodecName).anyMatch("hev1"::equals);
	}
	
	@Override
	@NonNull
	public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier progressBarSupplier, boolean deleteInput, @Nullable Integer ffmpegThreads){
		return new HevcConverter(ffmpeg, probeResult, input, output, temporary, progressBarSupplier, deleteInput, ffmpegThreads, isTargetWithOtherContainer(probeResult));
	}
}
