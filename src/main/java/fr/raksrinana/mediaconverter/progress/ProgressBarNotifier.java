package fr.raksrinana.mediaconverter.progress;

import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.NotNull;
import java.time.Duration;
import java.util.Objects;

@Log4j2
public class ProgressBarNotifier implements ProgressListener{
	private final String filename;
	private final long frameCount;
	private final String totalDuration;
	private final ProgressBar progressBar;
	
	public ProgressBarNotifier(@NotNull String filename, long frameCount, @NotNull Duration totalDuration, ProgressBar progressBar){
		this.filename = filename;
		this.frameCount = frameCount;
		this.totalDuration = durationToStr(totalDuration);
		this.progressBar = progressBar;
		
		progressBar.stepTo(0);
		progressBar.setExtraMessage(filename);
		progressBar.maxHint(totalDuration.toMillis());
	}
	
	@NotNull
	private String durationToStr(@NotNull Duration duration){
		return String.format("%02dh%02dm%02ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
	}
	
	@Override
	public void onProgress(FFmpegProgress progress){
		if(Objects.nonNull(progress.getTimeMillis())){
			progressBar.stepTo(progress.getTimeMillis());
		}
		log.debug("{} - {} / {} frames - {} fps - {} / {}",
				filename,
				progress.getFrame(),
				frameCount,
				progress.getFps(),
				durationToStr(Duration.ofMillis(progress.getTimeMillis())),
				totalDuration);
	}
}
