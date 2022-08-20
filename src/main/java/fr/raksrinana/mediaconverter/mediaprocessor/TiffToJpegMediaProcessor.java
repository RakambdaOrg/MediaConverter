package fr.raksrinana.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.raksrinana.mediaconverter.itemprocessor.TiffConverter;
import fr.raksrinana.mediaconverter.progress.ProgressBarSupplier;
import java.nio.file.Path;
import java.util.List;

public class TiffToJpegMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("tiff");
	
	@Override
	public boolean canHandle(FFprobeResult probeResult){
		return probeResult.getStreams().stream()
				.anyMatch(stream -> CODECS.contains(stream.getCodecName()));
	}
	
	@Override
	public MediaProcessorTask createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary, ProgressBarSupplier converterProgressBarSupplier){
		return new TiffConverter(input, output, temporary);
	}
	
	@Override
	public String getDesiredExtension(){
		return "jpg";
	}
}
