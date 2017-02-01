import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class motionAnalyzer {
	private ArrayList<Integer> breaks;
	private final int WIDTH = 480;
  private final int HEIGHT = 270;
  private final long frameByteSize = WIDTH * HEIGHT * 3;
  private String fileName;
  private InputStream is;
  private byte[] bytes;
  private double[][] pastFrameY;
  private double[][] currFrameY;
  private double THRESHOLD = 1.0;

	motionAnalyzer(String fileName, ArrayList<Integer> breaks){
		this.fileName = fileName;
		this.breaks = breaks;
	}

	public ArrayList<Double> analyzeFullscreenAverage(){
		File file;
		ArrayList<Double> movementPercents = new ArrayList<Double>();

		try{
			file = new File(fileName);
			is = new FileInputStream(file);
		  bytes = new byte[(int)frameByteSize];

			pastFrameY = new double[HEIGHT][WIDTH];
			currFrameY = new double[HEIGHT][WIDTH];

			double pastAverage = 0.0;
			double currAverage = 0.0;
			for(int i = 0; i < breaks.size()-1; i++){
				int frameStart = breaks.get(i);
				int frameEnd = breaks.get(i+1);
				int thresholdBreaks = 0;
				//System.out.println("BREAK " + frameStart + " TO " + frameEnd);
				boolean flag = true;

				readBytes(pastFrameY);
				pastAverage = createEntireFrameAvg(pastFrameY);

				for(int j = 1; j <frameEnd-frameStart; j++){
					if(flag){
			    		readBytes(currFrameY);
			    		currAverage = createEntireFrameAvg(currFrameY);
			    		if(Math.abs(pastAverage - currAverage) > THRESHOLD){
			    			thresholdBreaks++;
			    		}
			    		pastAverage = currAverage;
			    		copyBackToPast(currFrameY);
			    		//flag = !flag;
			    }
			    else {
			    	//flag = !flag;
			    	skipBytes(frameByteSize);
			    }
				}
				movementPercents.add((double)thresholdBreaks/(double)((frameEnd-frameStart)));
			}
		  is.close();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		return movementPercents;
	}

	public ArrayList<Double> analyzeBlockAverage(){
		File file;
		ArrayList<Integer> movementBreaks = new ArrayList<Integer>();
		ArrayList<Double> movementPerc = new ArrayList<Double>();

		try {
			file = new File(fileName);
			is = new FileInputStream(file);

		  bytes = new byte[(int)frameByteSize];

			pastFrameY = new double[HEIGHT][WIDTH];
			currFrameY = new double[HEIGHT][WIDTH];

			double[][] pastAverage = new double[HEIGHT/16][WIDTH/16];
			double[][] currAverage = new double[HEIGHT/16][WIDTH/16];

			for(int i = 0; i < breaks.size()-1; i++){
				int frameStart = breaks.get(i);
				int frameEnd = breaks.get(i+1);
				//System.out.println("BREAK " + frameStart + " TO " + frameEnd);
				boolean flag = true;

				int total = 0;
				readBytes(pastFrameY);
				pastAverage = createAverageBlocks(pastFrameY);

				for(int j = 1; j <frameEnd-frameStart; j++){
					if(flag){
			    		readBytes(currFrameY);
			    		currAverage = createAverageBlocks(currFrameY);
			    		total += breaksInBlocks(pastAverage,currAverage);
			    		copyBackAvg(pastAverage,currAverage);
			    		copyBackToPast(currFrameY);
			    		//flag = !flag;
			    }
			    else {
			    		//flag = !flag;
			    		skipBytes(frameByteSize);
			    }
				}
				movementBreaks.add(total);
			}
		  is.close();
		  //System.out.println("done");
		}
		catch(IOException e){
			e.printStackTrace();
		}
		double div = (double)Collections.max(movementBreaks);
		for(int i: movementBreaks){
			movementPerc.add((double)i / div);
		}
		return movementPerc;
	}

	private void copyBackToPast(double[][] newFrame){
		for(int i = 0; i < HEIGHT; i++){
			for(int j = 0; j < WIDTH; j++){
				pastFrameY[i][j] = newFrame[i][j];
			}
		}
	}

	private void copyBackAvg(double[][] past, double[][] curr){
		for(int i = 0; i < (HEIGHT/16); i++){
			for(int j = 0; j < (WIDTH/16); j++){
				past[i][j] = curr[i][j];
			}
		}
	}

	public double createEntireFrameAvg(double[][] frame){
		double avg = 0.0;
		for(int i = 0; i < HEIGHT; i++){
			for(int j = 0; j < WIDTH; j++){
				avg += frame[i][j];
			}
		}
		return avg/(HEIGHT*WIDTH);
	}

	private int breaksInBlocks(double[][] pastAvg, double[][] currAvg){
		int thresholdBreaks = 0;
		for(int i = 0; i < (HEIGHT/16); i++){
			for(int j = 0; j < (WIDTH/16); j++){
				if(Math.abs(pastAvg[i][j] - currAvg[i][j]) > THRESHOLD){
					thresholdBreaks++;
				}
			}
		}
		return thresholdBreaks;
	}

	private double[][] createAverageBlocks(double[][] frame){
		double[][] averageYforFrame = new double[HEIGHT/16][WIDTH/16];
		for(int iBlock = 0; iBlock < (HEIGHT/16); iBlock++){
			for(int jBlock = 0; jBlock < (WIDTH/16); jBlock++){
				 averageYforFrame[iBlock][jBlock] = averageBlock(iBlock,jBlock,frame);
			}
		}
		return averageYforFrame;
	}

	private double averageBlock(int iBlock, int jBlock, double[][] frame){
		double avg = 0.0;
		for(int i = 0; i < 16; i++){
			for(int j = 0; j < 16; j++){
				avg += frame[iBlock+i][jBlock+j];
			}
		}
		return avg/(16.0*16.0);
	}

	private void readBytes(double[][] frame) {
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
					int r = (int)bytes[ind] & 0xff;
					int g = (int)bytes[ind + HEIGHT * WIDTH] & 0xff;
					int b = (int)bytes[ind + HEIGHT * WIDTH * 2] & 0xff;

					frame[y][x] = .299 * (double)r + .587 * (double)g + .114 * (double)b;

					ind++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void skipBytes(long l){
		try {
			is.skip(l);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
