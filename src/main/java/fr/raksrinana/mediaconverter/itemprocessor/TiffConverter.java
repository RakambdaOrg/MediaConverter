package fr.raksrinana.mediaconverter.itemprocessor;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Requires the Recycle module to be installed: https://www.powershellgallery.com/packages/Recycle/1.0.2
 */
@Slf4j
public class TiffConverter implements Runnable{
	private final Path input;
	private final Path output;
	
	public TiffConverter(FFmpeg ffmpeg, FFmpegProbeResult probeResult, Path input, Path output, Path temporary){
		this.input = input;
		this.output = output;
	}
	
	@Override
	public void run(){
		log.info("Converting {} to {}", input, output);
		try{
			ProcessBuilder builder = new ProcessBuilder("magick",
					"-quality", "85%",
					"-sampling-factor", "4:2:0",
					"-interlace", "JPEG",
					"-colorspace", "sRGB",
					input.toAbsolutePath().toString(), output.toAbsolutePath().toString());
			Process process = builder.start();
			process.waitFor();
			
			if(Files.exists(output)){
				trashInput();
				
				log.info("Converted {} to {}", input, output);
			}
			else{
				log.warn("Output file {} not found, something went wrong", output);
			}
		}
		catch(IOException | InterruptedException e){
			log.error("Failed to run imagemagick on {}", input, e);
		}
	}
	
	private void trashInput() throws IOException{
		if(Desktop.isDesktopSupported()){
			var desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)){
				if(desktop.moveToTrash(input.toFile())){
					log.info("Moved input file {} to trash", input);
					return;
				}
			}
		}
		
		if(Files.deleteIfExists(input)){
			log.info("Deleted input file {}", input);
		}
	}
}
