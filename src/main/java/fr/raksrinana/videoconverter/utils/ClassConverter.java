package fr.raksrinana.videoconverter.utils;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public class ClassConverter extends BaseConverter<Class<?>>{
	public ClassConverter(String s){
		super(s);
	}
	
	@Override
	public Class<?> convert(String s){
		try{
			return Class.forName(s);
		}
		catch(ClassNotFoundException e){
			throw new ParameterException(this.getErrorString(s, "a class"));
		}
	}
}
