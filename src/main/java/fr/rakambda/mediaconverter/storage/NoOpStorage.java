package fr.rakambda.mediaconverter.storage;

import java.nio.file.Path;

public class NoOpStorage implements IStorage{
	@Override
	public void close(){
	}
	
	@Override
	public boolean isUseless(Path path){
		return false;
	}
	
	@Override
	public void setUseless(Path path){
	}
	
	@Override
	public void save(){
	}
}
