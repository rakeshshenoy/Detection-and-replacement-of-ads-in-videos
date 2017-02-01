package Image;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.System.exit;
import javax.swing.*;
import org.opencv.core.*;
import org.opencv.core.Core;
import static org.opencv.core.CvType.CV_32SC3;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.Math.*;
import java.util.Scanner;
import java.util.Timer;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.Math.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public final class Image
{
    public String strName;
    public String strAlpha;
    public BufferedImage imgI;
    public int iW, iH;
    public boolean bAlpha;

    public Image()
    {
        this.strName = null;
        this.imgI = new BufferedImage(480, 270, BufferedImage.TYPE_INT_RGB);
        this.bAlpha = false;
    }
    
    public Image(int iW, int iH)
    {
        this.iH = iH;
        this.iW = iW;

        this.imgI = new BufferedImage(iW, iH, BufferedImage.TYPE_INT_RGB);

        this.strAlpha = null;
        this.strName = null;
        this.bAlpha = false;
    }

    public void ReadImage(String strName, int iH, int iW)
    {
        this.imgI = new BufferedImage(iW, iH, BufferedImage.TYPE_INT_RGB);

        try
        {
            File file = new File(strName);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int) len];

            int offset = 0;
            int numRead = 0;

            while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)
            {
                offset += numRead;
            }

            int ind = 0;

            for (int y = 0; y < iH; y++)
            {
                for (int x = 0; x < iW; x++)
                {
                    //byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind + iH * iW];
                    byte b = bytes[ind + iH * iW * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    this.imgI.setRGB(x, y, pix);
                    ind++;
                    //System.out.println("Ind: " + ind + " X: " + x + " Y: " + y);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public Mat Image1CtoMat_CV()
    {
        float[] buff = new float[this.imgI.getWidth() * this.imgI.getHeight()];

        //System.out.println("Color: " + (this.imgI.getRGB(0, 0) & 0x000000FF));

        for(int i = 0;i<this.imgI.getWidth();i++)
        {
            for(int j = 0;j<this.imgI.getHeight();j++)
            {
                int iBW;

                iBW = this.imgI.getRGB(i, j) & 0x000000FF;

                buff[j * this.imgI.getWidth()+ i] = (float)(iBW);
            }
        }

        Mat mat = new Mat(this.imgI.getHeight(), this.imgI.getWidth(), CvType.CV_32FC1);

        mat.put(0,0, buff);

        return mat;
    }

    public Mat Image3CtoMat_CV()
    {
        float[] buff = new float[this.imgI.getWidth() * this.imgI.getHeight() * 3];

        for(int i = 0;i<this.imgI.getWidth();i++)
        {
            for(int j = 0;j<this.imgI.getHeight();j++)
            {
                buff[j*this.imgI.getWidth()*3 + i*3 + 0] = (float) ((this.imgI.getRGB(i, j) >> 16) & 0xFF);
                buff[j*this.imgI.getWidth()*3 + i*3 + 1] = (float) ((this.imgI.getRGB(i, j) >> 8) & 0xFF);
                buff[j*this.imgI.getWidth()*3 + i*3 + 2] = (float) (this.imgI.getRGB(i, j) & 0xFF);
            }
        }

       // byte[] data = ((DataBufferByte) this.imgI.getRaster().getDataBuffer()).getData();

        Mat mat = new Mat(this.imgI.getHeight(), this.imgI.getWidth(), CvType.CV_32FC3);

        mat.put(0,0, buff);

        return mat;
    }

    public void Mat_CVtoImage3C(Mat mat)
    {
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_INT_RGB);

        float[] data = new float[mat.rows()*mat.cols()*3];
        mat.get(0, 0, data);

        for(int i = 0;i <mat.cols();i++)
        {
            for(int j = 0;j<mat.rows();j++)
            {
                int iBW;

                int iR = (int)(data[j*mat.cols()*3 + i*3 + 0]);
                int iG = (int)(data[j*mat.cols()*3 + i*3 + 1]);
                int iB = (int)(data[j*mat.cols()*3 + i*3 + 2]);

                iBW = 0x00FFFFFF & (iR << 16 | iG << 8 | iB);

                image.setRGB(i, j, iBW);
            }
        }

        this.imgI = image;

        this.imgI = image;
    }

    public void Mat_CVtoImage1C(Mat mat)
    {
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), BufferedImage.TYPE_INT_RGB);

        int iCH = mat.channels();
        int iElemSize = (int) mat.elemSize1();
        int iElemSize1 = (int) mat.elemSize1();

        float[] data = new float[mat.rows()*mat.cols()*(int)mat.elemSize()];
        mat.get(0, 0, data);

        for(int i = 0;i <mat.cols();i++)
        {
            for(int j = 0;j<mat.rows();j++)
            {
                int iBW = (int) data[j*mat.cols() + i];

                iBW = 0x00FFFFFF & (iBW << 16 | iBW << 8 | iBW);

                image.setRGB(i, j, iBW);
            }
        }

        this.imgI = image;
    }

    public Image Pyramidal_Resampling(int xScale, int yScale, int Width, int Height)
            // xScale should be an integral multiple of 11 :
            // yScale should be an integral multiple of 9;
    {
        //System.out.println("Resampling and rescaling down by factor " + xScale);
        //Image imNew = new Image();
        Image imOut = new Image();
        int i,j,R1,R2,R3,R4,G1,G2,G3,G4,B1,B2,B3,B4;
        int pix_val,pixR,pixG,pixB;

        int iWidthNew = Width/xScale;
        int iHeightNew = Height/yScale;

        imOut.imgI = new BufferedImage(iWidthNew, iHeightNew,BufferedImage.TYPE_INT_RGB);
        imOut.strName = "Resamp.rgb";
        //this.imgI subsample this to that image
        //imNew.imgI

        if (xScale <= 0)
        {
            System.out.println("Cannot Have 0 or <0 as xScale "+
                               " Default : xScale = 1");
            xScale = 1;
        }

        else if (yScale <= 0)
        {
            System.out.println("Cannot Have 0 or <0 as xScale "+
                               " Default : xScale = 1");
            yScale = 1;
        }
        else if (xScale > 32)
        {
            System.out.println("Cannot Have >32 as xScale "+
                               " Default : xScale = 1");
            xScale = 1;
        }

        else if (yScale > 32)
        {
            System.out.println("Cannot Have 0 as xScale "+
                               " Default : xScale = 1");
            yScale = 1;
        }

        //int varH = yScale*9;
        //int varW = xScale*11;

        for(i=0; i<iHeightNew ;i++)
        {
            for(j=0; j<iWidthNew; j++)
            {

                double a = (j*(float)Width/(float)iWidthNew) - ((int)(j*Width/iWidthNew));//along width
                double b = (i*(float)Height/(float)iHeightNew) - ((int)(i*Height/iHeightNew));//along height

                int p1 = (int)((j*Width/iWidthNew));
                int q1 = (int)((i*Height/iHeightNew));

                //System.out.println("P1 :"+p1+"Q1 :"+q1);
                //if (((p1<1)||(p1>Width-1))||((q1<1)||(q1>Height-1)))
                if(p1 >= Width -1 || q1 >= Height -1)
                {
                     //this.imgI.setRGB((int)(j),(int)(i),0);
                    //System.out.println("Out of Boundaries");
                }
                else
                {
                   // System.out.println("P1: " + p1 + " q1: " + q1);
                   // System.out.println("i: " + i + " j: " + j);

                    R1 = ((this.imgI.getRGB(p1+1,q1+1))>>16) & 0x000000FF;
                    R2 = ((this.imgI.getRGB(p1+1,q1))>>16) & 0x000000FF;
                    R3 = ((this.imgI.getRGB(p1,q1+1))>>16) & 0x000000FF;
                    R4 = ((this.imgI.getRGB(p1,q1))>>16) & 0x000000FF;

                    G1 = ((this.imgI.getRGB(p1+1,q1+1))>>8) & 0x000000FF;
                    G2 = ((this.imgI.getRGB(p1+1,q1))>>8) & 0x000000FF;
                    G3 = ((this.imgI.getRGB(p1,q1+1))>>8) & 0x000000FF;
                    G4 = ((this.imgI.getRGB(p1,q1))>>8) & 0x000000FF;

                    B1 = (this.imgI.getRGB(p1+1,q1+1)) & 0x000000FF;
                    B2 = (this.imgI.getRGB(p1+1,q1)) & 0x000000FF;
                    B3 = (this.imgI.getRGB(p1,q1+1)) & 0x000000FF;
                    B4 = (this.imgI.getRGB(p1,q1))& 0x000000FF;

                    pixR = (int)(((1-a)*(1-b)*(R1))+((1-a)*(b)*(R2))+((a)*(1-b)*(R3))+((a)*(b)*(R4)));
                    pixG = (int)(((1-a)*(1-b)*(G1))+((1-a)*(b)*(G2))+((a)*(1-b)*(G3))+((a)*(b)*(G4)));
                    pixB = (int)(((1-a)*(1-b)*(B1))+((1-a)*(b)*(B2))+((a)*(1-b)*(B3))+((a)*(b)*(B4)));

                    pix_val = (((int)(pixR) & 0xff) << 16) | (((int)(pixG) & 0xff) << 8) | ((int)(pixB) & 0xff);
                    imOut.imgI.setRGB(j,i,pix_val);
                }
            }
        }
        return imOut;
    }

}