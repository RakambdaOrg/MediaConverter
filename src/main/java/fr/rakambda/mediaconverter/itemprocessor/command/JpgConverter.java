package fr.rakambda.mediaconverter.itemprocessor.command;

import fr.rakambda.mediaconverter.itemprocessor.CommandConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import java.nio.file.Path;

@Log4j2
public class JpgConverter extends CommandConverter{
	public JpgConverter(@NonNull Path input, @NonNull Path output, @NonNull Path temporary, boolean deleteInput, ProgressBarSupplier converterProgressBarSupplier){
		super(input, output, temporary, deleteInput, converterProgressBarSupplier);
	}
	
	@Override
	protected String[] getCommand(){
		return new String[]{
				"magick",
				"-quality",
				"85%",
				"-sampling-factor",
				"4:2:0",
				"-interlace",
				"JPEG",
				"-colorspace",
				"sRGB",
				getInput().toAbsolutePath().toString(),
				getTemporary().toString()
		};
	}
}
