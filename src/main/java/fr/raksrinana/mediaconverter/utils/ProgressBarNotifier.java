package fr.raksrinana.mediaconverter.utils;

import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;

@Log4j2
public class ProgressBarNotifier implements ProgressListener, AutoCloseable{
	private final String filename;
	private final long frameCount;
	private final String totalDuration;
	private final ProgressBar progressBar;
	
	public ProgressBarNotifier(@NotNull String filename, long frameCount, @NotNull Duration totalDuration){
		this.filename = filename;
		this.frameCount = frameCount;
		this.totalDuration = durationToStr(totalDuration);
		progressBar = new ProgressBarBuilder()
				.setTaskName(filename)
				.setUnit("Frames", 1)
				.setInitialMax(frameCount)
				.build();
	}
	
	@NotNull
	private String durationToStr(@NotNull Duration duration){
		return String.format("%02dh%02dm%02ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
	}
	
	@Override
	public void onProgress(FFmpegProgress progress){
		var processedDuration = Duration.ofMillis(progress.getTimeMillis());
		progressBar.stepTo(progress.getFrame());
		progressBar.setExtraMessage(progress.getFps() + " fps");
		log.debug("{} - {} / {} frames - {} fps - {} / {}",
				filename,
				progress.getFrame(), frameCount,
				progress.getFps(),
				durationToStr(processedDuration), totalDuration);
	}
	
	@Override
	public void close(){
		progressBar.close();
	}
}
