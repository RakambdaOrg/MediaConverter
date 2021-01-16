package fr.raksrinana.mediaconverter.codecprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.raksrinana.mediaconverter.MediaProcessor;
import fr.raksrinana.mediaconverter.itemprocessor.TiffConverter;
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
	public Runnable createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary){
		return new TiffConverter(input, output);
	}
	
	@Override
	public String getDesiredExtension(){
		return "jpg";
	}
}
