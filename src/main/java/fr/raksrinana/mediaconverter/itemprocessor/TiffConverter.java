package fr.raksrinana.mediaconverter.itemprocessor;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class TiffConverter extends ConverterRunnable{
	public TiffConverter(Path input, Path output){
		super(input, output);
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
					getInput().toAbsolutePath().toString(), getOutput().toAbsolutePath().toString());
			Process process = builder.start();
			process.waitFor();
			
			if(Files.exists(getOutput())){
				trashFile(getInput());
				
				log.info("Converted {} to {}", getInput(), getOutput());
			}
			else{
				log.warn("Output file {} not found, something went wrong", getOutput());
			}
		}
		catch(IOException | InterruptedException e){
			log.error("Failed to run imagemagick on {}", getInput(), e);
		}
	}
}
