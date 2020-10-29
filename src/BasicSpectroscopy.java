import java.awt.Color;

public class BasicSpectroscopy  // THIS IS OUTDATED AND WONT WORK WELL ANYMORE
{
	private static int THRESHOLD_INTENSITY_MIN = 20;
	private static int THRESHOLD_INTENSITY_MAX = 245;
	
	private static int STD_WIDTH = 1000;
	
	private static Picture Spectrum = new Picture("imgs/Data/Spectrum.png");

    public static double intensity(Color color, boolean weighted) 
    {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        if (r == g && r == b) return r; 
        
        return 0.299*r + 0.587*g + 0.114*b; 
    }

    public static Color toGray(Color color) 
    {
        int y = (int) (Math.round(intensity(color, true)));
        Color gray = new Color(y, y, y);
        return gray;
    }
    
    public static Picture resize(int newX, int newY, Picture pic)
    {
    	double sW = newX/(double)pic.width();
        double sH = newY/(double)pic.height();
        System.out.println(sW);
        System.out.println(sH);
        Picture newPic = new Picture(newX, newY);
        
        for (int col = 0; col < newX; col++) 
        {
            for (int row = 0; row < newY; row++) 
            {
               newPic.set(col, row, pic.get((int)(col/sW), (int)(row/sH)));
            }
        }

        return newPic;
    }

    
    public static int getWavelength(Color color)
    {
    	float[] hsb = new float[3];
    	Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsb);
    	
    	int wavelength = 999;
    	
    	float hue = hsb[0];
    	int h = (int) (hue * 360);
    	if (0 <= h && h <= 288)
    		wavelength = (int) ((288-h) * ((640-380)/288.0) + 380);
    	else if (324 <= h)
    		wavelength = (int) ((360-h) * ((700-640)/36.0) + 640);
    	else
    		wavelength = (int) ((288) * ((640-380)/288.0) + 380);
    	
    	return wavelength;
    }
    
    private static int[] getWVals(Picture spec)
    {
    	int width  = spec.width();
        int height = spec.height();
        
        int[] wvals = new int[STD_WIDTH];
        int[] ovals = new int[STD_WIDTH];
        
        for (int i = 0; i < STD_WIDTH; i++)
        {
        	wvals[i] = 0;
        	ovals[i] = 0;
        }
        
        int x = 0;
        double xi = 0;
        for (int i = 0; i < width; i++)
        {
        	Color color = spec.get(i, height/2);
        	if (intensity(color, false) > xi)
        	{
        		xi = intensity(color, false);
        		x = i;
        	}
        }
        
        for (int col = 0; col < width; col++)
        {
        	Color color = spec.get(col, height/2);
        	
        	if (intensity(color, false) > THRESHOLD_INTENSITY_MIN && intensity(color, false) < THRESHOLD_INTENSITY_MAX)
        	{
	        	int wavelength = getWavelength(color);
	        	
	        	//int d = Math.abs(x - col)/x;
	        	double r = 1;
	        	double intensity = intensity(color, false) * r;
	        	
	        	//AM - wvals[wavelength] = (int) ((wvals[wavelength] * ovals[wavelength] + intensity)/(ovals[wavelength] + 1));
	        	wvals[wavelength] = (int) (Math.sqrt((Math.pow(wvals[wavelength],2) * ovals[wavelength] + Math.pow(intensity, 2))/(ovals[wavelength]+1)));
	        	//GM - wvals[wavelength] = (int) (Math.pow((Math.pow(wvals[wavelength], ovals[wavelength]) * intensity), 1.0/(ovals[wavelength] + 1)));
	        	
	        	ovals[wavelength]++;
        	}
        }
        
        return wvals;
    }
    
    public static void getAnalysis(Picture spec)
    {
    	int[] data = new int[STD_WIDTH];
    }
    
    public static Picture plot(Picture spec, Picture graph, Color col, boolean fill)
    {
        int[] wvals = getWVals(spec);
        
        for (int i = 0; i < STD_WIDTH; i++)
        {
        	if (fill)
        	{
        		if (col == null)
        		{
        			Color color = Color.getHSBColor((float)(288 - (i - 380)/((640-380)/288.0))/360.0f, 1.0f, 1.0f);
        			for (int j = 255; j >= 255-(int) wvals[i]; j--)
        				graph.set(i, j, color);
        		}
        		else
        		{
        			for (int j = 255; j >= 255-(int) wvals[i]; j--)
        				graph.set(i, j, col);
        		}
        	}
        	else
        	{
	        	if (col == null)
	        		graph.set(i, 255-(int) wvals[i], Color.getHSBColor((float)(288 - (i - 380)/((640-380)/288.0))/360.0f, 1.0f, 1.0f));
	        	else
	        		graph.set(i, 255-(int) wvals[i], col);
        	}
        }
        
        return graph;
    }
    
    public static Picture lineSpec(Picture spec, Picture graph)
    {
    	int[] wvals = getWVals(spec);
    	
        for (int i = 0; i < STD_WIDTH; i++)
        {
        	if (wvals[i] != 0)
        	{
        		for (int j = 0; j < 256; j++)
        			graph.set(i, j, Color.getHSBColor((float)(288 - (i - 380)/((640-380)/288.0))/360.0f, 1.0f, wvals[i]/255.0f));
        	}
        }
        
        return graph;
    }
    
    public static Picture addLines(Picture graph, int intervalX, int intervalY)
    {
    	for (int i = 0; i < graph.width(); i+=intervalX)
    	{
    		for (int j = 0; j < graph.height(); j+=intervalY)
    			graph.set(i, j, Color.gray);
    	}
    	return graph;
    }
    
    public static Picture combineData(Picture[] pictures)
    {
    	int width = STD_WIDTH;
    	int height = 0;
    	
    	for (Picture picture : pictures)
    	{
    		height += picture.height();
    	}
    	Picture result = new Picture(width, height+pictures.length-1);
    	
    	int currentrow = 0;
    	for (int i = 0; i < pictures.length; i++)
    	{
    		for (int row = 0; row < pictures[i].height(); row++)
    		{
    			for (int col = 0; col < STD_WIDTH; col++)
    			{
    				result.set(col, currentrow, pictures[i].get(col, row));
    			}
    			currentrow++;
    		}
    		
    		if (i != pictures.length - 1)
    		{
	    		for (int col = 0; col < STD_WIDTH; col++)
				{
					result.set(col, currentrow, Color.white);
				}
	    		currentrow++;
    		}
    	}
    	
    	return result;
    }
    
    public static void main(String[] args) 
    {
        Picture picture = new Picture(args[0]);
        Picture picture2 = new Picture(args[1]);
        Picture picture4 = new Picture(args[2]);
        
        Picture newPic = new Picture(STD_WIDTH, 256);
        Picture newPic2 = new Picture(STD_WIDTH, 256);
        
        for (int row = 0; row < 256; row++)
        	for (int col = 0; col < STD_WIDTH; col++)
        		newPic.set(col, row, Color.black);
        for (int row = 0; row < 256; row++)
        	for (int col = 0; col < STD_WIDTH; col++)
        		newPic2.set(col, row, Color.black);
        
        plot(picture, newPic, null, true);
        plot(picture4, newPic, null, true);
        
        addLines(newPic, 20, 5);
        addLines(newPic, 100, 2);
        
        lineSpec(picture, newPic2);
        lineSpec(picture4, newPic2);
        
        for (int i = 380; i < 800; i++)
        {
        	if (newPic2.get(i, 50).equals(Color.BLACK))
        		System.out.println(i);
        }
        
        //Picture picture3 = new Picture("imgs/Data/spectra.jpg");
        //int[] vals = getWVals(picture3);
        //for (int i = 0; i < vals.length; i++)
       // 	if (vals[i] > THRESHOLD_INTENSITY_MIN)
       // 		System.out.println(i);
       // Picture newPic3 = new Picture(STD_WIDTH, 256);
       // lineSpec(picture3, newPic3);
        
        Picture[] data = new Picture[2];
        data[0] = newPic;
        data[1] = newPic2;
      //  data[2] = newPic3;
        Picture merged = combineData(data);
        
        merged.show();
    }
}


/*Copyright 2000-2017, Robert Sedgewick and Kevin Wayne and now Aathreya Kadambi.*/