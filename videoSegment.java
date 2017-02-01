import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class videoSegment {

	videoSegment(String fileName){
		this.fileName = fileName;
	}

	public void analyze(){

		File file;

		breaks = new ArrayList<Integer>();
		try{
			file = new File(fileName);
			is = new FileInputStream(file);

			long frameByteSize = WIDTH*HEIGHT*3;
		    long numFrames = file.length()/frameByteSize;


		    bytes = new byte[(int)frameByteSize];
		    histogramPrev = new int[4][4][4];
		    histogramNext = new int[4][4][4];


			/* read in the first frame to seed the process */
		    readBytes(histogramPrev);
			breaks.add(0);

		    for(int i = 1; i < numFrames-1; i ++){

		    		clearHistogramNext();
		    		readBytes(histogramNext);
		    		double val = SDvalue();
		    		val = val / (WIDTH*HEIGHT);
		    		val *= 100;

					/*if the SDValue is greater than the threshold
					  and is
					*/
					//System.out.println(val);
		    		if(val > THRESHOLD){
		    			breaks.add(i);
		    		}
		    		copyHistogramBack();
		    }
		    is.close();
		    breaks.add((int)numFrames);

		}
		catch(IOException e){
			e.printStackTrace();
		}


	}

	public ArrayList<Integer> getBreaks(){
		return breaks;
	}

	public void printBreaks(){
		for(Integer i : breaks){
			System.out.println("BREAK AT: " + i);
		}
	}

	private void readBytes(int[][][] histogram) {
		try {
			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length
					&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
						offset += numRead;
			}
			int ind = 0;
			for (int y = 0; y < HEIGHT; y++) {
				for (int x = 0; x < WIDTH; x++) {
					byte r = bytes[ind];
					byte g = bytes[ind + HEIGHT * WIDTH];
					byte b = bytes[ind + HEIGHT * WIDTH * 2];

					/* extracting the 2 most significant bits
						and filling the histogram bins*/
					int ri = (int)( (r & 0xff) &0xC0);
					int gi = (int)( (g & 0xff) &0xC0);
					int bi = (int)( (b & 0xff) &0xC0);

					histogram[ri/64][gi/64][bi/64]++;

					ind++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//save the histogram as the previous histogram
	private void copyHistogramBack(){
		for(int i = 0; i < 4; i++)
			for(int j = 0; j < 4; j++)
				for(int k = 0; k < 4; k++){
					histogramPrev[i][j][k] = histogramNext[i][j][k];
				}
	}

	//clears HistogramNext
	private void clearHistogramNext(){
		for(int i = 0; i < 4; i++)
			for(int j = 0; j < 4; j++)
				for(int k = 0; k < 4; k++){
					histogramNext[i][j][k] = 0;
				}
	}


	private double SDvalue(){
		int sum = 0;
		for(int i = 0; i < 4; i++)
			for(int j = 0; j < 4; j++)
				for(int k = 0; k < 4; k++){
					sum += Math.abs(histogramPrev[i][j][k] - histogramNext[i][j][k]);
				}
		return ((double)sum);
	}

	ArrayList<Integer> breaks;
	private final int THRESHOLD = 25;
	private int[][][] histogramPrev;
	private int[][][] histogramNext;
	private final int WIDTH = 480;
	private final int HEIGHT = 270;
	private String fileName;
	private InputStream is;
	private byte[] bytes;
}
