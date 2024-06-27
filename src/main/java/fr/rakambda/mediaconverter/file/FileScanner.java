package fr.rakambda.mediaconverter.file;

import fr.rakambda.mediaconverter.IProcessor;
import fr.rakambda.mediaconverter.storage.IStorage;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

@Slf4j
public class FileScanner implements FileVisitor<Path>, AutoCloseable, IProcessor{
	private final ProgressBar progressBar;
	private final IStorage storage;
	@Getter
	private final BlockingQueue<Path> outputQueue;
	private final Collection<Path> excluded;
	
	private boolean shutdown;
	
	public FileScanner(@NonNull ProgressBar progressBar, @NonNull IStorage storage, @NonNull Collection<Path> excluded){
		this.progressBar = progressBar;
		this.storage = storage;
		this.excluded = excluded;
		shutdown = false;
		
		outputQueue = new LinkedBlockingQueue<>(500);
		progressBar.maxHint(progressBar.getMax() + 1);
	}
	
	@Override
	public FileVisitResult preVisitDirectory(@NonNull Path dir, @NonNull BasicFileAttributes attrs) throws IOException{
		if(shutdown){
			return TERMINATE;
		}
		if(Files.isHidden(dir) && Objects.nonNull(dir.getParent())){
			return SKIP_SUBTREE;
		}
		var isExcluded = excluded.stream().anyMatch(exclude -> Objects.equals(exclude, dir.toAbsolutePath()));
		if(isExcluded){
			return SKIP_SUBTREE;
		}
		log.debug("Entering folder {}", dir);
		try(var listing = Files.list(dir)){
			progressBar.maxHint(progressBar.getMax() + listing.count());
		}
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs){
		if(shutdown){
			return TERMINATE;
		}
		try{
			outputQueue.put(file);
		}
		catch(InterruptedException e){
			throw new RuntimeException(e);
		}
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFileFailed(@NonNull Path file, @NonNull IOException exc){
		if(shutdown){
			return TERMINATE;
		}
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult postVisitDirectory(@NonNull Path dir, @Nullable IOException exc){
		if(shutdown){
			return TERMINATE;
		}
		log.trace("Leaving folder {}", dir);
		progressBar.step();
		try{
			storage.save();
		}
		catch(SQLException | IOException e){
			log.error("Failed to save useless files after folder", e);
		}
		return CONTINUE;
	}
	
	@Override
	public void resume(){
	}
	
	@Override
	public void pause(){
	}
	
	@Override
	public void close(){
		shutdown = true;
	}
}
