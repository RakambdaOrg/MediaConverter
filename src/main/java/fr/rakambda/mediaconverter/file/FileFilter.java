package fr.rakambda.mediaconverter.file;

import fr.rakambda.mediaconverter.storage.IStorage;
import lombok.Getter;
import lombok.NonNull;
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
public class FileFilter implements Runnable, AutoCloseable{
    private final ProgressBar progressBar;
    private final IStorage storage;
    private final BlockingQueue<Path> inputQueue;
    @Getter
    private final BlockingQueue<Path> outputQueue;
    private final Collection<String> extensionsToScan;
    private final CountDownLatch countDownLatch;
    private boolean shutdown;

    public FileFilter(@NonNull ProgressBar progressBar, @NonNull IStorage storage, @NonNull BlockingQueue<Path> inputQueue, @NonNull Collection<String> extensionsToScan) {
        this.progressBar = progressBar;
        this.storage = storage;
        this.inputQueue = inputQueue;
        this.extensionsToScan = extensionsToScan;

        outputQueue = new LinkedBlockingDeque<>(500);
        shutdown = false;
        countDownLatch = new CountDownLatch(1);
    }

    @Override
    public void run() {
        try {
            do {
                var file = inputQueue.poll(5, TimeUnit.SECONDS);
                if (Objects.nonNull(file)) {
                    if (processFile(file)) {
                        outputQueue.put(file);
                    } else {
                        progressBar.step();
                    }
                }
            }
            while (!shutdown || !inputQueue.isEmpty());
        } catch (InterruptedException e) {
            log.error("Error waiting for element", e);
        } finally {
            countDownLatch.countDown();
        }
    }

    private boolean processFile(@NonNull Path file) {
        try {
            if (storage.isUseless(file)) {
                return false;
            }

            if (isNotMedia(file) || Files.isHidden(file)) {
                storage.setUseless(file);
                return false;
            }
            return true;
        } catch (SQLException | IOException e) {
            log.error("Failed to filter file {}", file, e);
            return false;
        }
    }

    private boolean isNotMedia(@NonNull Path file) {
        var filename = file.getFileName().toString();
        var dotIndex = filename.lastIndexOf('.');

        if (dotIndex <= 0) {
            return true;
        }

        var extension = filename.substring(dotIndex + 1).toLowerCase();
        return !extensionsToScan.contains(extension);
    }
	
	@Override
	public void close(){
        shutdown = true;
		try{
			countDownLatch.await();
		}
		catch(InterruptedException e){
			log.info("Failed to wait for latch", e);
		}
    }
}
