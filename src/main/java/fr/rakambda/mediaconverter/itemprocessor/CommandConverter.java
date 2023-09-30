package fr.rakambda.mediaconverter.itemprocessor;

import fr.rakambda.mediaconverter.progress.ProgressBarHandle;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

@Log4j2
public abstract class CommandConverter extends ConverterRunnable{
	private final ProgressBarSupplier converterProgressBarSupplier;
	
	private ProgressBarHandle progressBar;
	private Process process;
	
	protected CommandConverter(@NonNull Path input, @NonNull Path output, @NonNull Path temporary, boolean deleteInput, ProgressBarSupplier converterProgressBarSupplier){
		super(input, output, temporary, deleteInput);
		this.converterProgressBarSupplier = converterProgressBarSupplier;
	}
	
	@Override
	protected void convert(@NonNull ExecutorService executorService) throws Exception{
		log.info("Converting {} to {}", getInput(), getOutput());
		progressBar = converterProgressBarSupplier.get();
		
		ProcessBuilder builder = new ProcessBuilder(getCommand());
		executorService.submit(() -> {
			try{
				progressBar.getProgressBar().stepTo(0);
				progressBar.getProgressBar().setExtraMessage(getOutput().getFileName().toString());
				progressBar.getProgressBar().maxHint(1);
				
				process = builder.start();
				process.onExit()
						.thenAccept(p -> progressBar.getProgressBar().stepTo(1))
						.thenAccept(p -> close());
				process.waitFor();
			}
			catch(Exception e){
				log.error("Failed to convert", e);
			}
		});
	}
	
	@Override
	public void cancel(){
		if(Objects.nonNull(process)){
			process.destroy();
		}
	}
	
	@Override
	public void close(){
		if(Objects.nonNull(progressBar)){
			progressBar.close();
		}
	}
	
	protected abstract String[] getCommand();
}
