package fr.raksrinana.mediaconverter.utils;

import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;

@Slf4j
public class ProgressBarNotifier implements ProgressListener{
	private final String filename;
	private final long frameCount;
	private final String totalDuration;
	
	public ProgressBarNotifier(String filename, long frameCount, String totalDuration){
		this.filename = filename;
		this.frameCount = frameCount;
		this.totalDuration = totalDuration;
	}
	
	@Override
	public void onProgress(FFmpegProgress progress){
		var processedDuration = Duration.ofMillis(progress.getTimeMillis());
		log.info("{} - {} / {} frames - {} fps - {}h{}m{}s / {}",
				filename,
				progress.getFrame(), frameCount,
				progress.getFps(),
				processedDuration.toHours(), processedDuration.toMinutesPart(), processedDuration.toSecondsPart(), totalDuration);
	}
}