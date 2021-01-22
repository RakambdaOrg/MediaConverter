package fr.raksrinana.mediaconverter.itemprocessor;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;

@Slf4j
@Getter
public abstract class ConverterRunnable implements Runnable{
	private final Path input;
	private final Path output;
	
	protected ConverterRunnable(Path input, Path output){
		this.input = input.toAbsolutePath().normalize();
		this.output = output.toAbsolutePath().normalize();
	}
	
	protected void trashFile(Path file) throws IOException{
		if(Desktop.isDesktopSupported()){
			var desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)){
				if(desktop.moveToTrash(file.toFile())){
					log.info("Moved file {} to trash", file);
					return;
				}
			}
		}
		
		if(Files.deleteIfExists(file)){
			log.info("Deleted file {}", file);
		}
	}
	
	protected void copyFileAttributes(Path from, Path to) throws IOException{
		var baseAttributes = Files.getFileAttributeView(from, BasicFileAttributeView.class).readAttributes();
		var attributes = Files.getFileAttributeView(to, BasicFileAttributeView.class);
		attributes.setTimes(baseAttributes.lastModifiedTime(), baseAttributes.lastAccessTime(), baseAttributes.creationTime());
	}
	
	@Override
	public void run(){
		try{
			convert();
		}
		catch(Exception e){
			log.error("Error converting", e);
		}
	}
	
	protected abstract void convert();
}
