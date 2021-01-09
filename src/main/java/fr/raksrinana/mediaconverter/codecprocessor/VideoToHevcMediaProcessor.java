package fr.raksrinana.mediaconverter.codecprocessor;

import fr.raksrinana.mediaconverter.MediaProcessor;
import fr.raksrinana.mediaconverter.itemprocessor.HevcConverter;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import java.nio.file.Path;
import java.util.List;

public class VideoToHevcMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("h264", "vp9", "wmv3");
	
	@Override
	public boolean canHandle(FFmpegProbeResult probeResult){
		return probeResult.getStreams().stream()
				.anyMatch(stream -> CODECS.contains(stream.codec_name));
	}
	
	@Override
	public Runnable createConvertTask(FFmpeg ffmpeg, FFmpegProbeResult probeResult, Path input, Path output, Path temporary){
		return new HevcConverter(ffmpeg, probeResult, input, output, temporary);
	}
	
	@Override
	public String getDesiredExtension(){
		return "mp4";
	}
}
