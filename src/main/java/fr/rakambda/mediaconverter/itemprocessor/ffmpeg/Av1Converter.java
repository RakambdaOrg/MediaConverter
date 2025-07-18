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
public class Av1Converter extends FfmpegVideoConverter{
	public Av1Converter(@NonNull FFmpeg ffmpeg,
			@Nullable FFprobeResult probeResult,
			@NonNull Path input,
			@NonNull Path output,
			@NonNull Path temporary,
			@NonNull ProgressBarSupplier converterProgressBarSupplier,
			boolean deleteInput,
			@Nullable Integer ffmpegThreads
	){
		super(ffmpeg, probeResult, input, output, temporary, converterProgressBarSupplier, deleteInput, ffmpegThreads);
	}
	
	@Override
	protected BaseOutput<?> buildOutput(BaseOutput<?> output){
		return output
				.setCodec(StreamType.SUBTITLE, "copy")
				.setCodec(StreamType.AUDIO, "libopus")
				.addArguments("-b:a", "128000")
				.setCodec(StreamType.VIDEO, "libsvtav1")
				.addArguments("-preset", "4")
				.addArguments("-crf", "32")
				.addArguments("-g", "240")
				.addArguments("-svtav1-params", "tune=0:enable-overlays=1:scd=1:scm=2")
				.addArguments("-pix_fmt", "yuv420p10le")
				.addArguments("-movflags", "use_metadata_tags")
				.addArguments("-map_metadata", "0")
				.addArguments("-map", "0")
				.addArguments("-map", "-0:d")
				.addArguments("-max_muxing_queue_size", "512");
	}
}
