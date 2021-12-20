package fr.raksrinana.mediaconverter;

import fr.raksrinana.mediaconverter.storage.IStorage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.tongfei.progressbar.ProgressBar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FileFilter implements Runnable{
	private final ProgressBar progressBar;
	private final IStorage storage;
	private final BlockingQueue<Path> inputQueue;
	@Getter
	private final BlockingQueue<Path> outputQueue;
	private final Collection<String> extensionsToScan;
	private final CountDownLatch countDownLatch;
	private boolean shutdown;
	
	public FileFilter(ProgressBar progressBar, IStorage storage, BlockingQueue<Path> inputQueue, Collection<String> extensionsToScan){
		this.progressBar = progressBar;
		this.storage = storage;
		this.inputQueue = inputQueue;
		this.extensionsToScan = extensionsToScan;
		
		outputQueue = new LinkedBlockingDeque<>();
		shutdown = false;
		countDownLatch = new CountDownLatch(1);
	}
	
	@Override
	public void run(){
		try{
			do{
				var file = inputQueue.poll(5, TimeUnit.SECONDS);
				if(Objects.nonNull(file)){
					if(processFile(file)){
						outputQueue.offer(file);
					}
					else{
						progressBar.step();
					}
				}
			}
			while(!shutdown || !inputQueue.isEmpty());
		}
		catch(InterruptedException e){
			log.error("Error waiting for element", e);
		}
		finally{
			countDownLatch.countDown();
		}
	}
	
	private boolean processFile(Path file){
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
	
	private boolean isNotMedia(Path file){
		var filename = file.getFileName().toString();
		var dotIndex = filename.lastIndexOf('.');
		
		if(dotIndex <= 0){
			return true;
		}
		
		var extension = filename.substring(dotIndex + 1).toLowerCase();
		return !extensionsToScan.contains(extension);
	}
	
	public void shutdown() throws InterruptedException{
		shutdown = true;
		countDownLatch.await();
	}
}
