package fr.raksrinana.mediaconverter;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import java.nio.file.Path;

public interface MediaProcessor{
	boolean canHandle(FFprobeResult probeResult);
	
	Runnable createConvertTask(FFmpeg ffmpeg, FFprobeResult probeResult, Path input, Path output, Path temporary);
	
	String getDesiredExtension();
}
