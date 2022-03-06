package fr.raksrinana.mediaconverter.progress;

import lombok.RequiredArgsConstructor;
import me.tongfei.progressbar.InteractiveConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.temporal.ChronoUnit;

@RequiredArgsConstructor
public class ProgressBarGenerator{
	public ProgressBar generate(int index){
		return new ProgressBarBuilder()
				.setTaskName("Converter " + (index + 1))
				.setUnit("frame", 1)
				.setSpeedUnit(ChronoUnit.SECONDS)
				.showSpeed()
				.setConsumer(new InteractiveConsoleProgressBarConsumer(new PrintStream(new FileOutputStream(FileDescriptor.err)), -1))
				.build();
	}
}
