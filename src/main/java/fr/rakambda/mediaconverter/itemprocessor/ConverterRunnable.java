package fr.rakambda.mediaconverter.itemprocessor;

import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessorTask;
import fr.rakambda.mediaconverter.utils.FutureHelper;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Log4j2
@Getter
public abstract class ConverterRunnable implements MediaProcessorTask{
	private final Path input;
	private final Path output;
	private final Path temporary;
	private final boolean deleteInput;
	private final Collection<Runnable> listeners;
	
	protected ConverterRunnable(@NonNull Path input, @NonNull Path output, @NonNull Path temporary, boolean deleteInput){
		this.input = input.toAbsolutePath().normalize();
		this.output = output.toAbsolutePath().normalize();
		this.temporary = temporary.toAbsolutePath().normalize();
		this.deleteInput = deleteInput;
		
		listeners = new ArrayDeque<>();
	}
	
	@Override
	public void addCompletionListener(@NonNull Runnable listener){
		listeners.add(listener);
	}
	
	protected void trashFile(@NonNull Path file) throws IOException{
		if(Desktop.isDesktopSupported()){
			var desktop = Desktop.getDesktop();
			if(desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)){
				if(desktop.moveToTrash(file.toFile())){
					log.debug("Moved file {} to trash", file);
					return;
				}
			}
		}
		
		if(Files.deleteIfExists(file)){
			log.debug("Deleted file {}", file);
		}
	}
	
	protected void copyFileAttributes(@NonNull Path from, @NonNull Path to) throws IOException{
		var baseAttributes = Files.getFileAttributeView(from, BasicFileAttributeView.class).readAttributes();
		var attributes = Files.getFileAttributeView(to, BasicFileAttributeView.class);
		attributes.setTimes(baseAttributes.lastModifiedTime(), baseAttributes.lastAccessTime(), baseAttributes.creationTime());
	}
	
	@Override
	public void execute(@NonNull ExecutorService executorService, boolean dryRun){
		try{
			FutureHelper.makeCompletableFuture(convert(executorService, dryRun))
					.thenAccept(this::handleSuccess)
					.exceptionally(this::handleError)
					.thenAccept(empty -> listeners.forEach(Runnable::run));
		}
		catch(Exception e){
			handleError(e);
			listeners.forEach(Runnable::run);
		}
	}
	
	@SneakyThrows(IOException.class)
	private void handleSuccess(Object result){
		if(Files.exists(getTemporary())){
			var inputAttributes = Files.getFileAttributeView(getInput(), BasicFileAttributeView.class).readAttributes();
			
			var inputTemporary = getTemporary().getParent().resolve("original_" + getInput().getFileName().toString());
			if(deleteInput){
				Files.move(getInput(), inputTemporary);
				Files.move(getTemporary(), getOutput());
			}
			else{
				Files.move(getTemporary(), getOutput());
			}
			
			if(isCopyAttributes()){
				copyFileAttributes(inputAttributes, getOutput());
			}
			if(deleteInput){
				trashFile(inputTemporary);
			}
			
			log.debug("Converted {} to {}", getInput(), getOutput());
		}
		else{
			log.warn("Output file {} not found in temp dir {}, something went wrong", getOutput(), getTemporary());
		}
	}
	
	private Void handleError(Throwable error){
		log.error("Error converting {}", getInput(), error);
		try{
			var path = getTemporary();
			if(Objects.nonNull(path)){
				Files.deleteIfExists(path);
			}
		}
		catch(IOException ignored){
		}
		return null;
	}
	
	protected boolean isCopyAttributes(){
		return true;
	}
	
	protected void copyFileAttributes(@NonNull BasicFileAttributes baseAttributes, Path to) throws IOException{
		var attributes = Files.getFileAttributeView(to, BasicFileAttributeView.class);
		attributes.setTimes(baseAttributes.lastModifiedTime(), baseAttributes.lastAccessTime(), baseAttributes.creationTime());
	}
	
	protected abstract Future<?> convert(@NonNull ExecutorService executorService, boolean dryRun) throws Exception;
}
