package fr.raksrinana.mediaconverter.progress;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.ProgressBar;

@RequiredArgsConstructor
public class ProgressBarHandle implements AutoCloseable{
	@Getter
	private final ProgressBar progressBar;
	private final ProgressBarSupplier parent;
	
	@Override
	public void close(){
		parent.addBack(progressBar);
	}
}
