package fr.rakambda.mediaconverter.itemprocessor;

import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Path;

@Log4j2
public class JxlConverter extends ConverterRunnable{
	public JxlConverter(@NonNull Path input, @NonNull Path output, @NonNull Path temporary) {
		super(input, output, temporary);
	}
	
	@Override
	protected void convert() throws InterruptedException, IOException{
		log.info("Converting {} to {}", getInput(), getOutput());
		ProcessBuilder builder = new ProcessBuilder("magick",
				getInput().toAbsolutePath().toString(),
				getTemporary().toString());
		Process process = builder.start();
		process.waitFor();
	}
}
