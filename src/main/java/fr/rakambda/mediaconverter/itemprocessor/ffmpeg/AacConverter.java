package fr.rakambda.mediaconverter.itemprocessor.ffmpeg;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffmpeg.BaseOutput;
import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.Output;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.FfmpegVideoConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import java.nio.file.Path;

@Log4j2
public class AacConverter extends FfmpegVideoConverter{
	public AacConverter(@NonNull FFmpeg ffmpeg, @Nullable FFprobeResult probeResult, @NonNull Path input, @NonNull Path output, @NonNull Path temporary, @NonNull ProgressBarSupplier converterProgressBarSupplier, boolean deleteInput){
		super(ffmpeg, probeResult, input, output, temporary, converterProgressBarSupplier, deleteInput);
	}
	
	@Override
	protected Output buildOutput(BaseOutput<?> output){
		return output
				.setCodec(StreamType.AUDIO, "aac")
				.addArguments("-b:a", "192k");
	}
}
