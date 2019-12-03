package fr.raksrinana.videoconverter;

import lombok.Getter;

@Getter
public class BatchProcessorResult{
	private long scanned;
	private long handled;
	private long created;
	private long errored;
	
	public BatchProcessorResult(long scanned, long handled, long created, long errored){
		this.scanned = scanned;
		this.handled = handled;
		this.created = created;
		this.errored = errored;
	}
	
	public static BatchProcessorResult newEmpty(){
		return new BatchProcessorResult(0, 0, 0, 0);
	}
	
	public static BatchProcessorResult newScanned(){
		return new BatchProcessorResult(1, 0, 0, 0);
	}
	
	public static BatchProcessorResult newHandled(){
		return new BatchProcessorResult(1, 1, 0, 0);
	}
	
	public static BatchProcessorResult newCreated(){
		return new BatchProcessorResult(1, 1, 1, 0);
	}
	
	public static BatchProcessorResult newErrored(){
		return new BatchProcessorResult(1, 1, 0, 1);
	}
	
	public BatchProcessorResult add(BatchProcessorResult processorResult){
		this.scanned += processorResult.getScanned();
		this.handled += processorResult.getHandled();
		this.created += processorResult.getCreated();
		this.errored += processorResult.getErrored();
		return this;
	}
}
