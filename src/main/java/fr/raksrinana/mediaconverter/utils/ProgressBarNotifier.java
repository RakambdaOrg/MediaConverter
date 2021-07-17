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
				.setUnit("s", 1000)
				.setInitialMax(totalDuration.toMillis())
				.build();
	}
	
	@NotNull
	private String durationToStr(@NotNull Duration duration){
		return String.format("%02dh%02dm%02ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
	}
	
	@Override
	public void onProgress(FFmpegProgress progress){
		progressBar.stepTo(progress.getTimeMillis());
		progressBar.setExtraMessage("%02ffps".formatted(progress.getFps()));
		log.debug("{} - {} / {} frames - {} fps - {} / {}",
				filename,
				progress.getFrame(), frameCount,
				progress.getFps(),
				durationToStr(Duration.ofMillis(progress.getTimeMillis())), totalDuration);
	}
	
	@Override
	public void close(){
		progressBar.close();
	}
}
