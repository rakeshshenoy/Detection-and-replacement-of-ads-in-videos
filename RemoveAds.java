import java.io.File;
import java.util.ArrayList;

import org.opencv.core.Core;

public class RemoveAds {
	public static void main(String[] args) {
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		String vFileName = "videoOutput.rgb";
		String aFileName = "audioOutput.wav";
		int flag = 1;
		/*audioAnalyze aa = new audioAnalyze(vFileName,aFileName);
		ArrayList<Integer> shots = new ArrayList<Integer>();
		shots = aa.calcAudioWeights();
		aa.writeVideo(shots);
		aa.writeAudio(shots);*/
		if(flag == 1)
		{
			File file = new File("videoOutput.rgb");
			detectLogos detectObj = new detectLogos(file, file.length()/(480*270*3));
		}
		//System.out.println("Summarization Complete!");
    }
}
