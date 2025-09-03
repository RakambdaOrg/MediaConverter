package fr.rakambda.mediaconverter.file;

import fr.rakambda.mediaconverter.storage.IStorage;
import org.jspecify.annotations.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.function.Consumer;

@Slf4j
public class FileFilter implements Runnable{
	private final Path file;
	private final ProgressBar progressBar;
	private final IStorage storage;
	private final Collection<String> extensionsToScan;
	private final Consumer<Path> callback;
	
	public FileFilter(@NonNull Path file,
			@NonNull ProgressBar progressBar,
			@NonNull IStorage storage,
			@NonNull Collection<String> extensionsToScan,
			@NonNull Consumer<Path> callback
	){
		this.file = file;
		this.progressBar = progressBar;
		this.storage = storage;
		this.extensionsToScan = extensionsToScan;
		this.callback = callback;
	}
	
	@Override
	public void run(){
		if(processFile(file)){
			callback.accept(file);
		}
		else{
			progressBar.step();
		}
	}
	
	private boolean processFile(@NonNull Path file){
		try{
			if(storage.isUseless(file)){
				return false;
			}
			
			if(isNotMedia(file) || Files.isHidden(file)){
				storage.setUseless(file);
				return false;
			}
			return true;
		}
		catch(SQLException | IOException e){
			log.error("Failed to filter file {}", file, e);
			return false;
		}
	}
	
	private boolean isNotMedia(@NonNull Path file){
		var filename = file.getFileName().toString();
		var dotIndex = filename.lastIndexOf('.');
		
		if(dotIndex <= 0){
			return true;
		}
		
		var extension = filename.substring(dotIndex + 1).toLowerCase();
		return !extensionsToScan.contains(extension);
	}
}
