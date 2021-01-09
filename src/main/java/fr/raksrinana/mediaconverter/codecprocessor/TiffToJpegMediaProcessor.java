package fr.raksrinana.mediaconverter.codecprocessor;

import fr.raksrinana.mediaconverter.MediaProcessor;
import fr.raksrinana.mediaconverter.itemprocessor.TiffConverter;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import java.nio.file.Path;
import java.util.List;

public class TiffToJpegMediaProcessor implements MediaProcessor{
	private static final List<String> CODECS = List.of("tiff");
	
	@Override
	public boolean canHandle(FFmpegProbeResult probeResult){
		return probeResult.getStreams().stream()
				.anyMatch(stream -> CODECS.contains(stream.codec_name));
	}
	
	@Override
	public Runnable createConvertTask(FFmpeg ffmpeg, FFmpegProbeResult probeResult, Path input, Path output, Path temporary){
		return new TiffConverter(ffmpeg, probeResult, input, output, temporary);
	}
	
	@Override
	public String getDesiredExtension(){
		return "jpg";
	}
}
