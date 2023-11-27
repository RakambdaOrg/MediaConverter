package fr.rakambda.mediaconverter.progress;

import com.github.kokorin.jaffree.ffmpeg.FFmpegProgress;
import com.github.kokorin.jaffree.ffmpeg.ProgressListener;
import lombok.extern.log4j.Log4j2;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.NotNull;
import java.io.Closeable;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class ConverterProgressBarNotifier implements ProgressListener, Closeable{
	private final String filename;
	private final long frameCount;
	private final Duration totalDuration;
	private final String totalDurationStr;
	private final ProgressBarSupplier progressBarSupplier;
	
	private ProgressBarHandle progressBarHandle;
	
	public ConverterProgressBarNotifier(@NotNull String filename, long frameCount, @NotNull Duration totalDuration, ProgressBarSupplier progressBarSupplier){
		this.filename = filename;
		this.frameCount = frameCount;
		this.totalDuration = totalDuration;
		totalDurationStr = durationToStr(totalDuration);
		this.progressBarSupplier = progressBarSupplier;
	}
	
	@NotNull
	private String durationToStr(@NotNull Duration duration){
		return String.format("%02dh%02dm%02ds", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
	}
	
	@Override
	public void onProgress(FFmpegProgress progress){
		if(Objects.nonNull(progress.getTimeMillis()) && progress.getTimeMillis() >= 0){
			try{
				getProgressBar().stepTo(progress.getTimeMillis());
			}
			catch(InterruptedException e){
				log.error("Failed to update progress bar", e);
			}
		}
		log.debug("{} - {} / {} frames - {} fps - {} / {}",
				filename,
				progress.getFrame(),
				frameCount,
				progress.getFps(),
				Optional.ofNullable(progress.getTimeMillis()).map(Duration::ofMillis).map(this::durationToStr).orElse(null),
				totalDurationStr);
	}
	
	private ProgressBar getProgressBar() throws InterruptedException{
		if(Objects.isNull(progressBarHandle)){
			progressBarHandle = progressBarSupplier.get();
			progressBarHandle.getProgressBar().stepTo(0);
			progressBarHandle.getProgressBar().setExtraMessage(filename);
			progressBarHandle.getProgressBar().maxHint(totalDuration.toMillis());
		}
		return progressBarHandle.getProgressBar();
	}
	
	@Override
	public void close(){
		if(Objects.nonNull(progressBarHandle)){
			progressBarHandle.close();
		}
	}
}
