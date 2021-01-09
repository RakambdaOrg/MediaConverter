package fr.raksrinana.mediaconverter.utils;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
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
	public void progress(Progress progress){
		final var processedDuration = Duration.ofNanos(progress.out_time_ns);
		log.info("{} - {} / {} frames - {} fps - {}h{}m{}s / {}",
				filename,
				progress.frame, frameCount,
				progress.fps.floatValue(),
				processedDuration.toHours(), processedDuration.toMinutesPart(), processedDuration.toSecondsPart(), totalDuration);
	}
}
