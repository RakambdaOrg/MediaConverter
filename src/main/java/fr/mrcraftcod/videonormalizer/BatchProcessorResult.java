package fr.mrcraftcod.videonormalizer;

public class BatchProcessorResult{
	public static final BatchProcessorResult EMPTY = new BatchProcessorResult(0, 0, 0);
	public static final BatchProcessorResult SCANNED_1 = new BatchProcessorResult(1, 0, 0);
	public static final BatchProcessorResult HANDLED_1 = new BatchProcessorResult(1, 1, 0);
	public static final BatchProcessorResult CREATED_1 = new BatchProcessorResult(1, 1, 1);
	private int scanned;
	private int handled;
	private int created;
	
	public BatchProcessorResult(int scanned, int handled, int created){
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
	
	public int getCreated(){
		return created;
	}
	
	public int getHandled(){
		return handled;
	}
	
	public int getScanned(){
		return scanned;
	}
}
