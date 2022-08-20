package fr.raksrinana.mediaconverter.itemprocessor;

import lombok.extern.log4j.Log4j2;
import java.io.IOException;
import java.nio.file.Path;

@Log4j2
public class TiffConverter extends ConverterRunnable{
	public TiffConverter(Path input, Path output, Path temporary){
		super(input, output, temporary);
	}
	
	@Override
	protected boolean isCopyAttributes(){
		return false;
	}
	
	@Override
	protected void convert(){
		log.info("Converting {} to {}", getInput(), getOutput());
		try{
			ProcessBuilder builder = new ProcessBuilder("magick",
					"-quality", "85%",
					"-sampling-factor", "4:2:0",
					"-interlace", "JPEG",
					"-colorspace", "sRGB",
					getInput().toAbsolutePath().toString(), getTempPath().toString());
			Process process = builder.start();
			process.waitFor();
		}
		catch(IOException | InterruptedException e){
			log.error("Failed to run imagemagick on {}", getInput(), e);
		}
	}
}
