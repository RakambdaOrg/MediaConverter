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
		if(isPicture(probeResult)){
			return false;
		}
		
		if(isOtherVideoType(probeResult)){
			return true;
		}
		if(isOtherHevcType(probeResult)){
			return true;
		}
		
		return isHevcWithOtherContainer(probeResult);
	}
	
	private boolean isPicture(@NotNull FFprobeResult probeResult){
		return probeResult.getStreams().stream().anyMatch(stream -> "Main Still Picture".equals(stream.getProfile()));
	}
	
	private boolean isOtherVideoType(@NotNull FFprobeResult probeResult){
		return probeResult.getStreams().stream().anyMatch(stream -> CODECS.contains(stream.getCodecName()));
	}
	
	private boolean isOtherHevcType(@NotNull FFprobeResult probeResult){
		return probeResult.getStreams().stream()
				.filter(this::isHevcCodec)
				.anyMatch(stream -> !isTargetCodecTag(stream));
	}
	
	private boolean isHevcCodec(@NonNull Stream stream){
		return "hevc".equals(stream.getCodecName());
	}
	
	private boolean isTargetCodecTag(@NonNull Stream stream){
		return "hvc1".equals(stream.getCodecTagString()) || "[0][0][0][0]".equals(stream.getCodecTagString());
	}
	
	private boolean isHevcWithOtherContainer(@NotNull FFprobeResult probeResult){
		var isCorrectContainer = Objects.equals(probeResult.getFormat().getFormatName(), getDesiredFormat());
		var isCorrectCodecTag = probeResult.getStreams().stream().anyMatch(this::isTargetCodecTag);
		var isHevc = probeResult.getStreams().stream().anyMatch(this::isHevcCodec);
		return isHevc && (!isCorrectContainer || !isCorrectCodecTag);
	}
	
	@Override
	@NonNull
	public MediaProcessorTask createConvertTask(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier progressBarSupplier, boolean deleteInput, @Nullable Integer ffmpegThreads){
		var hevcWithOtherContainer = isHevcWithOtherContainer(probeResult);
		return new HevcConverter(ffmpeg, probeResult, input, output, temporary, progressBarSupplier, deleteInput, ffmpegThreads, hevcWithOtherContainer);
	}
}
