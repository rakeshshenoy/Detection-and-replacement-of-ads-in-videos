import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class audioAnalyze {
	public audioAnalyze(String vName, String aName) {
	vFileName = vName;
	aFileName = aName;
    }

    public double sigmoid(double x)
    {
    	return (1/(1+Math.exp(-x)));
    }

    public ArrayList<Integer> calcAudioWeights() {

	// This is the original list of shot break numbers
	ArrayList<Integer> breaks = new ArrayList<Integer>();

	ArrayList<Double> motionWs = new ArrayList<Double>();
	ArrayList<Double> motionWs1 = new ArrayList<Double>();

	ArrayList<Double> audioWs = new ArrayList<Double>();

	// This is a sorted list of shots based on audio weight
	// List is organized as: BreakNum, Weight, BreakNum, Weight, etc...
	LinkedList<Double> weights = new LinkedList<Double>();

	// This is the final list of summary shots in temporal order
	ArrayList<Integer> finalShots = new ArrayList<Integer>();

	try {
	    File soundFile = new File(aFileName);
	    InputStream inputStream = new FileInputStream(soundFile);

	    InputStream waveStream = inputStream;
	    InputStream bufferedIn = new BufferedInputStream(waveStream);
	    AudioInputStream audioInputStream = null;
	    AudioFormat audioFormat = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
			audioFormat = audioInputStream.getFormat();
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	    long totFileBytes = soundFile.length(); 						// Total original audio file length in bytes
	    int numChan = audioFormat.getChannels();						// Number of Channels (= 1)
	    double bytesPerSample = audioFormat.getFrameSize();					// Number of bytes per sample (= 2)
	    double sampleRate = audioFormat.getFrameRate();					// Sample Rate (= 22050 Hz)
	    //double bytesPerSecond = sampleRate*bytesPerSample;					// Number bytes per second (= 44100 Hz)
	    long totNumSamples = (long)((totFileBytes-HEADER_LEN)/(bytesPerSample*numChan));	// Total original number of audio samples
	    long totNumBytes = (long)(totNumSamples*2);						// Total original number of audio bytes
	    long numSamplesReq = (long)(totNumSamples*(1-percent));  					// Number of Samples Required by percentage
	    long numBytesReq = (long)numSamplesReq*2;            					// Number of Bytes Required by percentage

	    bytesPerVidFrame = bytesPerSample*sampleRate*(1/FPS);
	    vidFramesPerByte = 1/bytesPerVidFrame;

	    int numFramesReq = (int)bytesToFrames(numBytesReq);
	    int totNumFrames = (int)bytesToFrames(totNumBytes);

	    System.out.println("Running videoSegment...");
	    videoSegment vs = new videoSegment(vFileName);
	    vs.analyze();
	    breaks = vs.getBreaks();

	    System.out.println("Shot Breaks: "+breaks);
	    System.out.println("Shot Breaks Size: "+breaks.size());

	    System.out.println("Running motionAnalyzer...");
	    motionAnalyzer ma = new motionAnalyzer(vFileName,breaks);
	    motionWs =  ma.analyzeFullscreenAverage();
	    motionWs1 = ma.analyzeBlockAverage();

	    System.out.println("Motion Weights: "+motionWs);
	    System.out.println("Motion Weights Size: "+motionWs.size());
	    double average = 0;
	    for(int i=0;i<motionWs.size();i++) {
		average+=motionWs.get(i);
	    }
	    System.out.println("Motion Weights Average Value: "+average/motionWs.size());
	    System.out.println("Motion Weights1: "+motionWs1);
	    System.out.println("Motion Weights1 Size: "+motionWs1.size());
	    average=0;
	    for(int i=0;i<motionWs1.size();i++) {
		average+=motionWs1.get(i);
	    }
	    System.out.println("Motion Weights1 Average Value: "+average/motionWs1.size());
	    System.out.println("AnalyzingAudio...");

	    audioWs = getAudioWeights(breaks, audioInputStream);
	    System.out.println("Audio Weights: "+audioWs);
	    System.out.println("Audio Weights Size: "+audioWs.size());
	    average=0;
	    for(int i=0;i<audioWs.size();i++) {
		average+=audioWs.get(i);
	    }
	    System.out.println("Audio Weights Average Value: "+average/audioWs.size());
	    double shotInd = 1;
	    double combWgt = 0;

	    for(int i=0;i<breaks.size()-1;i++) {
		// ***************************************
		// This is where the weights are combined.
		combWgt = (4.09412097)*motionWs.get(i) +  (59.77857273)*motionWs1.get(i) + (-5.52205249)*audioWs.get(i);
		//combWgt = (-4.43098625)*motionWs.get(i) +  (33.63771518)*motionWs1.get(i) + (-1.97505953)*audioWs.get(i);
	    //combWgt = (-1.76447298)*motionWs.get(i) +  (28.05220735)*motionWs1.get(i) + (-1.53014561)*audioWs.get(i);
		// ***************************************

		// If this is the first entry into the weighted list, just add shot number and weight to the list
		if(weights.size()==0) {
		    weights.add(shotInd);
		    weights.add(combWgt);
		    shotInd++;
		}
		// Else insert the shot number and weight so that the list is sorted from highest weight to lowest weight
		else {
		    // Sorts the LinkedList in order of highest audio weighted shots first
		    ListIterator<Double> iter = weights.listIterator();
		    for(int j=1;j<weights.size();j+=2) {
			if(iter.hasNext()) {
			    iter.next(); // First entry is the break number. Ignore it for the comparison.
			    if(combWgt>iter.next()) {
				weights.add(j-1,shotInd);
				weights.add(j,combWgt);
				j=weights.size();
			    }
			    // Add to the end if smallest value
			    else if(j==(weights.size()-1)) {
				weights.addLast(shotInd);
				weights.addLast(combWgt);
				j=weights.size();
			    }
			}
		    }
		    shotInd++;
		}
	    }
	    System.out.println("Combined Weights: "+weights);

	    // Calculate the required shot number based on required number of frames (based on the percentage)
	    // finalShots contains the required percentage of shot numbers based on the percentage
	    LinkedList<Integer> finalShotNums = new LinkedList<Integer>();
	    ListIterator<Double> iterW = weights.listIterator();
	    int[] adOrContent = new int[breaks.size()];

	    while(iterW.hasNext()) {
			double ind = iterW.next();
			double wgt = iterW.next(); // Shot weight unused at this point
			double val = sigmoid(wgt);
			// Sort the final list of shot numbers
		    if(val > 0.85)
		    {
		    	adOrContent[(int)ind] = 1;
				if(finalShotNums.size()==0) {
				    finalShotNums.add((int)ind);
				}
				else {
				    ListIterator<Integer> iterF = finalShotNums.listIterator();
				    for(int i=0;i<finalShotNums.size();i++) {
					if(iterF.hasNext()) {
					    if(ind<iterF.next()) {
						finalShotNums.add(i,(int)ind);
						i=finalShotNums.size();
					    }
					    else if(i==(finalShotNums.size()-1)) {
						finalShotNums.addLast((int)ind);
						i=finalShotNums.size();
					    }
					}
				    }
				}
		  	}
		  	else
		  		adOrContent[(int)ind] = 0;
		}
	    // Build the list of frames to write in the final summary
	    // List format: first frame of shot, second frame of shot, first frame of shot, second frame of shot, ...
	    finalShots = new ArrayList<Integer>();
	    ListIterator<Integer> iterF = finalShotNums.listIterator();

	    while(iterF.hasNext()) {
			int shotNum = iterF.next();
			int start = breaks.get(shotNum-1);
			int end = breaks.get(shotNum)-1;
			if(end - start > 60)
			{	
				finalShots.add(start);
				finalShots.add(end);
			}
			else
			{
				iterF.remove();
			}
	    }

	    iterF = finalShotNums.listIterator();
	    int prev = 0;
	    while(iterF.hasNext())
	   	{
	   		int curr = iterF.next();
	   		shotsInBetween.add(curr - prev - 1);
	   		prev = curr;
	   	}
	   	iterF = finalShotNums.listIterator();
	   	ListIterator<Integer> iterS = shotsInBetween.listIterator();
	   	int flag = 0;
	   	while(iterS.hasNext() && iterF.hasNext())
	   	{
	   		int diff = iterS.next();
	   		if(diff > 0 && diff < 4)
	   		{
	   			iterS.set(0);
	   			int first = iterF.next();
	   			int second = iterF.next();
	   			iterF.previous();
	   			while(first != second - 1)
	   			{
	   				iterF.add(++first);
	   				iterS.add(0);
	   			}
	   		}
	   		else
	   		{
	   			if(flag == 0)
	   				flag = 1;
	   			else
	   				iterF.next();
	   		}
	   	}

	    System.out.println("AnalyzingAudio...Complete");
	    System.out.print("Ad or Content: [");
	    for(int i = 0; i < breaks.size()-2; i++)
	    	System.out.print(adOrContent[i] + ", ");
	    System.out.println(adOrContent[breaks.size()-1] + "]");
	    System.out.println("Shots in Between: "+shotsInBetween);
	    System.out.println("Final Shot Frame Boundary Nums: "+finalShotNums);
	    System.out.println("Final Shot Frame Boundaries: "+finalShots);

	}
	catch(FileNotFoundException e) {
	    e.printStackTrace();
	    return null;
	}
	return finalShots;
    }

    public void buildHeader(OutputStream outputStream, long numBytes, AudioFormat audioFormat) {
	try {
	    outputStream.write(0x52); //R
	    outputStream.write(0x49); //I
	    outputStream.write(0x46); //F
	    outputStream.write(0x46); //F

	    long riffckSize = numBytes-8;

	    byte[] b = new byte[4];
	    for(int i=0;i<4;i++) {
		if(Math.floor(riffckSize/Math.pow(16, 6-(2*i)))!=0) {
		    b[i] = (byte)Math.floor(riffckSize/Math.pow(16, 6-(2*i)));
		    riffckSize -= Math.pow(16, 6-(2*i));
		}
		else {
		    b[i] = 0x00;
		}
		//System.out.println(b[i]+" ");
	    }
	    outputStream.write(b[3]);
	    outputStream.write(b[2]);
	    outputStream.write(b[1]);
	    outputStream.write(b[0]);

	    // 0x45564157
	    // WAVEID: "WAVE"
	    outputStream.write(0x57); //W
	    outputStream.write(0x41); //A
	    outputStream.write(0x56); //V
	    outputStream.write(0x45); //E

	    // 0x20746d66
	    // ckID: "fmt "
	    outputStream.write(0x66); //f
	    outputStream.write(0x6d); //m
	    outputStream.write(0x74); //t
	    outputStream.write(0x20); //'Space'

	    // 0x00000012
	    // cksize: 18
	    outputStream.write(0x12);
	    outputStream.write(0x00);
	    outputStream.write(0x00);
	    outputStream.write(0x00);

	    // 0x0001
	    // wFormatTag: WAVE_FORMAT_PCM
	    outputStream.write(0x01);
	    outputStream.write(0x00);

	    // 0x0001
	    // nChannels (Nc): 1 (mono)
	    int numChannels = audioFormat.getChannels();
	    for(int i=0;i<2;i++) {
		if(Math.floor(numChannels/Math.pow(16, 2-(2*i)))!=0) {
		    b[i] = (byte)Math.floor(numChannels/Math.pow(16, 2-(2*i)));
		    numChannels -= Math.pow(16, 2-(2*i));
		}
		else {
		    b[i] = 0x00;
		}
		//System.out.println(b[i]+" ");
	    }
	    outputStream.write(b[1]); // 0x01
	    outputStream.write(b[0]); // 0x00

	    // 0x00005622
	    // nSamplesPerSec (F): 22050 Hz
	    float sampleRate = audioFormat.getFrameRate();
	    for(int i=0;i<4;i++) {
		if(Math.floor(sampleRate/Math.pow(16, 6-(2*i)))!=0) {
		    b[i] = (byte)Math.floor(sampleRate/Math.pow(16, 6-(2*i)));
		    sampleRate -= Math.pow(16, 6-(2*i));
		}
		else {
		    b[i] = 0x00;
		}
		//System.out.println(b[i]+" ");
	    }
	    outputStream.write(b[3]); //0x22
	    outputStream.write(b[2]); //0x56
	    outputStream.write(b[1]); //0x00
	    outputStream.write(b[0]); //0x00

	    // 0x0000ac44
	    // nAvgBytesPerSec (F*M*Nc): 44100 Hz
	    float bytesPerSec = audioFormat.getFrameRate()*audioFormat.getFrameSize()*audioFormat.getChannels();
	    for(int i=0;i<4;i++) {
		if(Math.floor(bytesPerSec/Math.pow(16, 6-(2*i)))!=0) {
		    b[i] = (byte)Math.floor(bytesPerSec/Math.pow(16, 6-(2*i)));
		    bytesPerSec -= Math.pow(16, 6-(2*i));
		}
		else {
		    b[i] = 0x00;
		}
		//System.out.println(b[i]+" ");
	    }
	    outputStream.write(b[3]); //0x44
	    outputStream.write(b[2]); //0xac
	    outputStream.write(b[1]); //0x00
	    outputStream.write(b[0]); //0x00

	    // 0x0002
	    // nBlockAlign (M*Nc): 2 bytes/frame
	    int bytesPerFrame = audioFormat.getFrameSize();
	    for(int i=0;i<2;i++) {
		if(Math.floor(bytesPerFrame/Math.pow(16, 2-(2*i)))!=0) {
		    b[i] = (byte)Math.floor(bytesPerFrame/Math.pow(16, 2-(2*i)));
		    bytesPerFrame -= Math.pow(16, 2-(2*i));
		}
		else {
		    b[i] = 0x00;
		}
		//System.out.println(b[i]+" ");
	    }
	    outputStream.write(b[1]); //0x02
	    outputStream.write(b[0]); //0x00

	    // 0x0010
	    // wBitsPerSample (8*M): 16 bits/sample
	    int bitsPerSample = 8*audioFormat.getFrameSize();
	    for(int i=0;i<2;i++) {
		if(Math.floor(bitsPerSample/Math.pow(16, 2-(2*i)))!=0) {
		    b[i] = (byte)Math.floor(bitsPerSample/Math.pow(16, 2-(2*i)));
		    bitsPerSample -= Math.pow(16, 2-(2*i));
		}
		else {
		    b[i] = 0x00;
		}
		//System.out.println(b[i]+" ");
	    }
	    outputStream.write(0x10); //0x10
	    outputStream.write(0x00); //0x00

	    // 0x0000
	    // cbSize: 0
	    outputStream.write(0x00);
	    outputStream.write(0x00);

	    // 0x61746164
	    // ckID: "data"
	    outputStream.write(0x64); //d
	    outputStream.write(0x61); //a
	    outputStream.write(0x74); //t
	    outputStream.write(0x61); //a

	    // 0x........ Hex value dependent on total file size
	    // cksize: M*Nc*Ns
	    //	M = bytes/sample (= 2 in our wav files)
	    //  Nc = Number of Channels (= 1 in our wav files)
	    //  Ns = Number of Samples
	    //	0 or 1: padding byte at end if M*Nc*Ns is odd
	    // Can calculate by taking total file size in bytes - headerLen
	    long datackSize = numBytes-HEADER_LEN;

	    // Calculate the little endian byte pair equivalent of data chunk size
	    //byte[] b = new byte[4];
	    for(int i=0;i<4;i++) {
		if(Math.floor(datackSize/Math.pow(16, 6-(2*i)))!=0) {
		    b[i] = (byte)Math.floor(datackSize/Math.pow(16, 6-(2*i)));
		    datackSize -= Math.pow(16, 6-(2*i));
		}
		else {
		    b[i] = 0x00;
		}
		//System.out.println(b[i]+" ");
	    }
	    outputStream.write(b[3]);
	    outputStream.write(b[2]);
	    outputStream.write(b[1]);
	    outputStream.write(b[0]);
	}
	catch (IOException e1) {
		return;
	}
    }

    public long bytesToFrames(double bytes) {
	return Math.round(bytes*vidFramesPerByte);
    }

    public long framesToBytes(double frames) {
	return (long)(frames*bytesPerVidFrame);
    }

    public ArrayList<Double> getAudioWeights(ArrayList<Integer> breaks, AudioInputStream audioInputStream) {
	ArrayList<Double> aWeights = new ArrayList<Double>();
	int readBytes = 0;
	byte[] audioBuffer = new byte[EXTERNAL_BUFFER_SIZE];
	long byteCount = 0;
	int index = 1;
	double wgtTotal = 0;
	int wgtCount = 0;
	double max = Double.NEGATIVE_INFINITY;
	long breakPoint = framesToBytes(breaks.get(index));
	index++;
	try {
	    // boolean last = false;

	    // While there are still bytes to read from the input stream
	    while(readBytes != -1) {
		readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length);

		// More bytes to read from the audio input stream
		if(readBytes >= 0) {

		    // Scan through only the MSBs of the buffer (every other odd byte)
		    for(int i=1;i<readBytes;i+=2) {

			// If we have not reached the break point yet, keep summing for the average calculation
			if(byteCount<breakPoint) {
			    wgtTotal+=audioBuffer[i];
			    wgtCount++;
			}

			// At the breakpoint, calculate the average for the previous shot and reset the average weight counters for the next shot
			else {
			 //   System.out.print("index: "+index);
			   // System.out.print(" break: "+breakPoint);
			    //System.out.println(" add: "+wgtTotal/wgtCount);
			    aWeights.add(wgtTotal/wgtCount);
			    wgtTotal = 0;
			    wgtTotal+=audioBuffer[i];
			    wgtCount = 0;
			    wgtCount++;
			    //index++;
			    if(index<breaks.size()) {
				breakPoint=framesToBytes(breaks.get(index));
				index++;
			    }
			    else {
				readBytes = -1;
			    }
			}
			byteCount+=2;
		    }
		}
	    }

	    while(aWeights.size()<breaks.size()-1) {
		aWeights.add(wgtTotal/wgtCount);
	    }

	    // Normalize the audio weights
	    for(int i=0;i<aWeights.size();i++) {
		if(aWeights.get(i)>max) {
		    max = aWeights.get(i);
		}
	    }
	    for(int i=0;i<aWeights.size();i++) {
		double val = aWeights.get(i);
		aWeights.set(i, max/val);
	    }
	}
	catch (IOException e1) {
	    return null;
	}
	return aWeights;
    }

    public void writeAudio(ArrayList<Integer> shots) {
	try {
	    File soundFile = new File(aFileName);
	    InputStream inputStream = new FileInputStream(soundFile);
	    InputStream waveStream = inputStream;
	    InputStream bufferedIn = new BufferedInputStream(waveStream);
	    AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
	    AudioFormat audioFormat = audioInputStream.getFormat();

	    long totFileBytes = soundFile.length(); 						// Total original audio file length in bytes
	    int numChan = audioFormat.getChannels();						// Number of Channels (= 1)
	    double bytesPerSample = audioFormat.getFrameSize();					// Number of bytes per sample (= 2)
	    double sampleRate = audioFormat.getFrameRate();					// Sample Rate (= 22050 Hz)
	    double bytesPerSecond = sampleRate*bytesPerSample;					// Number bytes per second (= 44100 Hz)
	    long totNumSamples = (long)((totFileBytes-HEADER_LEN)/(bytesPerSample*numChan));	// Total original number of audio samples
	    long totNumBytes = (long)(totNumSamples*2);						// Total original number of audio bytes

	    bytesPerVidFrame = bytesPerSample*sampleRate*(1/FPS);
	    vidFramesPerByte = 1/bytesPerVidFrame;

	    OutputStream outputStream = new FileOutputStream("audioOutput.wav");
	    int count=0;
	    long firstByte=0;
	    long lastByte=0;
	    if(shots.size()!=0) {
		firstByte= framesToBytes(shots.get(count));
		lastByte = framesToBytes(shots.get(count+1));
		count+=2;
	    }

	    // Calculate final number of audio bytes in the summary
	    long totalBytes = 0;
	    for(int i=0;i<shots.size()-1;i+=2) {
		totalBytes+=framesToBytes(shots.get(i+1)-shots.get(i));
	    }

	    buildHeader(outputStream,totalBytes,audioFormat);

	    long currByte = 0;
	    int readBytes = 0;
	    byte[] audioBuffer = new byte[EXTERNAL_BUFFER_SIZE];
	    System.out.println("Writing Audio...0%");
	    while (readBytes != -1) {
		readBytes = audioInputStream.read(audioBuffer, 0, audioBuffer.length);
		if(readBytes >= 0) {
		    for(int i=1;i<readBytes;i+=2) { // Each sample is 2 bytes

			if((currByte>lastByte)&&(count<shots.size())) {
			    System.out.println("Writing Audio..."+Math.round(100*(double)currByte/(double)totNumBytes)+"%");
			    firstByte= framesToBytes(shots.get(count));
			    lastByte = framesToBytes(shots.get(count+1));
			    count+=2;
			}

			if((currByte>=firstByte)&&(currByte<=lastByte)) {
			    outputStream.write(audioBuffer[i-1]);
			    outputStream.write(audioBuffer[i]);
			}

			if((count>=shots.size())&&(currByte>lastByte)) {
			    readBytes=-1;
			}
			currByte+=2;
		    }
		}
	    }

	    // Print the total time of the compressed file
	    int minutes = (int)Math.floor((double)totalBytes/(bytesPerSecond*60.0));
	    int seconds = (int)Math.floor(((double)totalBytes/bytesPerSecond)-60.0*minutes);
	    if(seconds<10) {
		System.out.println(minutes+":0"+seconds);
	    }
	    else {
		System.out.println(minutes+":"+seconds);
	    }
	}
	catch(FileNotFoundException e) {
	    e.printStackTrace();
	    return;
	}
	catch (UnsupportedAudioFileException e1) {
	    return;
	}
	catch (IOException e1) {
	    return;
	}
    }

    public void writeVideo(ArrayList<Integer> shots) {
	try {
	    File file = new File(vFileName);
	    InputStream is = new FileInputStream(file);
	    long len = WIDTH*HEIGHT*3;
	    long numFrames = file.length()/len;
	    byte[] bytes = new byte[(int)len];

	    OutputStream vidOutputStream = new FileOutputStream("videoOutput.rgb");
	    int count=0;
	    int firstFrame=0;
	    int lastFrame=0;
	    if(shots.size()!=0) {
		firstFrame = shots.get(count);
		lastFrame = shots.get(count+1);
		count+=2;
	    }
	    //System.out.println("Writing Video...0%");
	    for(int i=0;i<numFrames;i++) {
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
		    offset += numRead;
		}

		// Update first/last frame pointers if there are more shots to be written
		if((i>lastFrame)&&(count<shots.size())) {
		    System.out.println("Writing Video..."+Math.round(100*(double)i/(double)numFrames)+"%");
		    firstFrame = shots.get(count);
		    lastFrame = shots.get(count+1);
		    count+=2;
		}

		// Write all frames in the relevant shot
		if((i>=firstFrame)&&(i<=lastFrame)) {
		    vidOutputStream.write(bytes);
		}

		// We are finished if we've written all shots
		if((count>=shots.size())&&(i>lastFrame)) {
		    i=(int)numFrames;
		}
	    }
	    vidOutputStream.flush();
	    vidOutputStream.close();
	}
	catch (IOException e) {
	    e.printStackTrace();
	}

    }

    public ArrayList<Integer> shotsInBetween = new ArrayList<Integer>();
    private final int HEADER_LEN = 46;
    private String vFileName;
    private String aFileName;
    private double percent;
    private double bytesPerVidFrame;
    private double vidFramesPerByte;
    private final int WIDTH = 480;
    private final int HEIGHT = 270;
    //private final double FPS = 23.976; // Frames Per Second
    private final double FPS = 30; // Frames Per Second
    private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
}
