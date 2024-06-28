package fr.rakambda.mediaconverter;

import org.jetbrains.annotations.NotNull;
import java.io.Closeable;
import java.util.Queue;

public interface IProcessor extends Closeable, Runnable{
	void resume();
	
	void pause();
	
	@Override
	void close();
	
	@NotNull
	Queue<?> getOutputQueue();
}
