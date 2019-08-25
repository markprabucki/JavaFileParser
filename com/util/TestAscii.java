package com.kohls.es.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

public class TestAscii 
{
	private static java.io.FileWriter outfile;
	private static java.io.PrintWriter pw;	
	private int errorCounter = 0;
   public static void main(String[] args)  {
  	 int lineCounter = 0;
	 int errorCounter = 0;	   
      try {

         System.out.println("Testing file:" + args[0]);
         String inputFileName = args[0];
         outfile = new java.io.FileWriter(inputFileName + "-FIXED");	   
         pw = new java.io.PrintWriter(outfile);
         FileInputStream fstream = new FileInputStream(args[0]);
         DataInputStream in = new DataInputStream(fstream);
         BufferedReader br = new BufferedReader(new InputStreamReader(in));
         String strLine;
         while ((strLine = br.readLine()) != null)   {
        	lineCounter++;
            strLine = testAscii(strLine,lineCounter);
            pw.println(strLine);
         }
         in.close();
         System.out.println("Done - number of lines processed:" + lineCounter);
       }
        catch (Exception e)
        {

           System.err.println("Error: " + e.getMessage());
        }
  }

  private static String testAscii(String test, int line) 
  {
	  String result = test;
	  byte bytearray []  = test.getBytes();
      CharsetDecoder d = Charset.forName("US-ASCII").newDecoder();
      try 
      {
	     CharBuffer r = d.decode(ByteBuffer.wrap(bytearray));
	     r.toString();
	  }
	  catch(Exception e) 
	  {		 
		 result = filterNonAscii(test);
	     System.out.println("Error on line:" + line + ",line is:" + test);
	  }
	  return result;
  }
  
	public static String filterNonAscii(String inString) {
		// Create the encoder and decoder for the character encoding
		Charset charset = Charset.forName("US-ASCII");
		CharsetDecoder decoder = charset.newDecoder();
		CharsetEncoder encoder = charset.newEncoder();
		// This line is the key to removing "unmappable" characters.
		encoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
		String result = inString;

		try {
			// Convert a string to bytes in a ByteBuffer
			ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(inString));

			// Convert bytes in a ByteBuffer to a character ByteBuffer and then to a string.
			CharBuffer cbuf = decoder.decode(bbuf);
			result = cbuf.toString();
		} catch (Exception cce) {
			cce.printStackTrace();
		}

		return result;	
	}
  
  
}

