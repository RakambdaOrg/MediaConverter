package fr.rakambda.mediaconverter.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

//https://stackoverflow.com/a/16208043/3281185
public class Continue{
	private final ReentrantLock pauseLock;
	private final Condition unpaused;
	private boolean isPaused;
	
	public Continue(){
		pauseLock = new ReentrantLock();
		unpaused = pauseLock.newCondition();
	}
	
	public void checkIn() throws InterruptedException{
		if(isPaused){
			pauseLock.lock();
			try{
				while(isPaused){
					unpaused.await();
				}
			}
			finally{
				pauseLock.unlock();
			}
		}
	}
	
	public void checkInUntil(Date deadline) throws InterruptedException{
		if(isPaused){
			pauseLock.lock();
			try{
				while(isPaused){
					unpaused.awaitUntil(deadline);
				}
			}
			finally{
				pauseLock.unlock();
			}
		}
	}
	
	public void checkIn(long nanosTimeout) throws InterruptedException{
		if(isPaused){
			pauseLock.lock();
			try{
				while(isPaused){
					unpaused.awaitNanos(nanosTimeout);
				}
			}
			finally{
				pauseLock.unlock();
			}
		}
	}
	
	public void checkIn(long time, TimeUnit unit) throws InterruptedException{
		if(isPaused){
			pauseLock.lock();
			try{
				while(isPaused){
					unpaused.await(time, unit);
				}
			}
			finally{
				pauseLock.unlock();
			}
		}
	}
	
	public void checkInUninterruptibly(){
		if(isPaused){
			pauseLock.lock();
			try{
				while(isPaused){
					unpaused.awaitUninterruptibly();
				}
			}
			finally{
				pauseLock.unlock();
			}
		}
	}
	
	public boolean isPaused(){
		return isPaused;
	}
	
	public void pause(){
		pauseLock.lock();
		try{
			isPaused = true;
		}
		finally{
			pauseLock.unlock();
		}
	}
	
	public void resume(){
		pauseLock.lock();
		try{
			if(isPaused){
				isPaused = false;
				unpaused.signalAll();
			}
		}
		finally{
			pauseLock.unlock();
		}
	}
}
