package fr.rakambda.mediaconverter.itemprocessor.ffmpeg;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.BaseOutput;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.FfmpegVideoConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import org.jspecify.annotations.NonNull;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import java.nio.file.Path;

@Log4j2
public class OpusConverter extends FfmpegVideoConverter{
	public OpusConverter(FFmpeg ffmpeg,
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
				.setCodec(StreamType.AUDIO, "libopus")
				.addArguments("-b:a", "128000");
	}
}
