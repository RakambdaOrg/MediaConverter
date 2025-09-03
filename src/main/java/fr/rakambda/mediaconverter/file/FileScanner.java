package fr.rakambda.mediaconverter.file;

import fr.rakambda.mediaconverter.storage.IStorage;
import org.jspecify.annotations.NonNull;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import org.jspecify.annotations.Nullable;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.FileVisitResult.TERMINATE;

@Slf4j
public class FileScanner implements FileVisitor<Path>, AutoCloseable{
	private final ProgressBar progressBar;
	private final IStorage storage;
	private final Collection<Path> excluded;
	private final Consumer<Path> callback;
	
	private boolean shutdown;
	
	public FileScanner(@NonNull ProgressBar progressBar, @NonNull IStorage storage, @NonNull Collection<Path> excluded, Consumer<Path> callback){
		this.progressBar = progressBar;
		this.storage = storage;
		this.excluded = excluded;
		this.callback = callback;
		shutdown = false;
		
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
		callback.accept(file);
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
	public void close(){
		shutdown = true;
	}
}
