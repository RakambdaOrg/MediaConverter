package fr.rakambda.mediaconverter.itemprocessor;

import lombok.extern.log4j.Log4j2;
import java.io.IOException;
import java.nio.file.Path;

@Log4j2
public class JpgConverter extends ConverterRunnable{
	public JpgConverter(Path input, Path output, Path temporary){
		super(input, output, temporary);
	}
	
	@Override
	protected boolean isCopyAttributes(){
		return false;
	}
	
	@Override
	protected void convert() throws InterruptedException, IOException{
		log.info("Converting {} to {}", getInput(), getOutput());
		ProcessBuilder builder = new ProcessBuilder("magick",
				"-quality", "85%",
				"-sampling-factor", "4:2:0",
				"-interlace", "JPEG",
				"-colorspace", "sRGB",
				getInput().toAbsolutePath().toString(), getTemporary().toString());
		Process process = builder.start();
		process.waitFor();
	}
}
