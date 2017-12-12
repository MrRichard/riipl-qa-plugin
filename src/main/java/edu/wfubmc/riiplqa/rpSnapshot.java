//Comment out the package line when testing this code
//in the /riipl30/bwagner/incoming/rpSnapshot directory
package edu.wfubmc.riiplqa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.lang.Double;
import java.lang.Math;

//Image Stuff
import ij.ImagePlus;
import ij.io.FileSaver;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.awt.Graphics;

//Charts
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.ChartFactory;
// import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.jfree.chart.plot.XYPlot;
import java.awt.Color;


//Alternate saving
import javax.imageio.ImageIO;


/** A class/executable for creating motion correction image from an SPM rp*.txt file
* @author Ben Wagner
* @author RIIPL Lab
* @version 0.1 January 2012
*/
public class rpSnapshot
{
	/** Executable is "java -jar rpSnapshot outfile.jpg rp.txt"
	*/
	public static void main (String[] args) 
	{
		String outFile=null;
		File rpFile=null;
		for (String s: args) 
		{
				if (outFile==null)
				{
					outFile=s;
				}
				else
				{
					rpFile=new File(s);
				}
		}
		
//		ImagePlus outImg = createRPGraphic(outFile, rpFile, null);

		Double maxV[] = new Double[6];
		ImagePlus outImg = createRPGraphic(outFile, rpFile, maxV);
		System.out.println("Max X movement (mm) : " + maxV[0]);
		System.out.println("Max Y movement (mm) : " + maxV[1]);
		System.out.println("Max Z movement (mm) : " + maxV[2]);
		System.out.println("Max Pitch mvmt (deg): " + maxV[3]);
		System.out.println("Max Roll mvmt  (deg): " + maxV[4]);
		System.out.println("Max Yaw mvmt   (deg): " + maxV[5]);

//		outImg.show();
	}
	
	/** Creates a jpg of slices from multiple nifti images
	* @param		outFile	Name of the jpg output file
	* @param		rpFile	An rp*.txt
	* @return						An ij.ImagePlus (ImageJ) that may be shown or otherwise manipulated
	*/
	public static ImagePlus createRPGraphic(String outFile, File rpFile, Double maxV[]) 
	{
		int nVols=0;
		
		XYSeries mvmtX=new XYSeries("Movement X");  //Also know as right (in mm)
		XYSeries mvmtY=new XYSeries("Movement Y");; //Also know as forward (in mm)
		XYSeries mvmtZ=new XYSeries("Movement Z");; //Also know as up (in mm)
		XYSeries mvmtPitch=new XYSeries("Pitch");;  //Pitch (in radians)
		XYSeries mvmtRoll =new XYSeries("Roll");;   //Roll (in radians)
		XYSeries mvmtYaw  =new XYSeries("Yaw");;    //Yaw (in radians)

		System.out.println("Reading: " + rpFile);

		/* Read the 6 x nVol file */
		try
		{
			BufferedReader bufRdr = new BufferedReader(new FileReader(rpFile));
			String line=null;
	
			while ((line=bufRdr.readLine())!=null)
			{
				nVols=nVols+1;

				StringTokenizer st = new StringTokenizer(line," ");
				mvmtX.add(nVols,Double.parseDouble(st.nextToken()));
				mvmtY.add(nVols,Double.parseDouble(st.nextToken()));
				mvmtZ.add(nVols,Double.parseDouble(st.nextToken()));
				mvmtPitch.add(nVols,Math.toDegrees(Double.parseDouble(st.nextToken())));
				mvmtRoll.add(nVols,Math.toDegrees(Double.parseDouble(st.nextToken())));
				mvmtYaw.add(nVols,Math.toDegrees(Double.parseDouble(st.nextToken())));
			}
		}
		catch (Exception e)
		{
			System.out.print("Unable to read data from file: "+rpFile);
			System.out.println("Error: "+e);
			System.exit(1);
		}

		/* Create Image */
		int width=500, height=300;
		BufferedImage combined = new BufferedImage(width,2*height+10,BufferedImage.TYPE_INT_ARGB);

		try
		{
			XYSeriesCollection mvmtXYZ = new XYSeriesCollection();
			mvmtXYZ.addSeries(mvmtX);
			mvmtXYZ.addSeries(mvmtY);
			mvmtXYZ.addSeries(mvmtZ);
			
			XYSeriesCollection mvmtPRY = new XYSeriesCollection();
			mvmtPRY.addSeries(mvmtPitch);
			mvmtPRY.addSeries(mvmtRoll);
			mvmtPRY.addSeries(mvmtYaw);
			

			JFreeChart top = ChartFactory.createXYLineChart(
				"Translation", //Title
				"Volume Number", //X axis label
				"mm", //Y axis label
				mvmtXYZ, //dataset
				PlotOrientation.VERTICAL, //plot orientation
				true, //show legend
				true, //use tooltips
				false //configure chart to genderate urls
				);

			XYPlot plot = (XYPlot) top.getPlot();
			plot.getRenderer().setSeriesPaint(0, Color.BLUE);
			plot.getRenderer().setSeriesPaint(1, Color.GREEN);
			plot.getRenderer().setSeriesPaint(2, Color.RED);

			JFreeChart bottom = ChartFactory.createXYLineChart(
				"Rotation", //Title
				"Volume Number", //X axis label
				"degrees", //Y axis label
				mvmtPRY, //dataset
				PlotOrientation.VERTICAL, //plot orientation
				true, //show legend
				true, //use tooltips
				false //configure chart to genderate urls
				);

			plot = (XYPlot) bottom.getPlot();
			plot.getRenderer().setSeriesPaint(0, Color.BLUE);
			plot.getRenderer().setSeriesPaint(1, Color.GREEN);
			plot.getRenderer().setSeriesPaint(2, Color.RED);

//			ChartUtilities.saveChartAsJPEG(new File(outFile),chart,500,300);

			Graphics g = combined.getGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0,0,width,2*height+10);
			g.drawImage(top.createBufferedImage(width,height),0,0,null);
			g.drawImage(bottom.createBufferedImage(width,height),0,height+10,null);
			g.dispose();

		}
		catch (Exception e)
		{
			System.out.print("Unable to create/save chart");
			System.out.println("Error: "+e);
			System.exit(1);
		}

		ImagePlus imp=new ImagePlus("Montage Image",combined);
		FileSaver fs=new FileSaver(imp);
		fs.setJpegQuality(100);
		System.out.println("Saving file: "+outFile);
		fs.saveAsJpeg(outFile);

		if (maxV==null)
		{
			System.out.println("Will NOT compute max values");
		}
		else
		{
			System.out.println("Computing max values...");
			maxV[0]=Math.max(Math.abs(mvmtX.getMinY()),Math.abs(mvmtX.getMaxY()));
			maxV[1]=Math.max(Math.abs(mvmtY.getMinY()),Math.abs(mvmtY.getMaxY()));
			maxV[2]=Math.max(Math.abs(mvmtZ.getMinY()),Math.abs(mvmtZ.getMaxY()));
			maxV[3]=Math.max(Math.abs(mvmtPitch.getMinY()),Math.abs(mvmtPitch.getMaxY()));
			maxV[4]=Math.max(Math.abs(mvmtRoll.getMinY()),Math.abs(mvmtRoll.getMaxY()));
			maxV[5]=Math.max(Math.abs(mvmtYaw.getMinY()),Math.abs(mvmtYaw.getMaxY()));
		}

		System.out.println("Done!");
		return imp;
	}
}
