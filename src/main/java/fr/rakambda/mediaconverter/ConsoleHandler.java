package fr.rakambda.mediaconverter;

import fr.rakambda.mediaconverter.mediaprocessor.MediaProcessorTask;
import fr.rakambda.mediaconverter.utils.Continue;
import lombok.extern.log4j.Log4j2;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;

/**
 * Handles commands sent in the standard input.
 */
@Log4j2
class ConsoleHandler extends Thread implements AutoCloseable{
	private static final int WAIT_DELAY = 10000;
	private final Continue aContinue;
	private final Collection<ExecutorService> executorServices;
	private final Collection<MediaProcessorTask> tasks;
	private boolean stop;
	
	ConsoleHandler(Continue aContinue){
		super();
		this.aContinue = aContinue;
		this.executorServices = new LinkedList<>();
		this.tasks = new LinkedList<>();
		
		stop = false;
		setDaemon(true);
		setName("Console watcher");
		log.info("Console handler created");
	}
	
	@Override
	public void run(){
		log.info("Console handler started");
		try(var sc = new Scanner(System.in)){
			while(!stop){
				try{
					if(!sc.hasNext()){
						try{
							Thread.sleep(WAIT_DELAY);
						}
						catch(InterruptedException ignored){
						}
						continue;
					}
					var line = sc.nextLine();
					var args = new LinkedList<>(Arrays.asList(line.split(" ")));
					if(args.isEmpty()){
						continue;
					}
					var command = args.poll();
					if("q".equals(command)){
						log.info("Exiting");
						this.executorServices.forEach(ExecutorService::shutdownNow);
						this.tasks.forEach(MediaProcessorTask::cancel);
					}
					else if("p".equals(command)){
						log.info("Pausing");
						aContinue.pause();
					}
					else if("r".equals(command)){
						log.info("Resuming");
						aContinue.resume();
					}
				}
				catch(Exception e){
					log.warn("Error executing console command", e);
				}
			}
		}
	}
	
	/**
	 * Close the console handler.
	 */
	@Override
	public void close(){
		stop = true;
	}
	
	public void registerExecutor(ExecutorService executor){
		executorServices.add(executor);
	}
	
	public void registerTasks(ConcurrentLinkedDeque<MediaProcessorTask> converters){
		tasks.addAll(converters);
	}
}
