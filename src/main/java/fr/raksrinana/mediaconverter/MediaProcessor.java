package fr.raksrinana.mediaconverter;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import java.nio.file.Path;

public interface MediaProcessor{
	boolean canHandle(FFmpegProbeResult probeResult);
	
	Runnable createConvertTask(FFmpeg ffmpeg, FFmpegProbeResult probeResult, Path input, Path output, Path temporary);
	
	String getDesiredExtension();
}
