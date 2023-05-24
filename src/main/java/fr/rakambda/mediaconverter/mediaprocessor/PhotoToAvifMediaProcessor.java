package fr.rakambda.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.rakambda.mediaconverter.itemprocessor.AvifConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import java.nio.file.Path;
import java.util.List;

public class PhotoToAvifMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("jpeg");
	
	@Override
	public boolean canHandle(FFprobeResult probeResult){
		return probeResult.getStreams().stream()
				.anyMatch(stream -> CODECS.contains(stream.getCodecName()));
	}
	
	@Override
	public MediaProcessorTask createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary, ProgressBarSupplier converterProgressBarSupplier){
		return new AvifConverter(input, output, temporary);
	}
	
	@Override
	public String getDesiredExtension(){
		return "avif";
	}
}
