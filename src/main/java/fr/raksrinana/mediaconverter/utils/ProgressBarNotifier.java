package fr.raksrinana.mediaconverter.utils;

import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import lombok.extern.log4j.Log4j2;
import java.time.Duration;

@Log4j2
public class ProgressBarNotifier implements ProgressListener{
	private final String filename;
	private final long frameCount;
	private final String totalDuration;
	
	public ProgressBarNotifier(String filename, long frameCount, Duration totalDuration){
		this.filename = filename;
		this.frameCount = frameCount;
		this.totalDuration = durationToStr(totalDuration);
	}
	
	private String durationToStr(Duration duration){
		return String.format("%02dh%02dm%02ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
	}
	
	@Override
	public void onProgress(FFmpegProgress progress){
		var processedDuration = Duration.ofMillis(progress.getTimeMillis());
		log.info("{} - {} / {} frames - {} fps - {} / {}",
				filename,
				progress.getFrame(), frameCount,
				progress.getFps(),
				durationToStr(processedDuration), totalDuration);
	}
}
