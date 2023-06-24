package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.Av1Converter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class VideoToAv1MediaProcessor implements MediaProcessor {
	private static final List<String> CODECS = List.of("h264", "vp9", "wmv3", "mpeg2video", "hevc", "h265");

	@Override
	public boolean canHandle(FFprobeResult probeResult, Path file){
		if(Objects.isNull(probeResult)){
			return false;
		}
		return probeResult.getStreams().stream().anyMatch(stream -> isWantedCodec(stream.getCodecName()));
	}

	private boolean isWantedCodec(String codecName) {
		return CODECS.contains(codecName);
	}

	@Override
	public MediaProcessorTask createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary, ProgressBarSupplier progressBarSupplier) {
		return new Av1Converter(ffmpeg, probeResult, input, output, temporary, progressBarSupplier);
	}

	@Override
	public String getDesiredExtension() {
		return "mkv";
	}
}
