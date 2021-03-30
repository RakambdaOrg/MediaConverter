package fr.raksrinana.mediaconverter.mediaprocessor;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import fr.raksrinana.mediaconverter.itemprocessor.HevcConverter;
import java.nio.file.Path;
import java.util.List;

public class VideoToHevcMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("h264", "vp9", "wmv3");
	
	@Override
	public boolean canHandle(FFprobeResult probeResult){
		return probeResult.getStreams().stream()
				.anyMatch(stream -> CODECS.contains(stream.getCodecName()));
	}
	
	@Override
	public Runnable createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary){
		return new HevcConverter(ffmpeg, probeResult, input, output, temporary);
	}
	
	@Override
	public String getDesiredExtension(){
		return "mp4";
	}
}
