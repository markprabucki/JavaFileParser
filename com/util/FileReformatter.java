package com.util;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
public class FileReformatter {
   private static java.io.FileWriter outfile;
   private static java.io.PrintWriter pw;
   public static void main(String[] args)  {
      try {
         fileReformatter();
      }catch (Exception e){
         System.err.println("Error: " + e.getMessage());
      }
  }

  private static void fileReformatter() throws Exception {
	  	int writeCount = 0;
        int readCount = 0;
        int progressCounter = 0;
		String inFile = "input.dat";
		String outFile = "output.dat";
	    try {
	        System.out.println("Input:" + inFile);
	        System.out.println("outfile:" + outFile);
	        String outputFileName = outFile;
	        outfile = new java.io.FileWriter(outputFileName);
	        pw = new java.io.PrintWriter(outfile);
	        System.out.println("Reading file:" + inFile);
	        FileInputStream infstream = new FileInputStream(inFile);
	        DataInputStream inDStream = new DataInputStream(infstream);
	        BufferedReader brInput = new BufferedReader(new InputStreamReader(inDStream));
	        String lineIn = "";
	        String lineOut = "";
	        String prdCode = "";
	        String skuCode = "";
	        String dept = "";
	        String majCl = "";
            String skuStatusCode = "";
	        while ((lineIn = brInput.readLine()) != null) {
	           readCount++;


	       // System.out.println("line In:" + lineIn);
	       // 00319831,01222085,25,0012,0020|
	       // 01000022,92488446,30|
	       // 0123456789|123456789|123456789|



	           prdCode = lineIn.substring(0, 8);
	           skuCode = lineIn.substring(9,17);
	           skuStatusCode = lineIn.substring(18,20);
	           dept = lineIn.substring(21,25);
	           majCl = lineIn.substring(26,30);

	       //    System.out.println("(pass1) prdCode:" + prdCode + " skuCode:" + skuCode + " skuStatusCode:" + skuStatusCode);

	           prdCode.trim();
	           int lastLeadZeroIndex = 0;

	           for (int i = 0; i < prdCode.length(); i++) {
	             char c = prdCode.charAt(i);
	             if (c == '0') {
	               lastLeadZeroIndex = i;
	             } else {
	               break;
	             }
	           }
	           if(prdCode.indexOf('0') != -1) {
	              prdCode = prdCode.substring(lastLeadZeroIndex+1, prdCode.length());
	           }

	           skuCode.trim();
	           lastLeadZeroIndex = 0;
	           for (int i = 0; i < skuCode.length(); i++) {
	             char c = skuCode.charAt(i);
	             if (c == '0') {
	               lastLeadZeroIndex = i;
	             } else {
	               break;
	             }
	           }
	           if(skuCode.indexOf('0') != -1 ) {
	        //	  System.out.println("index:" + skuCode.indexOf("0"));
	              skuCode = skuCode.substring(lastLeadZeroIndex+1, skuCode.length());
	           }
	           skuStatusCode.trim();






	        //   System.out.println("(pass2) prdCode:" + prdCode + " skuCode:" + skuCode + " skuStatusCode:" + skuStatusCode);
	           lineOut = prdCode+","+skuCode+","+skuStatusCode+","+dept+","+majCl;
	       //    System.out.println("line Out:" + lineOut);
	           pw.write(lineOut + "\n");
	           writeCount++;
	           progressCounter++;
	    	   if(progressCounter==5000) {
	    	      progressCounter = 0;
	    	      System.out.println("----------------------------------------");
	    	   	  System.out.println("Done - number of lines processed:" + progressCounter);
	    		  System.out.println("write:" + writeCount);
	    	   }
	        }

	        System.out.println("sleeping...");
	        Thread.sleep(10000);
	        invsm.close();
	        pw.close();
	        System.out.println("Lines read:" + readCount);
	        System.out.println("Lines written:" + writeCount);
	    }catch (Exception e){
           System.err.println("Error: " + e.getMessage());
        }
  }
}