package fr.raksrinana.mediaconverter;

import fr.raksrinana.mediaconverter.storage.IStorage;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Slf4j
public class FileScanner implements FileVisitor<Path>{
	private final ProgressBar progressBar;
	private final IStorage storage;
	private final Collection<Path> queue;
	private final Collection<Path> excluded;
	private final Collection<String> extensionsToScan;
	
	public FileScanner(ProgressBar progressBar, IStorage storage, Collection<Path> queue, Collection<Path> excluded, Collection<String> extensionsToScan){
		this.progressBar = progressBar;
		this.storage = storage;
		this.queue = queue;
		this.excluded = excluded;
		this.extensionsToScan = extensionsToScan;
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException{
		if(Files.isHidden(dir) && Objects.nonNull(dir.getParent())){
			return SKIP_SUBTREE;
		}
		var isExcluded = excluded.stream().anyMatch(exclude -> Objects.equals(exclude, dir.toAbsolutePath()));
		if(isExcluded){
			return SKIP_SUBTREE;
		}
		log.debug("Entering folder {}", dir);
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException{
		try{
			if(storage.isUseless(file)){
				return CONTINUE;
			}
		}
		catch(SQLException e){
			log.error("Failed to interact with storage", e);
		}
		
		if(isNotMedia(file) || Files.isHidden(file)){
			storage.setUseless(file);
			return CONTINUE;
		}
		
		progressBar.maxHint(progressBar.getMax() + 1);
		queue.add(file);
		return CONTINUE;
	}
	
	private boolean isNotMedia(Path file){
		var filename = file.getFileName().toString();
		var dotIndex = filename.lastIndexOf('.');
		
		if(dotIndex <= 0){
			return true;
		}
		
		var extension = filename.substring(dotIndex + 1).toLowerCase();
		return !extensionsToScan.contains(extension);
	}
	
	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc){
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException exc){
		log.trace("Leaving folder {}", dir);
		try{
			storage.save();
		}
		catch(SQLException | IOException e){
			log.error("Failed to save useless files after folder", e);
		}
		return CONTINUE;
	}
}
