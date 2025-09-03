package fr.rakambda.mediaconverter.ffmpeg;

import com.github.kokorin.jaffree.process.Stopper;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import org.jspecify.annotations.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

@Log4j2
@RequiredArgsConstructor
public class CustomStopper implements Stopper{
	private final Stopper stopper;
	private final Integer affinityMask;
	
	@Override
	public void graceStop(){
		stopper.graceStop();
	}
	
	@Override
	public void forceStop(){
		stopper.forceStop();
	}
	
	@Override
	public void setProcess(@NonNull Process process) {
		stopper.setProcess(process);
		
		if(Objects.nonNull(affinityMask) && affinityMask > 0){
			setAffinityMask(affinityMask, process.pid());
		}
	}
	
	private void setAffinityMask(int affinityMask, long pid){
		log.info("Setting affinity {} to pid {}", affinityMask, pid);
		var instance = Native.load("Kernel32", AffinityKernel.class);
		var result = instance.SetProcessAffinityMask(new WinNT.HANDLE(new Pointer(pid)), affinityMask);
		log.info("Set affinity result: {}", result);
	}
}
