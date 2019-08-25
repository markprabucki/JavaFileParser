package com.util;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.HashMap;
public class JavaFileParser
{
   private static java.io.FileWriter outfile;
   private static java.io.FileWriter loadfile;
   private static java.io.PrintWriter pw;
   private static java.io.PrintWriter pw2;
   public static void main(String[] args)  {

      try
      {
    	  JavaFileParser();
      }
      catch (Exception e)
      {
         System.err.println("Error: " + e.getMessage());
      }
  }

  private static void JavaFileParser() throws Exception
  {

	    String driverName = "oracle.jdbc.driver.OracleDriver";
	    int Counter1 = 0;
	    int Counter2 = 0;
	    int progressCounter = 0;
	  	int writeCount = 0;
        int foundCount = 0;

		String inFile1 = "Input01.txt";
		String inFile2 = "input02.txt";
		String outFile = "report.txt";
		String loaderFile = "loader.txt";
	    try {
	    	Class.forName(driverName);
	        System.out.println("inFile1:" + inFile1);
	        System.out.println("inFile2:" + inFile2);
	        System.out.println("outfile:" + outFile);
	        System.out.println("loader:" + loaderFile);


	        String inputFileName1 = inFile1;
	        String inputFileName2 = inFile2;
	        String outputFileName = outFile;
	        String loaderFileName = loaderFile;

	        outfile = new java.io.FileWriter(outputFileName);
	        pw = new java.io.PrintWriter(outfile);

	        loadfile = new java.io.FileWriter(loaderFileName);
	        pw2 = new java.io.PrintWriter(loadfile);

	        System.out.println("Reading file:" + inFile1);
	        FileInputStream infstream1 = new FileInputStream(inFile1);
	        System.out.println("Reading file:" + inFile2);
	        FileInputStream infstream2 = new FileInputStream(inFile2);

	        DataInputStream inStream1 = new DataInputStream(infstream1);
	        BufferedReader brin1 = new BufferedReader(new InputStreamReader(inStream1));

	        DataInputStream inStream2 = new DataInputStream(infstream2);
	        BufferedReader brin2 = new BufferedReader(new InputStreamReader(inStream2));

	        String strLine = "";
	        String sku = "";
	        String product = "";
            String pkgCode = "";
            String statusCode = "";
            String activeInd = "";
            String bmData = "";
            String svcCode = "";


	        HashMap hashmap = new HashMap();

	        while ((strLine = brvsm.readLine()) != null)
	        {
	           product = strLine.substring(0, 8);
	           sku = strLine.substring(8,16);
	           pkgCode = strLine.substring(16,18);

	           statusCode = strLine.substring(36,38);
	           svcCode = strLine.substring(38,41).trim();
		       System.out.println("svcCode:" + svcCode);


	      	   hashmap.put(sku, svcCode);

	           vsmCounter++;
	           System.out.println("sku-->" + sku + "<---   svcCode-->" + svcCode + "<--");

	        }

	        while ((strLine = brbm.readLine()) != null) {
	           bmCounter++;
	 	       sku = strLine.substring(31,41).replace('"', ' ').trim();
	 	       svcCode = strLine.substring(61,64).replace('"',' ').trim();
	           bmData = pkgCode + statusCode;
	           if(hashmap.containsKey(sku))
	           {
	        	   foundCount++;
	        	   String hashMapValue = (String)hashmap.get(sku);
	        	   hashMapValue = hashMapValue.trim();
	        	   if(!hashMapValue.equalsIgnoreCase(svcCode)){

	        		   String rptdata = "SKU:," + sku + " Value:," + hashMapValue + " ,Value:," + bmData + " svcCode:," + svcCode;
	        	       String outdata = "" + sku + "|CODE|" + hashMapValue;

	        	       System.out.println(outdata);
	        		   pw.write(rptdata + "\n");
	        		   pw2.write(outdata + "\n");
	        	       writeCount++;
	        	   }


	           }
	           else
	           {
	           }
	           if(progressCounter>=10)
	           {
	        	  progressCounter = 0;
	        	  System.out.println("----------------------------------------");
	   	          System.out.println("Done - number of lines processed:" + progressCounter);
		          System.out.println("write:" + writeCount);
	           }
	        }



	        System.out.println("sleeping...");
	        Thread.sleep(10000);
	        inbm.close();
	        invsm.close();
	        pw.close();
	        pw2.close();

	    } catch (ClassNotFoundException e) {
	        System.out.println("Could not find database Driver");
	    }
        catch (Exception e)
        {
           System.err.println("Error: " + e.getMessage());
        }
	    finally {
	    }
  }
}

