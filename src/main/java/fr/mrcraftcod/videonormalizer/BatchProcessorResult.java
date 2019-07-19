package fr.mrcraftcod.videonormalizer;

public class BatchProcessorResult{
	private long scanned;
	private long handled;
	private long created;
	
	public BatchProcessorResult(long scanned, long handled, long created){
		this.scanned = scanned;
		this.handled = handled;
		this.created = created;
	}
	
	public BatchProcessorResult add(BatchProcessorResult processorResult){
		this.scanned += processorResult.getScanned();
		this.handled += processorResult.getHandled();
		this.created += processorResult.getCreated();
		return this;
	}
	
	public long getCreated(){
		return created;
	}
	
	public long getHandled(){
		return handled;
	}
	
	public long getScanned(){
		return scanned;
	}
	
	public static BatchProcessorResult newEmpty(){
		return new BatchProcessorResult(0, 0, 0);
	}
	
	public static BatchProcessorResult newScanned(){
		return new BatchProcessorResult(1, 0, 0);
	}
	
	public static BatchProcessorResult newHandled(){
		return new BatchProcessorResult(1, 1, 0);
	}
	
	public static BatchProcessorResult newCreated(){
		return new BatchProcessorResult(1, 1, 1);
	}
}
