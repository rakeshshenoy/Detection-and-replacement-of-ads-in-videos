import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.*;
import javax.swing.*;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import Image.Image;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.highgui.Highgui;

public class detectLogos {
	public detectLogos(File file, long numFrames){
		System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
		this.numFrames = numFrames;
		frameCounter=0;
		imgDB=new Image();
		try {
		    //File file = new File(fileName);
		    is = new FileInputStream(file);
	
		    long len = WIDTH*HEIGHT*3;
	
		    bytes = new byte[(int)len];
		    while(frameCounter < numFrames)
		    	readBytes();
		} 
		catch (IOException e) {
		    e.printStackTrace();
		}
    }

    private  void readBytes() {
	frameCounter++;
	try {
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
		offset += numRead;
	    }
	    int ind = 0;
	    for(int y = 0; y < HEIGHT; y++){
		for(int x = 0; x < WIDTH; x++){
		    byte r = bytes[ind];
		    byte g = bytes[ind+HEIGHT*WIDTH];
		    byte b = bytes[ind+HEIGHT*WIDTH*2]; 

		    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
		    imgDB.imgI.setRGB(x,y,pix);
		    ind++;
		} 
	    }
	    
	} 
	catch (IOException e) {
	    e.printStackTrace();
	}
	if(frameCounter % 30 == 0)
	{
		System.out.println("Frame=" + frameCounter);      

        Image imQ = new Image();
        Image imQL2;
        Image imQL3;
        Image imQL4;
        Image imResL2;
        Image imResL3;
        Image imResL4;
        //double imResL2, imResL3, imResL4;
                
        //Init Functions 
        for(int ind = 0; ind < 1; ind++)
        {	
	        imQ.ReadImage(logos[ind], 270, 480);
	        if(!logos[ind].equals("subway_logo.rgb"))
	        {
		        Image cropped = new Image();
		        cropped.imgI = new BufferedImage(240, 270, BufferedImage.TYPE_INT_RGB);
		        for(int i = 0; i < 270; i++)
		        	for(int j = 120; j< 360; j++)
		        		cropped.imgI.setRGB(j-120, i, imQ.imgI.getRGB(j, i)); 
		        imQ = cropped;
	        }
	        
	        //Resample and scale down       
	        //imQL1 = imQ.Pyramidal_Resampling(1, 1, 480, 270);
	        imQL2 = imQ.Pyramidal_Resampling(2, 2, imQ.imgI.getWidth(), imQ.imgI.getHeight());
	        imQL3 = imQ.Pyramidal_Resampling(3, 3, imQ.imgI.getWidth(), imQ.imgI.getHeight());
	        imQL4 = imQ.Pyramidal_Resampling(4, 4, imQ.imgI.getWidth(), imQ.imgI.getHeight());
	        //imQL5 = imQ.Pyramidal_Resampling(5, 5, 480, 270);
	        
	        //Template Matching
	        //imResL1 = rg.TemplateMatching(imQL1, imgDB, Imgproc.TM_CCORR);
	        imResL2 = TemplateMatching(imQL2, imgDB, Imgproc.TM_CCOEFF_NORMED, logos[ind]);
	        imResL3 = TemplateMatching(imQL3, imgDB, Imgproc.TM_CCOEFF_NORMED, logos[ind]);
	        imResL4 = TemplateMatching(imQL4, imgDB, Imgproc.TM_CCOEFF_NORMED, logos[ind]);
	        //imResL5 = rg.TemplateMatching(imQL5, imgDB, Imgproc.TM_CCORR);
	
	        try{
	        	if(imResL2 != null)
	        	{
	        		File outputfile2 = new File("image2" + frameCounter + ".jpg");
	        		ImageIO.write(imResL2.imgI, "jpg", outputfile2);
	        	}
	        	if(imResL3 != null)
	        	{
	        		File outputfile3 = new File("image3" + frameCounter + ".jpg");
	        		ImageIO.write(imResL3.imgI, "jpg", outputfile3);
	        	}
	        	if(imResL4 != null)
	        	{
	        		File outputfile4 = new File("image4" + frameCounter + ".jpg");
	        		ImageIO.write(imResL4.imgI, "jpg", outputfile4);
	        	}
	        }
	        catch(IOException e){
	        	
	        }
	        //if(imResL2 > 0 || imResL3 > 0 || imResL4 > 0)
	        //	logosFound[ind] = 1;
        }
    	}
	
    } 
    
    private Image TemplateMatching (Image imQuery, Image imDB, int match_method, String logoName)
    {
    	double threshold;
    	if(logoName.equals("subway_logo.rgb"))
    		threshold = 0.18;
    	else if(logoName.equals("starbucks_logo2.rgb"))
    		threshold = 0.25;
    	else if(logoName.equals("nfl_logo.rgb"))
    		threshold = 0.37;
    	else if(logoName.equals("Mcdonalds_logo.rgb"))
    		threshold = 0.18;
    	else
    		threshold = 0.37;
    	
        //System.out.println("Running Template Matching ...");
        Mat matQuery = imQuery.Image3CtoMat_CV();
        Mat matDB = imDB.Image3CtoMat_CV();
        Mat hsvQ = new Mat(), hsvDB = new Mat();

        Imgproc.cvtColor(matQuery, hsvQ, Imgproc.COLOR_RGB2HSV);
        Imgproc.cvtColor(matDB, hsvDB, Imgproc.COLOR_RGB2HSV);
        
        // Create result image matrix
        int resultImg_cols = matDB.cols() - matQuery.cols() + 1;
        int resultImg_rows = matDB.rows() - matQuery.rows() + 1;

        Mat matRes = new Mat(resultImg_rows, resultImg_cols, CvType.CV_32FC1);

        // Template Matching with Normalization
        Imgproc.matchTemplate(hsvDB, hsvQ, matRes, match_method);
        //Imgproc.threshold(matRes, matRes, threshold, 1, 3);
        //Core.normalize(matRes, matRes, 0, 1, Core.NORM_MINMAX, -1, new Mat());
       
        // / Localizing the best match with minMaxLoc
        Core.MinMaxLocResult Location_Result = Core.minMaxLoc(matRes);
        //System.out.println(Location_Result.minVal + " " + Location_Result.maxVal);
        System.out.println("maxVal=" + Location_Result.maxVal);
        if(Location_Result.maxVal > 0)
        {
        	//System.out.println("maxVal=" + Location_Result.maxVal);
        	Point matchLocation;

	        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED)
	        {
	            matchLocation = Location_Result.minLoc;
	        }
	        else
	        {
	            matchLocation = Location_Result.maxLoc;
	        }
	
	        // Display Area by Rectangle
	        Core.rectangle(matDB, matchLocation, new Point(matchLocation.x + matQuery.cols(), matchLocation.y + matQuery.rows()), new Scalar(0, 255, 0));
	
	        Image imOut = new Image(matDB.width(), matDB.height());
	        imOut.Mat_CVtoImage3C(matDB);
	
	        return imOut;
        }
        else
        	return null;
    }
    
    public int[] logosFound = new int[4];
    private String[] logos = {"starbucks_logo2.rgb"};
    private long frameCounter;
    private String fileName;
    private long numFrames; 
    private final int WIDTH = 480;
    private final int HEIGHT = 270;
    private final double FPS = 30;
    private InputStream is;
    Image imgDB;
    private byte[] bytes;
}
