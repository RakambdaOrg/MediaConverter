package fr.rakambda.mediaconverter.itemprocessor.ffmpeg;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.BaseOutput;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.FfmpegVideoConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;

@Log4j2
public class HevcConverter extends FfmpegVideoConverter{
	private final boolean isTargetWithOtherContainer;
	
	public HevcConverter(@NonNull FFmpeg ffmpeg,
			@Nullable FFprobeResult probeResult,
			@NonNull Path input,
			@NonNull Path output,
			@NonNull Path temporary,
			@NonNull ProgressBarSupplier converterProgressBarSupplier,
			boolean deleteInput,
			@Nullable Integer ffmpegThreads,
			boolean isTargetWithOtherContainer
	){
		super(ffmpeg, probeResult, input, output, temporary, converterProgressBarSupplier, deleteInput, ffmpegThreads);
		this.isTargetWithOtherContainer = isTargetWithOtherContainer;
	}
	
	@Override
	protected BaseOutput<?> buildOutput(BaseOutput<?> output){
		if(isTargetWithOtherContainer){
			return output.copyAllCodecs();
		}
		
		return output
				.setCodec(StreamType.AUDIO, "aac")
				.addArguments("-b:a", "128000")
				.setCodec(StreamType.VIDEO, "libx265")
				.addArguments("-preset", "medium")
				.addArguments("-crf", "21")
				.addArguments("-vf", "format=yuv420p10le")
				.addArguments("-movflags", "use_metadata_tags")
				.addArguments("-map_metadata", "0")
				.addArguments("-map", "0")
				.addArguments("-map", "-0:d")
				.addArguments("-max_muxing_queue_size", "512");
	}
}
