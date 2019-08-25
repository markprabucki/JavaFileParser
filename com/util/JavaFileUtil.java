package com.mark.es.promotion;
import java.io.BufferedReader; // file reader - more efficient (not as efficient as java 1.6 though)
import java.io.File;           // file work output and input
import java.io.FileNotFoundException;  // exception handling
import java.io.FileReader;     // file work output and input
import java.io.IOException;    // exception handling
import java.io.PrintWriter;  // output files
import java.util.ArrayList;  // list of old and new files
import java.util.HashMap;    // Promo record comparisons
import java.util.Iterator;   // used to iterate the keys for each hashMap
import java.util.StringTokenizer; // This is used to generate a unique key


import com.mark.es.util.markLogUtil;
/**
  *PromoDeltaUtil
  *
  * notes:
  *   "Promo ID" is the term used by the corporate promo system to identify 
  *       a related group of promotional items   
  *        (e.g. promo id 974 = Father's Day "main" promotions
  *			975 = Father's Day "unadvertised" promotions
  *			976 = Father's Day "bonus buy" promotions      )
  *   "Promo ID" does not correspond to the internal BM "promotionID";
  *	it is implemented in BM as a Promotion Folder (e.g. P974)
  *
  * 
  *   "Promo Interface ID" is the term used by the corporate promo system to identify a specific offer
  *	(e.g.  promo interface id 38743812  = sale $129.99 for sku 87320988 on 6/8/06 thru 6/17/06)
  *   "Promo Interface ID" is not the same as the internal BM "promotionID", but there is a one-to-one relationship
  *  	it is implemented in BM as the promo "name"  (e.g. P38743812)   
  *
  *
  *  This java class was ported from Perl (ES_promo_delta.pl) on 3-15-2011
  *  the port was due to supportability of the process and to better manage data capacity and
  *  future changes
  *  
  *  The purpose of this java class is to compare two directories which contain files of promotion records in 
  *  bmi format -  the files will be parsed by ES_promo_parse.pl -  the promotion extract created by ESK029 
  *  PromoExtract class will be broken up into files - one file per promotion -  a promotion being defined as
  *  the PromoID -- ie:  11_0228T:P2974 is a promotion,   Clearance:P2222 is a promotion
  *  
  *  Promotion records must be handled differently due to blue martini import file rules.
  *  If any promotion object records change then the promotion must be deleted and re-added 
  *  
  *  The blue martini bmi rules do not apply to assortments, assortment contents.    
  *  If a promotion is be removed because it is old - the entire promo folder and assortment can be removed and
  *  all children will be removed by that action.
  *
**/
public class PromoDeltaUtil {
	
	private ArrayList oldFiles = new ArrayList();  // used to contain a list of the parsed promtions in the previous extract
	private ArrayList newFiles = new ArrayList();  // used to contain a list of the parsed promotions in the current extract
	private ArrayList compareFiles = new ArrayList(); // used to contain a list of parsed promotions that must be compared
	private PrintWriter pwAddFile = null;          // output file for bmi load
	private PrintWriter pwDelFile = null;          // output file for bmi delete
	private String sOldDir = null;                 // directory location for previous promotion extract parsed files
	private String sNewDir = null;                 // directory location for current promotion extract parsed files
	private int loadRecords = 0;                   // just an informational count for displaying how many we have for esk010 and esk073
	private int delRecords = 0;	                   // just an informational count for displaying how many we have for esk010 and esk073
	
	private HashMap hash_newPromo = new HashMap(); //  HashMap for new Promotion record types - only will contain one promo file per run
	private HashMap hash_newPromoCond = new HashMap();//  HashMap for new Promotion Condition record types - only will contain one promo file per run
	private HashMap hash_newPromoDisc = new HashMap();//  HashMap for new Promotion Discount record types - only will contain one promo file per run
	private HashMap hash_newPromoDate = new HashMap();//  HashMap for new Promotion Date record types - only will contain one promo file per run
	private HashMap hash_newAssortment = new HashMap();//  HashMap for new Assortment record types - only will contain one promo file per run
	private HashMap hash_newAssortmentContents = new HashMap();//  HashMap for new Assortment Content record types - only will contain one promo file per run
	private HashMap hash_newObjectAttribute = new HashMap();//  HashMap for new Object Attribute record types - only will contain one promo file per run

	private HashMap hash_oldPromo = new HashMap();  //  HashMap for old Promotion record types - only will contain one promo file per run   
	private HashMap hash_oldPromoCond = new HashMap();//  HashMap for old Promotion Condition record types - only will contain one promo file per run
	private HashMap hash_oldPromoDisc = new HashMap();//  HashMap for old Promotion Discount record types - only will contain one promo file per run
	private HashMap hash_oldPromoDate = new HashMap();//  HashMap for old Promotion Date record types - only will contain one promo file per run
	private HashMap hash_oldAssortment = new HashMap();//  HashMap for old Assortment record types - only will contain one promo file per run
	private HashMap hash_oldAssortmentContents = new HashMap();//  HashMap for old Assortment Content record types - only will contain one promo file per run
	private HashMap hash_oldObjectAttribute = new HashMap();//  HashMap for old Object Attribute record types - only will contain one promo file per run    	
    
	private HashMap hash_basePromoChange = new HashMap(); // if anything changes in a promotion it must be 
                                                          // reloaded - because the base object has changed
	/**
	 * Constructor for PromoDeltaUtil
	 * no args
	 */
	public PromoDeltaUtil() {
	}

	/**
	 * createDelta
	 * 
	 * This is the only public method available on PromoDeltaUtil and will do 
	 * all the delta work 
	 * 
	 * 
	 * @param sOldDir - old directory for previous extract parsed files
	 * @param sNewDir - new directory for current extract parsed files
	 * @param pwAddFile - file to be used for the promo load for esk073
	 * @param pwDelFile - file to be used for the promo delete for esk010
	 * @throws Exception 
	 */
	public void createDelta(String sOldDir
			               ,String sNewDir
			               ,PrintWriter pwAddFile
			               ,PrintWriter pwDelFile) throws Exception {
	   long start = System.currentTimeMillis();	   // used for comparison at the end of the process for run time duration
		
	   this.sOldDir = sOldDir;  // old directory for previous extract parsed files
	   this.sNewDir = sNewDir;  // new directory for current extract parsed files
	   this.pwAddFile = pwAddFile;  // file to be used for the promo load for esk073
	   this.pwDelFile = pwDelFile;  // file to be used for the promo delete for esk010
	   markLogUtil.logmarkGeneral("-- Getting files from previous extract....",markLogUtil.LEVEL_CRITICAL);// informational
	   oldFiles = new ArrayList(listFileNamesInDir(sOldDir));   // builds the arrayList of promo files from previous extract
	   markLogUtil.logmarkGeneral("files:" + oldFiles.size(),markLogUtil.LEVEL_CRITICAL);	   // informational show how many files
	   markLogUtil.logmarkGeneral("-- Getting files from current extract....",markLogUtil.LEVEL_CRITICAL);	  // informational
	   newFiles = new ArrayList(listFileNamesInDir(sNewDir)); // builds the arrayList of promo files from current extract
	   markLogUtil.logmarkGeneral("-- files:" + newFiles.size(),markLogUtil.LEVEL_CRITICAL);	// informational show how many files   
	   markLogUtil.logmarkGeneral("-- Comparing files and removing old and new....",markLogUtil.LEVEL_CRITICAL);	// informational
	   compareFiles = buildListForCompare(oldFiles,newFiles);    // build list of files (promotions) that need to be compared
	                                                             // this will also remove those from the old/new arrayList
	   markLogUtil.logmarkGeneral("-- Adding old promotions to delete bmi output....",markLogUtil.LEVEL_CRITICAL);	   // informational
	   createDelOutput();    // write out promotions to the delete bmi file - promotions that are old - (not in the new list)
	   markLogUtil.logmarkGeneral("-- Adding new promotions to input bmi output....",markLogUtil.LEVEL_CRITICAL);	   // informational
	   createInsOutput();    // write out promotions that are only new and were not in the old list to the esk073 loader bmi
       markLogUtil.logmarkGeneral("* Unique Records for Delete: " + delRecords,markLogUtil.LEVEL_CRITICAL);  // informational for current del count
	   markLogUtil.logmarkGeneral("* Unique Records for Insert: " + loadRecords,markLogUtil.LEVEL_CRITICAL); // informational for current ins count
	   markLogUtil.logmarkGeneral("-- Comparing promotions that have changed --",markLogUtil.LEVEL_CRITICAL);// informational
	   comparePromos(); // this does the work for comparing promotions
	   
	   long milli = (System.currentTimeMillis() - start); // final timing for entire run time in seconds
	   long sec = (milli / 1000);
	   markLogUtil.logmarkGeneral("COMPARE PROCESS TOOK: " + sec + " seconds.",markLogUtil.LEVEL_CRITICAL);
       markLogUtil.logmarkGeneral("* Records for Delete: " + this.delRecords,markLogUtil.LEVEL_CRITICAL);
	   markLogUtil.logmarkGeneral("* Records for Insert: " + this.loadRecords,markLogUtil.LEVEL_CRITICAL);
	   
	}

	/**
	 * listFileNamesInDir
	 * 
	 * This will just return an ArrayList of fileNames in a directory 
	 * 
	 * @param dirPath - directory to build the fileName list from
	 * @throws Exception - if any exceptions occur then consider them fatal
	 *                     because it will be used for the comparison process
	 */
	private ArrayList listFileNamesInDir(String dirPath) throws Exception{
		File folder = new File(dirPath);
	    File[] listOfFiles = folder.listFiles();   // get list of fileNames only
	    ArrayList fileNames = new ArrayList();
	    for (int i = 0; i < listOfFiles.length; i++){
	        if (listOfFiles[i].isFile()){ // files only no directories
	          markLogUtil.logmarkGeneral("File " + listOfFiles[i].getName(),markLogUtil.LEVEL_CRITICAL);
	          fileNames.add(listOfFiles[i].getName()); // add to the ArrayList
	        } 
	        else if (listOfFiles[i].isDirectory()) { // ignore any directories
	          markLogUtil.logmarkGeneral("Directory (ignoring) " + listOfFiles[i].getName(),markLogUtil.LEVEL_CRITICAL);
	        }		
	    }
	    markLogUtil.logmarkGeneral("directory listing size:" + listOfFiles.length,markLogUtil.LEVEL_CRITICAL);
	    return fileNames; 
	}
	
	/**
	 * buildListForCompare
	 * 
	 * 
	 * This will return an arrayList that has promotions (or files) that 
	 * are in both the old and new Arraylists --- if they are in the 
	 * old and new list then they need to be compared..  they will be
	 * removed from the old and new ArrayList and added to the compare ArrayList
	 */
	private ArrayList buildListForCompare(ArrayList oldList, ArrayList newList)	{
		ArrayList copy = new ArrayList(oldList);  // use the old list to iterate off of
		Iterator iter = copy.listIterator();      // convenience iterator
		ArrayList out = new ArrayList();          // returned ArrayList
		Object file = null;
		while(iter.hasNext()){	
			file = iter.next();  // get next file
			if(newFiles.contains(file))	{
				markLogUtil.logmarkGeneral("New Extract contains:" + file,markLogUtil.LEVEL_CRITICAL); // display promos to compare in the log
				out.add(file.toString());                           // add promo to compare to the compare ArrayList
				oldFiles.remove(file);                              // remove from old ArrayList
				newFiles.remove(file);                              // remove from new ArrayList
			}
			else {
				markLogUtil.logmarkGeneral("New Extract does not contain:" + file.toString(),markLogUtil.LEVEL_CRITICAL); // informational to show what promos are being removed			
			}
		}
		return out;
	}
	
	/**
	 * 
	 * createDelOutput
	 * 
	 * This will iterate through the old ArrayList and write out all data to
	 * the delete bmi since they no longer exist in the new load files and
	 * can be removed
	 * 
	 * @throws IOException - if any exceptions occur then consider them fatal
	 *                       because otherwise our results will be wrong
	 * 
	 * we're building it to look like this
	 * -----------------------------------------
	 * 
	 * Example --- 
     * # 11_0228T:P2868 is old; remove
     * D|PROMOTION_FOLDER|P2868|/Promotions/11_0228T
     * D|ASSORTMENT_FOLDER|P2868|/Assortments/Promotions/11_0228T
     * # 11_0228T:P2869 is old; remove
     * D|PROMOTION_FOLDER|P2869|/Promotions/11_0228T
     * D|ASSORTMENT_FOLDER|P2869|/Assortments/Promotions/11_0228T
     * # 11_0228T:P2865 is old; remove
     * D|PROMOTION_FOLDER|P2865|/Promotions/11_0228T
     * D|ASSORTMENT_FOLDER|P2865|/Assortments/Promotions/11_0228T
     * # 11_0228T:P2987 is old; remove
     * D|PROMOTION_FOLDER|P2987|/Promotions/11_0228T
     * D|ASSORTMENT_FOLDER|P2987|/Assortments/Promotions/11_0228T
     * # 11_0228T:P2866 is old; remove
     * D|PROMOTION_FOLDER|P2866|/Promotions/11_0228T
     * D|ASSORTMENT_FOLDER|P2866|/Assortments/Promotions/11_0228T
	 * 
	 */
	private void createDelOutput() throws IOException {
		markLogUtil.logmarkGeneral("Writing old data to Delete BMI File",markLogUtil.LEVEL_CRITICAL);  // informational
		markLogUtil.logmarkGeneral("Size of old data promotions is: " + oldFiles.size(),markLogUtil.LEVEL_CRITICAL); // informational
		
        String assortmentPath = "/Assortments/Promotions/"; // blue martini promo assortment location 
		String promotionPath = "/Promotions/";              // blue martini promo assorment path
	
		String promotionParent = null; // this is for example 11_0228T
		String promotionFolder = null; // this is the promotion number example: 2423
		Iterator iter = oldFiles.listIterator(); // convenience iterator
		Object file = null;

		while (iter.hasNext()) {
			file = iter.next(); // next file
            markLogUtil.logmarkGeneral("Creating Del BMI Entry for:" + file.toString(),markLogUtil.LEVEL_CRITICAL);
            StringTokenizer st = new StringTokenizer(file.toString(), ":");  // files are named 11_0228T:P2423 by
                                                                             // ES_promo_parse.pl            
            /* 
             * 
             * The files must be named -- parent promo : promo folder
             * these are created by ES_promo_parse.pl in the delta job
             *   
             *     11_0228T:P2865
             *     Clearance:P2423
             */
           	promotionParent = st.nextToken();
            promotionFolder = st.nextToken();   
           
            StringBuffer promoFolderBmi = new StringBuffer();      // more efficient than string
            StringBuffer assortmentFolderBmi = new StringBuffer(); // more efficient than string 
             
            promoFolderBmi.append("D|PROMOTION_FOLDER|");  // remove the promotion folder
            promoFolderBmi.append(promotionFolder);        // ie: 11_0228T
            promoFolderBmi.append("|");                    // blue martini delimiter
            promoFolderBmi.append(promotionPath);          // promotion folder path
            promoFolderBmi.append(promotionParent);        // prmootion number
            
            assortmentFolderBmi.append("D|ASSORTMENT_FOLDER|"); // remove the promotion assortment
            assortmentFolderBmi.append(promotionFolder);        // ie: 11_0228T
            assortmentFolderBmi.append("|");                    // blue martini delimiter
            assortmentFolderBmi.append(assortmentPath);         // promo assortment path
            assortmentFolderBmi.append(promotionParent);        // promotion number
        
            delRecords++;
            pwDelFile.write("# " + file.toString() + " is old; remove \n");  // write out comment
            pwDelFile.write(promoFolderBmi.toString() + "\n");               // write out delete
            pwDelFile.write(assortmentFolderBmi.toString() + "\n");          // write out delete

		}
	}
	
	/**
	 * createInsOutput
	 * 
	 * This will iterate through the new ArrayList and write out all data to
	 * the insert bmi since they are new and do not need to be compared
	 * 
	 * 
	 * @throws IOException - if any exceptions occur then consider them fatal
	 *                       because otherwise our results will be wrong
	 *                       
	 * we're building it to look like this
	 * ---------------------------------------------------
	 * Example --- 
	 * 
	 * # 11_0309T:P2872 - adds
     * I|ASSORTMENT|AP169887581|/Assortments/Promotions/11_0309T/P2872|A|GRAND OPENING / P169887581|Percent Off: 40.000 SCL_484047||2011-03-09 00:00:00|2011-03-20 23:59:59|||
     * I|PROMOTION|P171003923|/Promotions/11_0309T/P2872|U|GRAND OPENING / P171003923|Price Point: 13.990 SKU_91242793
     * I|PROMOTION_CONDITION|P171003923|P|EQ|/PriceLists/USA_Standard|||743188||1||||PRODUCT
     * I|PROMOTION_CONDITION|P169163094|P|EQ|/PriceLists/USA_Standard|||512071||1||||PRODUCT
     * I|PROMOTION_CONDITION|P169164896|P|EQ|/PriceLists/USA_Standard|||AP169164896||1||||ASSORTMENT
     * I|PROMOTION_DISCOUNT|P171003923|SP|/PriceLists/USA_Standard||99999|||13.990|743188|99999|PA|PRODUCT
     * I|PROMOTION_DATE|P171003923|2011-03-09 00:00:00|2011-03-20 23:59:59
     * I|ASSORTMENT_CONTENTS|/Assortments/Promotions/PromotionMaster|ASSORTMENT|/Assortments/Promotions/11_0309T/P2872/AP170745823
     * S|OBJECT_ATTRIBUTE|PROMOTION|P170485656|Promo_Pricing_Type|SPO
     * S|OBJECT_ATTRIBUTE|PROMOTION|P169970103|Promo_Pricing_Type|SPP
	 */
	private void createInsOutput() throws IOException	{
		markLogUtil.logmarkGeneral("Writing new data to Insert BMI File",markLogUtil.LEVEL_CRITICAL);                // informational
		markLogUtil.logmarkGeneral("Size of new data promotions is: " + newFiles.size(),markLogUtil.LEVEL_CRITICAL); // informational
		BufferedReader inputReader = null;
		Iterator iter = newFiles.listIterator();  // convenience iterator
		Object file = null;                       // file object to read lines 
		String inputLine; // holds the line read from the file
		while(iter.hasNext()) {
			file = iter.next();  // next file in the new ArrayList
            markLogUtil.logmarkGeneral("Creating Insert BMI Entry for:" + file.toString(),markLogUtil.LEVEL_CRITICAL); // display new promotions 
            inputReader = new BufferedReader(new FileReader(new File(sNewDir,file.toString()))); // new reader
            pwAddFile.write("# " + file.toString() + " - adds \n"); // write out comment to the add file
			while ((inputLine = inputReader.readLine()) != null) { // read a line off the file
				loadRecords++;   // increment counter - this will be show in the log later so we know how many records
				                 // we have for esk073
				pwAddFile.write(inputLine + "\n");  // write out the line - it is already formatted into a load I record action type
				                                    // this was done by ESK029 promoExtract 
			}            
		}
	}
	
	/**
	 * comparePromos
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * 
	 * This does all the work for comparing hashMaps of promotions
	 * If any file or IO exceptions they are fatal since the load files must
	 * be accurate 
	 * 
	 * 
	 */
	private void comparePromos() throws FileNotFoundException, IOException {
		Iterator iter =  compareFiles.listIterator();
		Object file = null;
		while(iter.hasNext()) {
			file = iter.next();
            markLogUtil.logmarkGeneral("-----------------------------------------------------------------------",markLogUtil.LEVEL_CRITICAL);			
			markLogUtil.logmarkGeneral("Comparing Promo:" + file.toString(),markLogUtil.LEVEL_CRITICAL);
			System.out.println ("Free Mem: " + Runtime.getRuntime().freeMemory());	// show memory before and after each
			                                                                        // HashMap build process since that will 
			                                                                        // utilize the most memory
			
			buildNewPromoHash(file.toString()); // build the HashMaps for New Promotions
			
			System.out.println ("Free Mem: " + Runtime.getRuntime().freeMemory());	// show memory before and after each
                                                                                    // HashMap build process since that will 
                                                                                    // utilize the most memory
			
			buildOldPromoHash(file.toString()); // build the HashMaps for Old Promotions
			System.out.println ("Free Mem: " + Runtime.getRuntime().freeMemory());  // show memory before and after each
                                                                                    // HashMap build process since that will 
                                                                                    // utilize the most memory
			
			
			buildBasePromoChangesHash();  // find if anything changed in base promotions first
			                              // this is critical because as we process each record type this will tell us if 
			                              // we need to reload due to a promotion object change 
			
			compareAssortment(); // 
			compareAssortmentContents();
			
			/*  Start of compares that are tied to a promotion Object - via blue martini's bmi loader */
		
			comparePromo();               // compare promotion records
			comparePromoCondition();      // compare promotion condition
			comparePromoDiscount();       // compare promotion discount
			comparePromoDate();           // compare promtion date
			compareObjectAttributes();    // compare Object Attributes
			/*  end of compares that are tied to a promotion Object - via blue martini's bmi loader */

//    pke8329 - move the following to before promo compares			
//			compareAssortment(); 
//			compareAssortmentContents();
			
            markLogUtil.logmarkGeneral("---------- done with Promo:" + file.toString() + " ----------",markLogUtil.LEVEL_CRITICAL);				
		}
	}	
	
	/**
	 * buildBasePromoChangesHash
	 * 
	 * The purpose of this is to check if anything in the promotion object has changed
	 * and we need to reload
	 *
	 */
	private void buildBasePromoChangesHash() {
		int PromoBaseChanges = 0;// counts for logging number of changes found at each record level
		int PromoDiscChanges = 0;// counts for logging number of changes found at each record level
		int PromoCondChanges = 0;// counts for logging number of changes found at each record level
		int PromoDateChanges = 0;// counts for logging number of changes found at each record level
		String promoKey = null; // used to build the unique key for the promotion for the changes Hashmap
		
		
		/**
		 * P R O M O T I O N     R E C O R D
		 * 
		 *    Look for any changes to the promotion at the promotion record Level
		 */
		markLogUtil.logmarkGeneral("Looking for base promotion changes",markLogUtil.LEVEL_CRITICAL);
        Iterator oldIterKeys = hash_oldPromo.keySet().iterator();
		Object oldKey=null;
		String newKey=null;		
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next();
			newKey = oldKey.toString();
		    if(hash_newPromo.containsKey(newKey)) {
 	           if(!hash_newPromo.get(newKey).equals(hash_oldPromo.get(oldKey))) {
 	        	   markLogUtil.logmarkGeneral("Promotion Has changed:" + newKey,markLogUtil.LEVEL_DEBUG);
 	         	   markLogUtil.logmarkGeneral("new:" + hash_newPromo.get(newKey),markLogUtil.LEVEL_DEBUG);
 	        	   markLogUtil.logmarkGeneral("old:" + hash_oldPromo.get(oldKey),markLogUtil.LEVEL_DEBUG);
 	        	   // the new key will look like:  11_0228T:P2873:P1111111111  
 	        	   // this makes it unique at the promo, promoNumber and Promo ID
 	        	   promoKey=newKey.replaceAll("_PROMOTION_", ":");  // we need to build a unique key 
 	        	   markLogUtil.logmarkGeneral("promokey:"+promoKey,markLogUtil.LEVEL_CRITICAL); // display promo that changed so we know it's being reloaded
 	        	   hash_basePromoChange.put(promoKey, newKey);
 	        	   PromoBaseChanges++;  // informational count to be used for logging
 	           }		
		    }
		}
		markLogUtil.logmarkGeneral("Found:" + PromoBaseChanges + " changes",markLogUtil.LEVEL_CRITICAL);
		
		/**
		 * P R O M O T I O N    C O N D I T I O N      R E C O R D
		 * 
		 *    Look for any changes to the promotion at the promotion condition record Level
		 */
		markLogUtil.logmarkGeneral("Looking for base promotion condition changes",markLogUtil.LEVEL_CRITICAL);		
		oldIterKeys = hash_oldPromoCond.keySet().iterator();
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next();
			newKey = oldKey.toString();			
		    if(hash_newPromoCond.containsKey(newKey)) {
		       if(!hash_oldPromoCond.get(oldKey).equals(hash_newPromoCond.get(newKey))) {
 	        	   markLogUtil.logmarkGeneral("Promotion Condition Has changed:" + newKey,markLogUtil.LEVEL_CRITICAL);
 	        	   markLogUtil.logmarkGeneral("new:" + hash_newPromoCond.get(newKey),markLogUtil.LEVEL_DEBUG);
 	        	   markLogUtil.logmarkGeneral("old:" + hash_oldPromoCond.get(oldKey),markLogUtil.LEVEL_DEBUG);
 	        	   // the new key will look like:  11_0228T:P2873:P1111111111  
 	        	   // this makes it unique at the promo, promoNumber and Promo ID 	        	   
 	        	   promoKey=newKey.replaceAll("_PROMOTION_CONDITION_", ":");// we need to build a unique key
 	        	   markLogUtil.logmarkGeneral("promokey:"+promoKey,markLogUtil.LEVEL_CRITICAL);// display promo that changed so we know it's being reloaded
 	        	   hash_basePromoChange.put(promoKey, newKey); 	        	   
 	        	   PromoCondChanges++;// informational count to be used for logging
		       }
		    }
		}
		markLogUtil.logmarkGeneral("Found:" + PromoCondChanges + " changes",markLogUtil.LEVEL_CRITICAL);		
		
		/**
		 * P R O M O T I O N    D I S C O U N T      R E C O R D
		 * 
		 *    Look for any changes to the promotion at the promotion discount record Level
		 */
		markLogUtil.logmarkGeneral("Looking for base promotion discount changes",markLogUtil.LEVEL_CRITICAL);		
		oldIterKeys = hash_oldPromoDisc.keySet().iterator();
		while(oldIterKeys.hasNext()) {
			oldKey = oldIterKeys.next();
			newKey = oldKey.toString();			
		    if(hash_newPromoDisc.containsKey(newKey)) {
		       if(!hash_oldPromoDisc.get(oldKey).equals(hash_newPromoDisc.get(newKey))) {
 	        	   markLogUtil.logmarkGeneral("Promotion Discount Has changed:" + newKey,markLogUtil.LEVEL_CRITICAL);
 	        	   markLogUtil.logmarkGeneral("new:" + hash_newPromoDisc.get(newKey),markLogUtil.LEVEL_DEBUG);
 	        	   markLogUtil.logmarkGeneral("old:" + hash_oldPromoDisc.get(oldKey),markLogUtil.LEVEL_DEBUG);
 	        	   // the new key will look like:  11_0228T:P2873:P1111111111  
 	        	   // this makes it unique at the promo, promoNumber and Promo ID 	        	    	        	   
 	        	   promoKey=newKey.replaceAll("_PROMOTION_DISCOUNT_", ":");// we need to build a unique key
 	        	   markLogUtil.logmarkGeneral("promokey:"+promoKey,markLogUtil.LEVEL_CRITICAL); // display promo that changed so we know it's being reloaded	
 	        	   hash_basePromoChange.put(promoKey, newKey); 	        	   
 	        	   PromoDiscChanges++;// informational count to be used for logging
		       }
		    }
		}
		markLogUtil.logmarkGeneral("Found:" + PromoDiscChanges + " changes",markLogUtil.LEVEL_CRITICAL);

		/**
		 * P R O M O T I O N    D A T E      R E C O R D
		 * 
		 *    Look for any changes to the promotion at the promotion date record Level
		 */		
		markLogUtil.logmarkGeneral("Looking for base promotion date changes",markLogUtil.LEVEL_CRITICAL);		
		oldIterKeys = hash_oldPromoDate.keySet().iterator();
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next();
			newKey = oldKey.toString();			
		    if(hash_newPromoDate.containsKey(newKey)) {
		       if(!hash_oldPromoDate.get(oldKey).equals(hash_newPromoDate.get(newKey))) {
 	        	   markLogUtil.logmarkGeneral("Promotion Date Has changed:" + newKey,markLogUtil.LEVEL_DEBUG);
 	        	   markLogUtil.logmarkGeneral("new:" + hash_newPromoDate.get(newKey),markLogUtil.LEVEL_DEBUG);
 	        	   markLogUtil.logmarkGeneral("old:" + hash_oldPromoDate.get(oldKey),markLogUtil.LEVEL_DEBUG);
 	        	   promoKey=newKey.replaceAll("_PROMOTION_DATE_", ":");// we need to build a unique key
 	        	   markLogUtil.logmarkGeneral("promokey:"+promoKey,markLogUtil.LEVEL_DEBUG);// display promo that changed so we know it's being reloaded 	
 	        	   hash_basePromoChange.put(promoKey, newKey); // add to the change hashMap 
 	        	                                               // if it's there already it just updates the existing one
 	        	   PromoDateChanges++;// informational count to be used for logging
		       }
		    }
		}
		markLogUtil.logmarkGeneral("Found:" + PromoDateChanges + " changes",markLogUtil.LEVEL_CRITICAL);		
		markLogUtil.logmarkGeneral("Number of promotions that have changed:" + hash_basePromoChange.size(),markLogUtil.LEVEL_CRITICAL);
	}
	
	/**
	 * comparePromo
	 *
	 * This Compares Promotion Record Changes Only using the 
	 * changes HashMap,  oldPromo HashMap and newPromo HashMap
	 * 
	 * if any promotion is contained in the changed promo hashMap it must be 
	 * reloaded - per blue martini bmi rules
	 *
	 */
	private void comparePromo() {
        markLogUtil.logmarkGeneral(" -- Comparing Promotion  --",markLogUtil.LEVEL_CRITICAL);
		/**
		 * PROMOTION Records
		 * If the promotion --- checking on promotion_condition, promotion_discount & promotion_date 
		 * changes then it must be deleted and reinserted along with
		 * the promotion_condition, promotion_discount and promotion_date records associated
		 * 
		 * I|PROMOTION|P168724186|/Promotions/11_0228T/P2866|U|NOT ADV SALE / P168724186|BOGO %: SKU_87126892
         * I|PROMOTION_CONDITION|P168724186|BC|GE|/PriceLists/USA_Standard|||AP168724186||2||||ASSORTMENT
         * I|PROMOTION_DISCOUNT|P168724186|CD|/PriceLists/USA_Standard||99999||50.000%||AP168724186|1|DP|ASSORTMENT
         * I|PROMOTION_DATE|P168724186|2011-02-28 00:00:00|2011-03-03 23:59:59
		 */
        Iterator oldIterKeys = hash_oldPromo.keySet().iterator();
		Object oldKey=null;  // key to iterator through from old HashMap 
		String newKey=null;  // String type for new hashMap - 
		String promoKey=null; // PromoKey for changes HashMap 
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next(); // keySet from old HashMap
			newKey = oldKey.toString();  // just a string type - could do a toString instead but the newkey made it easier
			                             // when looking at the code
		    if(hash_newPromo.containsKey(newKey)){  // if the new promo has the key then it must be compared
	       	   promoKey=newKey.replaceAll("_PROMOTION_", ":");  // build unique promotion key
 	           if(hash_basePromoChange.containsKey(promoKey)) { // check to see if the changes HashMap contains this promotion
 	        	   markLogUtil.logmarkGeneral("--- Base promo change ---",markLogUtil.LEVEL_CRITICAL);  // if the key is found reload the record
  	 	    	   delRecords++;  // informational count for log
                   pwDelFile.write("D" + hash_oldPromo.get(oldKey).toString().substring(1,hash_oldPromo.get(oldKey).toString().length()) + "\n"); // del bmi record  
 			       oldIterKeys.remove(); // remove this record from the hashMap - when we write out the counts we'll just know we processed them all  			      
 			       loadRecords++; // informational count for log
 			       pwAddFile.write(hash_newPromo.get(newKey).toString() + "\n"); // write out the bmi 
	    	       hash_newPromo.remove(newKey); // this must be removed because we'll iterate through new promotion records at the
 	           }
 	           else if(hash_newPromo.get(newKey).equals(hash_oldPromo.get(oldKey))) { // check for changes - should not find any 	        	                                                                            
		    	       oldIterKeys.remove();   // no changes so remove from both Hashmaps
		    	       hash_newPromo.remove(newKey);// this must be removed because we'll iterate through new promotion records at the
 	                }
		    } 
		    else{
 	    	    delRecords++;  // if a promotion was in a file (should not happen) and was in the old but not new we need to remove it
                               // this mostly happens with records at lower levels ie: assortments
            	pwDelFile.write("D" + hash_oldPromo.get(oldKey).toString().substring(1,hash_oldPromo.get(oldKey).toString().length()) + "\n");
		    	oldIterKeys.remove(); // remove from the old hashMap so when we do counts we will show zero that we processed them all
		    }		    
		}
		markLogUtil.logmarkGeneral("hash_oldPromo size after Compare..............:" + hash_oldPromo.size(),markLogUtil.LEVEL_CRITICAL); // informational
		markLogUtil.logmarkGeneral("hash_newPromo size after Compare..............:" + hash_newPromo.size(),markLogUtil.LEVEL_CRITICAL); // informational		
		Iterator newIterKeys = hash_newPromo.keySet().iterator(); // iterate through "new" records no compares needed
		                                                          // these are new promotion records (differant than new Promotions)
		                                                          // (new promotions create new files)  - new records will be only
		                                                          // in the new hashMap and not old and do not need to be compared
		while(newIterKeys.hasNext()) {			
			loadRecords++;			
        	pwAddFile.write(hash_newPromo.get(newIterKeys.next()).toString() + "\n");
			newIterKeys.remove();  // remove so at the end the count is zero showing we processed them all 
		}
		markLogUtil.logmarkGeneral("hash_oldPromo size after Final Write..............:" + hash_oldPromo.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newPromo size after Final Write..............:" + hash_newPromo.size(),markLogUtil.LEVEL_CRITICAL);
		
	}
	
	/**
	 * comparePromoCondition
	 * 
	 * This Compares Promotion Condition Record Changes Only using the 
	 * changes HashMap,  oldPromoCond HashMap and newPromoCond HashMap
	 * 
	 * if any promotion is contained in the changed promo hashMap it must be 
	 * reloaded - per blue martini bmi rules
	 *
	 */
	private void comparePromoCondition() {
        markLogUtil.logmarkGeneral(" -- Comparing Promotion Condition --",markLogUtil.LEVEL_CRITICAL);

		/**
		 * PROMOTION Records
		 * If the promotion --- checking on promotion_condition, promotion_discount & promotion_date 
		 * changes then it must be deleted and reinserted along with
		 * the promotion_condition, promotion_discount and promotion_date records associated
		 * 
		 * I|PROMOTION|P168724186|/Promotions/11_0228T/P2866|U|NOT ADV SALE / P168724186|BOGO %: SKU_87126892
         * I|PROMOTION_CONDITION|P168724186|BC|GE|/PriceLists/USA_Standard|||AP168724186||2||||ASSORTMENT
         * I|PROMOTION_DISCOUNT|P168724186|CD|/PriceLists/USA_Standard||99999||50.000%||AP168724186|1|DP|ASSORTMENT
         * I|PROMOTION_DATE|P168724186|2011-02-28 00:00:00|2011-03-03 23:59:59
		 */
		// Rule --- promo condition, date, are all tied to promotion
		// the promotion is deleted and re-inserted for anything to change
		String promoKey = null;// PromoKey for changes HashMap
		Iterator oldIterKeys = hash_oldPromoCond.keySet().iterator();
		Object oldKey=null;// key to iterator through from old HashMap
		String newKey=null;// String type for new hashMap -		
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next();// keySet from old HashMap
			newKey = oldKey.toString();// just a string type - could do a toString instead but the newkey made it easier
                                       // when looking at the code			
        // use this for debugging if needed			
		//	markLogUtil.logmarkGeneral("Cond old:" + hash_oldPromoCond.get(oldKey));
		//  markLogUtil.logmarkGeneral("Cond new:" + hash_newPromoCond.get(newKey));			
       	    promoKey=newKey.replaceAll("_PROMOTION_CONDITION_", ":");// build unique promotion key
		    if(hash_newPromoCond.containsKey(newKey)) {// if the new promo has the key then it must be compared
		    	if(hash_basePromoChange.containsKey(promoKey)) {// check to see if the changes HashMap contains this promotion
		    		markLogUtil.logmarkGeneral("--- Base promo change ---",markLogUtil.LEVEL_CRITICAL); // the promotion was removed so just re-add it
		    		                                                 // since removing the promotion removes the condition we only 
		    		                                                 // need to re-add
		    		oldIterKeys.remove(); 	// remove this record from the hashMap - when we write out the counts we'll just know we processed them all		      
		    		loadRecords++;// informational count for log
		    		pwAddFile.write(hash_newPromoCond.get(newKey).toString() + "\n");// write out the bmi
      	            hash_newPromoCond.remove(newKey);// this must be removed because we'll iterate through new promotion records at the end
                                                     // this must be removed because we'll iterate through new promotion records at the      	            
		    	} else if(hash_oldPromoCond.get(oldKey).equals(hash_newPromoCond.get(newKey))) // check for changes - should not find any
		    	       {
		    	          hash_newPromoCond.remove(newKey);// no changes so remove from both Hashmaps
		    	          oldIterKeys.remove();
 	        	        }
		    }
 	        else {  // no need to delete since promo will do it
		    	oldIterKeys.remove();		    	   	    	
		    }		    
		}
		markLogUtil.logmarkGeneral("hash_oldPromoCond size after Compare..........:" + hash_oldPromoCond.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newPromoCond size after Compare..........:" + hash_newPromoCond.size(),markLogUtil.LEVEL_CRITICAL);
		Iterator newIterKeys = hash_newPromoCond.keySet().iterator();
		while(newIterKeys.hasNext()) {// iterate through "new" records no compares needed	
                                      // these are new promotion records (differant than new Promotions)
                                      // (new promotions create new files)  - new records will be only
                                      // in the new hashMap and not old and do not need to be compared		
			loadRecords++;			
     		pwAddFile.write(hash_newPromoCond.get(newIterKeys.next()).toString() + "\n");
			newIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		}
		markLogUtil.logmarkGeneral("hash_oldPromoCond size after Final Write......:" + hash_oldPromoCond.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newPromoCond size after Final Write......:" + hash_newPromoCond.size(),markLogUtil.LEVEL_CRITICAL);

	}
	
	
	/**
	 * comparePromoDiscount
	 *
 	 * This Compares Promotion Discount Record Changes Only using the 
	 * changes HashMap,  oldPromoDisc HashMap and newPromoDisc HashMap
	 * 
	 * if any promotion is contained in the changed promo hashMap it must be 
	 * reloaded - per blue martini bmi rules
	 *
	 */
	private void comparePromoDiscount() {
        markLogUtil.logmarkGeneral(" -- Comparing Promotion Discount --",markLogUtil.LEVEL_CRITICAL);
		/**
		 * PROMOTION Records
		 * If the promotion --- checking on promotion_condition, promotion_discount & promotion_date 
		 * changes then it must be deleted and reinserted along with
		 * the promotion_condition, promotion_discount and promotion_date records associated
		 * 
		 * I|PROMOTION|P168724186|/Promotions/11_0228T/P2866|U|NOT ADV SALE / P168724186|BOGO %: SKU_87126892
         * I|PROMOTION_CONDITION|P168724186|BC|GE|/PriceLists/USA_Standard|||AP168724186||2||||ASSORTMENT
         * I|PROMOTION_DISCOUNT|P168724186|CD|/PriceLists/USA_Standard||99999||50.000%||AP168724186|1|DP|ASSORTMENT
         * I|PROMOTION_DATE|P168724186|2011-02-28 00:00:00|2011-03-03 23:59:59
		 */
		// Rule --- promo condition, date, are all tied to promotion
		// the promotion is deleted and re-inserted for anything to change        
        String promoKey=null;// PromoKey for changes HashMap
		Object oldKey=null;// key to iterator through from old HashMap
		String newKey=null;// String type for new hashMap -
		Iterator oldIterKeys = hash_oldPromoDisc.keySet().iterator();
		while(oldIterKeys.hasNext()) {
			oldKey = oldIterKeys.next();// keySet from old HashMap
			newKey = oldKey.toString();// just a string type - could do a toString instead but the newkey made it easier
                                       // when looking at the code						
       	    promoKey=newKey.replaceAll("_PROMOTION_DISCOUNT_", ":");// build unique promotion key		
		    if(hash_newPromoDisc.containsKey(newKey)) {// if the new promo has the key then it must be compared
	 	       if(hash_basePromoChange.containsKey(promoKey)) {// check to see if the changes HashMap contains this promotion
	  	          markLogUtil.logmarkGeneral("--- Base promo change ---",markLogUtil.LEVEL_CRITICAL); // the promotion was removed so just re-add it
                                                                   // since removing the promotion removes the condition we only 
                                                                   // need to re-add	  	          
		    	  oldIterKeys.remove(); // remove this record from the hashMap - when we write out the counts we'll just know we processed them all			      
		    	  loadRecords++;// informational count for log
		    	  pwAddFile.write(hash_newPromoDisc.get(newKey).toString() + "\n");// write out the bmi
	    	      hash_newPromoDisc.remove(newKey);// this must be removed because we'll iterate through new promotion records at the end
                                                   // this must be removed because we'll iterate through new promotion records at the      	            	    	      
	  	       }
	 	       else if(hash_oldPromoDisc.get(oldKey).equals(hash_newPromoDisc.get(newKey))) { // check for changes - should not find any
		    	       oldIterKeys.remove();// no changes so remove from both Hashmaps
		    	       hash_newPromoDisc.remove(newKey);
		            } 
		    }
		    else {   // no need to delete since promo will do it
		    	oldIterKeys.remove();// remove so at the end the count is zero showing we processed them all		    	   	    			    	
		    }		    
		}
		markLogUtil.logmarkGeneral("hash_oldPromoDisc size after Compare..........:" + hash_oldPromoDisc.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newPromoDisc size after Compare..........:" + hash_newPromoDisc.size(),markLogUtil.LEVEL_CRITICAL);
		Iterator newIterKeys = hash_newPromoDisc.keySet().iterator();
		while(newIterKeys.hasNext()) {	// iterate through "new" records no compares needed
                                        // these are new promotion records (differant than new Promotions)
                                        // (new promotions create new files)  - new records will be only
                                        // in the new hashMap and not old and do not need to be compared	
			loadRecords++;			
			pwAddFile.write(hash_newPromoDisc.get(newIterKeys.next()).toString() + "\n");
			newIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		}
		markLogUtil.logmarkGeneral("hash_oldPromoDisc size after Final Write......:" + hash_oldPromoDisc.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newPromoDisc size after Final Write......:" + hash_newPromoDisc.size(),markLogUtil.LEVEL_CRITICAL);
	}

	
	/**
	 * comparePromoDate
	 * 
	 * 
 	 * This Compares Promotion Date Record Changes Only using the 
	 * changes HashMap,  oldPromoDate HashMap and newPromoDate HashMap
	 * 
	 * if any promotion is contained in the changed promo hashMap it must be 
	 * reloaded - per blue martini bmi rules
	 *
	 *
	 */
	private void comparePromoDate() {
        markLogUtil.logmarkGeneral(" -- Comparing Promotion Date --",markLogUtil.LEVEL_CRITICAL);
		/**
		 * PROMOTION Records
		 * If the promotion --- checking on promotion_condition, promotion_discount & promotion_date 
		 * changes then it must be deleted and reinserted along with
		 * the promotion_condition, promotion_discount and promotion_date records associated
		 * 
		 * I|PROMOTION|P168724186|/Promotions/11_0228T/P2866|U|NOT ADV SALE / P168724186|BOGO %: SKU_87126892
         * I|PROMOTION_CONDITION|P168724186|BC|GE|/PriceLists/USA_Standard|||AP168724186||2||||ASSORTMENT
         * I|PROMOTION_DISCOUNT|P168724186|CD|/PriceLists/USA_Standard||99999||50.000%||AP168724186|1|DP|ASSORTMENT
         * I|PROMOTION_DATE|P168724186|2011-02-28 00:00:00|2011-03-03 23:59:59
		 */
		// Rule --- promo condition, date, are all tied to promotion
		// the promotion is deleted and re-inserted for anything to change                
        String promoKey=null;// PromoKey for changes HashMap
		Object oldKey=null;// key to iterator through from old HashMap
		String newKey=null;// String type for new hashMap -
		Iterator oldIterKeys = hash_oldPromoDate.keySet().iterator();
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next();// keySet from old HashMap
			newKey = oldKey.toString();	// just a string type - could do a toString instead but the newkey made it easier	
                                        // when looking at the code									
       	    promoKey=newKey.replaceAll("_PROMOTION_DATE_", ":");// build unique promotion key
		    if(hash_newPromoDate.containsKey(newKey)) {// if the new promo has the key then it must be compared
	 	        if(hash_basePromoChange.containsKey(promoKey)) {// check to see if the changes HashMap contains this promotion
	  	          markLogUtil.logmarkGeneral("--- Base promo change ---",markLogUtil.LEVEL_CRITICAL);  // the promotion was removed so just re-add it
                                                                    // since removing the promotion removes the condition we only 
                                                                    // need to re-add	  	          	  	          
		    	  oldIterKeys.remove(); // remove this record from the hashMap - when we write out the counts we'll just know we processed them all			      
		    	  loadRecords++;// informational count for log
		    	  pwAddFile.write(hash_newPromoDate.get(newKey).toString() + "\n");
    	          hash_newPromoDate.remove(newKey);	// this must be removed because we'll iterate through new promotion records at the end
                                                    // this must be removed because we'll iterate through new promotion records at the      	            	    	          	         
	  	        } else if(hash_oldPromoDate.get(oldKey).equals(hash_newPromoDate.get(newKey))) {// check for changes - should not find any
		    	          oldIterKeys.remove();// no changes so remove from both Hashmaps
		    	          hash_newPromoDate.remove(newKey);// this must be removed because we'll iterate through new promotion records at the end
		    		   } 
		    } else {  // no need to delete since promo will do it
      	        oldIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		    }		    
		}
		markLogUtil.logmarkGeneral("hash_oldPromoDate size after Compare..........:" + hash_oldPromoDate.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newPromoDate size after Compare..........:" + hash_newPromoDate.size(),markLogUtil.LEVEL_CRITICAL);
		Iterator newIterKeys = hash_newPromoDate.keySet().iterator();
		while(newIterKeys.hasNext()) {	// iterate through "new" records no compares needed		
                                        // these are new promotion records (differant than new Promotions)
                                        // (new promotions create new files)  - new records will be only
                                        // in the new hashMap and not old and do not need to be compared	
			loadRecords++;			
			pwAddFile.write(hash_newPromoDate.get(newIterKeys.next()).toString() + "\n");
			newIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		}
		markLogUtil.logmarkGeneral("hash_oldPromoDate size after Final Write......:" + hash_oldPromoDate.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newPromoDate size after Final Write......:" + hash_newPromoDate.size(),markLogUtil.LEVEL_CRITICAL);

	}
		
	/**
	 * compareAssortment
	 * 
 	 * This Compares Assortment Record Changes Only using the 
	 * oldAssortment HashMap and newAssortment HashMap
	 * 
	 * This record type does not need to adhere to the promotion object changes 
	 */
	private void compareAssortment() {
	    markLogUtil.logmarkGeneral(" -- Comparing Assortment --",markLogUtil.LEVEL_CRITICAL);

		/**
		 * PROMOTION Records
		 * If the promotion --- checking on promotion_condition, promotion_discount & promotion_date 
		 * changes then it must be deleted and reinserted along with
		 * the promotion_condition, promotion_discount and promotion_date records associated
		 * 
		 * I|PROMOTION|P168724186|/Promotions/11_0228T/P2866|U|NOT ADV SALE / P168724186|BOGO %: SKU_87126892
	     * I|PROMOTION_CONDITION|P168724186|BC|GE|/PriceLists/USA_Standard|||AP168724186||2||||ASSORTMENT
	     * I|PROMOTION_DISCOUNT|P168724186|CD|/PriceLists/USA_Standard||99999||50.000%||AP168724186|1|DP|ASSORTMENT
	     * I|PROMOTION_DATE|P168724186|2011-02-28 00:00:00|2011-03-03 23:59:59
		 */
		Object oldKey=null;// key to iterator through from old HashMap
		String newKey=null;	// String type for new hashMap -	
	    Iterator oldIterKeys = hash_oldAssortment.keySet().iterator();
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next();// keySet from old HashMap
			newKey = oldKey.toString();	// just a string type - could do a toString instead but the newkey made it easier when  looking at the code		
		    if(hash_newAssortment.containsKey(newKey)) {// if the new promo has the key then it must be compared
		       if(hash_oldAssortment.get(oldKey).equals(hash_newAssortment.get(newKey))) {// check for any changes to record
		    	   oldIterKeys.remove();// remove this record from the hashMap - when we write out the counts we'll just know we processed them all
		    	   hash_newAssortment.remove(newKey);// this must be removed because we'll iterate through new records at the end
		       } else {
		    	   oldIterKeys.remove();// we don't need to delete we can do an update to this record Type but we need to remove from HashMap so 
		    	                        // counts show we processed them all
		    	   loadRecords++; // informational for log so we know how many esk073 has to do 
		    	   pwAddFile.write("U" + hash_newAssortment.get(newKey).toString().substring(1,hash_newAssortment.get(newKey).toString().length()) + "\n");		    	   		    	   		    	   
		    	   hash_newAssortment.remove(newKey);// this must be removed because we'll iterate through new records at the end
		       }		    
		    } else {// must delete since it is not tied to promo object
	 	   	    delRecords++;	// informational for logging so we know how many esk010 has to do    
		    	pwDelFile.write("D" + hash_oldAssortment.get(oldKey).toString().substring(1,hash_oldAssortment.get(oldKey).toString().length()) + "\n"); 	    	     	    	     	    	     	    	    
		    	   oldIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		    }		    
		}
		markLogUtil.logmarkGeneral("hash_oldAssortment size after Compare.........:" + hash_oldAssortment.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newAssortment size after Compare.........:" + hash_newAssortment.size(),markLogUtil.LEVEL_CRITICAL);
		Iterator newIterKeys = hash_newAssortment.keySet().iterator();
		while(newIterKeys.hasNext()) {	// iterate through "new" records no compares needed
                                        // these are new promotion records (differant than new Promotions)
                                        // (new promotions create new files)  - new records will be only
                                        // in the new hashMap and not old and do not need to be compared	
			loadRecords++;			
			pwAddFile.write(hash_newAssortment.get(newIterKeys.next()).toString() + "\n");
			newIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		}
		markLogUtil.logmarkGeneral("hash_oldAssortment size after Final Write.....:" + hash_oldAssortment.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newAssortment size after Final Write.....:" + hash_newAssortment.size(),markLogUtil.LEVEL_CRITICAL);
	}
		
	/**
	 * compareAssortmentContents
	 * 
 	 * This Compares AssortmentContents Record Changes Only using the 
	 * oldAssortmentContent HashMap and newAssortmentContent HashMap
	 * 
	 * This record type does not need to adhere to the promotion object changes 
	 *
	 */
	private void compareAssortmentContents() {
        markLogUtil.logmarkGeneral(" -- Comparing Assortment Contents --",markLogUtil.LEVEL_CRITICAL);
		/**
		 * PROMOTION Records
		 * If the promotion --- checking on promotion_condition, promotion_discount & promotion_date 
		 * changes then it must be deleted and reinserted along with
		 * the promotion_condition, promotion_discount and promotion_date records associated
		 * 
		 * I|PROMOTION|P168724186|/Promotions/11_0228T/P2866|U|NOT ADV SALE / P168724186|BOGO %: SKU_87126892
         * I|PROMOTION_CONDITION|P168724186|BC|GE|/PriceLists/USA_Standard|||AP168724186||2||||ASSORTMENT
         * I|PROMOTION_DISCOUNT|P168724186|CD|/PriceLists/USA_Standard||99999||50.000%||AP168724186|1|DP|ASSORTMENT
         * I|PROMOTION_DATE|P168724186|2011-02-28 00:00:00|2011-03-03 23:59:59
		 */
		Iterator oldIterKeys = hash_oldAssortmentContents.keySet().iterator();
		Object oldKey=null;// key to iterator through from old HashMap
		String newKey=null;	// String type for new hashMap -	
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next();// keySet from old HashMap
			newKey = oldKey.toString();	// just a string type - could do a toString instead but the newkey made it easier when  looking at the code		
		    if(hash_newAssortmentContents.containsKey(newKey)) {// if the new promo has the key then it must be compared
		       if(hash_oldAssortmentContents.get(oldKey).equals(hash_newAssortmentContents.get(newKey))) {
		    	   oldIterKeys.remove();// remove this record from the hashMap - when we write out the counts we'll just know we processed them all
		    	   hash_newAssortmentContents.remove(newKey);// this must be removed because we'll iterate through new records at the end
		       } else {
		    	   oldIterKeys.remove();// we don't need to delete we can do an update to this record Type but we need to remove from HashMap so
		    	   loadRecords++;	// informational for logging so we know how many esk073 has to do	    	   
		    	   pwAddFile.write("U" + hash_newAssortmentContents.get(newKey).toString().substring(1,hash_newAssortmentContents.get(newKey).toString().length()) + "\n");		    	   		    	   		    	   		    	   
		    	   hash_newAssortmentContents.remove(newKey);// this must be removed because we'll iterate through new records at the end
		       }		    
		    } else {// must delete since it is not tied to promo object
	    	    delRecords++;// informational for logging so we know how many esk010 has to do	    	 
		    	pwDelFile.write("D" + hash_oldAssortmentContents.get(oldKey).toString().substring(1,hash_oldAssortmentContents.get(oldKey).toString().length()) + "\n"); 	    	     	    	     	    	     	    	    
	    	   oldIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		    }		    
		}
		markLogUtil.logmarkGeneral("hash_oldAssortmentContents size after Compare.:" + hash_oldAssortmentContents.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newAssortmentContents size after Compare.:" + hash_newAssortmentContents.size(),markLogUtil.LEVEL_CRITICAL);
		Iterator newIterKeys = hash_newAssortmentContents.keySet().iterator();
		while(newIterKeys.hasNext()) {	// iterate through "new" records no compares needed
                                        // these are new promotion records (differant than new Promotions)
                                        // (new promotions create new files)  - new records will be only
                                        // in the new hashMap and not old and do not need to be compared
			loadRecords++;
			pwAddFile.write(hash_newAssortmentContents.get(newIterKeys.next()).toString() + "\n");
			newIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		}
		markLogUtil.logmarkGeneral("hash_oldAssortmentContents size after Final Write:" + hash_oldAssortmentContents.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newAssortmentContents size after Final Write:" + hash_newAssortmentContents.size(),markLogUtil.LEVEL_CRITICAL);
	}
		
	/**
	 * compareObjectAttributes
	 *
	 * 
 	 * This Compares Object Attribute Record Changes Only using the 
	 * oldObjectAttribute HashMap and newObjectAttribute HashMap
	 * 
	 * if any promotion is contained in the changed promo hashMap it must be 
	 * reloaded - per blue martini bmi rules
	 *
	 */
	private void compareObjectAttributes() {
        markLogUtil.logmarkGeneral(" -- Comparing Object Attributes --",markLogUtil.LEVEL_CRITICAL);
		/**
		 * PROMOTION Records
		 * If the promotion --- checking on promotion_condition, promotion_discount & promotion_date 
		 * changes then it must be deleted and reinserted along with
		 * the promotion_condition, promotion_discount and promotion_date records associated
		 * 
		 * I|PROMOTION|P168724186|/Promotions/11_0228T/P2866|U|NOT ADV SALE / P168724186|BOGO %: SKU_87126892
         * I|PROMOTION_CONDITION|P168724186|BC|GE|/PriceLists/USA_Standard|||AP168724186||2||||ASSORTMENT
         * I|PROMOTION_DISCOUNT|P168724186|CD|/PriceLists/USA_Standard||99999||50.000%||AP168724186|1|DP|ASSORTMENT
         * I|PROMOTION_DATE|P168724186|2011-02-28 00:00:00|2011-03-03 23:59:59
		 */
		Iterator oldIterKeys = hash_oldObjectAttribute.keySet().iterator();
		Object oldKey=null;// key to iterator through from old HashMap
		String newKey=null;	// String type for new hashMap -	
		String promoKey=null;// PromoKey for changes HashMap
		while(oldIterKeys.hasNext()){
			oldKey = oldIterKeys.next();// keySet from old HashMap
			newKey = oldKey.toString();// just a string type - could do a toString instead but the newkey made it easier when  looking at the code
			if(newKey.indexOf("_OBJECT_ATTRIBUTE_PROMOTION_") > 0) { // must check if this Object Attribute is tied to a promotion - since there are other types of object Attributes
				promoKey=newKey.replaceAll("_OBJECT_ATTRIBUTE_PROMOTION_", ":"); // build changes key
				promoKey=promoKey.substring(0, promoKey.lastIndexOf("|"));  // build changes key
			}
    // use this for debugging if needed			
	//		markLogUtil.logmarkGeneral("OldKey:" + oldKey.toString());			
	//		markLogUtil.logmarkGeneral("promoKey:" + promoKey);						
			if(hash_newObjectAttribute.containsKey(newKey)) {// if the new hash has the key then it must be compared
				if(hash_basePromoChange.containsKey(promoKey)) {// check to see if the changes HashMap contains this promotion
					markLogUtil.logmarkGeneral("--- Base promo change object attribute ---",markLogUtil.LEVEL_CRITICAL); // the promotion was removed so just re-add it
			    	delRecords++;// informational for logging so we know how many esk010 has to do
	     		    pwDelFile.write("D" + hash_oldObjectAttribute.get(oldKey).toString().substring(1,hash_oldObjectAttribute.get(oldKey).toString().length()) + "\n");
	    		    oldIterKeys.remove();// remove so at the end the count is zero showing we processed them all  			      
	    	 	    loadRecords++;// informational so at the end we know how many esk073 has to process
	    		    pwAddFile.write(hash_newObjectAttribute.get(newKey).toString() + "\n");// write bmi load record
	    		    hash_newObjectAttribute.remove(newKey);// this must be removed because we'll iterate through new records at the end
	    	     } else if(hash_oldObjectAttribute.get(oldKey).equals(hash_newObjectAttribute.get(newKey))) 
	    	            {
	    	    	       hash_newObjectAttribute.remove(newKey);// this must be removed because we'll iterate through new records at the end
	    	               oldIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		                } else {
		    	           loadRecords++;// informational so at the end we know how many esk073 has to process	
		    	           pwAddFile.write("S" + hash_newObjectAttribute.get(newKey).toString().substring(1,hash_newObjectAttribute.get(newKey).toString().length()) + "\n");		    	   		    	   		    	   		    	   		    	   
		    	           oldIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		    	           hash_newObjectAttribute.remove(oldKey);// remove so at the end the count is zero showing we processed them all
		                }		    
	        }
	        else {  
	    	    delRecords++;// informational for logging so we know how many esk010 has to do
		    	pwDelFile.write("D" + hash_oldObjectAttribute.get(oldKey).toString().substring(1,hash_oldObjectAttribute.get(oldKey).toString().length()) + "\n"); 	    	     	    	    
	    	    oldIterKeys.remove();// remove so at the end the count is zero showing we processed them all		    	   	    	
	        }		    
		}
		
		// write any remaining ObjectAttributes (new ones)
		markLogUtil.logmarkGeneral("hash_oldObjectAttribute size after Compare....:" + hash_oldObjectAttribute.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newObjectAttribute size after Compare....:" + hash_newObjectAttribute.size(),markLogUtil.LEVEL_CRITICAL);
		Iterator newIterKeys = hash_newObjectAttribute.keySet().iterator();
		while(newIterKeys.hasNext()) {	// iterate through "new" records no compares needed
                                        // these are new promotion records (differant than new Promotions)
                                        // (new promotions create new files)  - new records will be only
                                        // in the new hashMap and not old and do not need to be compared						
			pwAddFile.write(hash_newObjectAttribute.get(newIterKeys.next()).toString() + "\n");
			newIterKeys.remove();// remove so at the end the count is zero showing we processed them all
		}
		markLogUtil.logmarkGeneral("hash_oldObjectAttribute size after Final Write:" + hash_oldObjectAttribute.size(),markLogUtil.LEVEL_CRITICAL);
		markLogUtil.logmarkGeneral("hash_newObjectAttribute size after Final Write:" + hash_newObjectAttribute.size(),markLogUtil.LEVEL_CRITICAL);
	}	
	
		
	/**
	 * buildNewPromoHash
	 * 
	 * 
	 *  Get all the old and new data for the specified Promo ID
	 *  getdata creates a hash (OLD or NEW) of hashes for each record type 
	 *  ('PROMOTION', 'ASSORTMENT', etc.) 
     * 	getdata("$olddir/$pid","OLD"); 
     *
     * I|ASSORTMENT|AP169887581|/Assortments/Promotions/11_0309T/P2872|A|GRAND OPENING / P169887581|Percent Off: 40.000 SCL_484047||2011-03-09 00:00:00|2011-03-20 23:59:59|||
     * I|PROMOTION|P171003923|/Promotions/11_0309T/P2872|U|GRAND OPENING / P171003923|Price Point: 13.990 SKU_91242793
     * I|PROMOTION_CONDITION|P171003923|P|EQ|/PriceLists/USA_Standard|||743188||1||||PRODUCT
     * I|PROMOTION_CONDITION|P169163094|P|EQ|/PriceLists/USA_Standard|||512071||1||||PRODUCT
     * I|PROMOTION_CONDITION|P169164896|P|EQ|/PriceLists/USA_Standard|||AP169164896||1||||ASSORTMENT
     * I|PROMOTION_DISCOUNT|P171003923|SP|/PriceLists/USA_Standard||99999|||13.990|743188|99999|PA|PRODUCT
     * I|PROMOTION_DATE|P171003923|2011-03-09 00:00:00|2011-03-20 23:59:59
     * I|ASSORTMENT_CONTENTS|/Assortments/Promotions/PromotionMaster|ASSORTMENT|/Assortments/Promotions/11_0309T/P2872/AP170745823
     * S|OBJECT_ATTRIBUTE|PROMOTION|P170485656|Promo_Pricing_Type|SPO
     * S|OBJECT_ATTRIBUTE|PROMOTION|P169970103|Promo_Pricing_Type|SPP
     *
	 * @param promo
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void buildNewPromoHash(String promo) throws FileNotFoundException, IOException {
		
		BufferedReader inputReader = null; // for reading the promotion files created by ES_promo_parse.pl
		String inputLine;	               // input line for file read
		String key = null;                 // key for the hash
        String actionType = null;          // record Action type - not used currently
        String recordType = null;          // RecordType - ie: promotion, assortment, etc
        String f3 = null;                  // varies based on recordtype
        String f4 = null;                  // varies based on recordType 
        StringTokenizer st = null;         // tokenizer to create unique key for hash
        markLogUtil.logmarkGeneral("Working Directory:" + sNewDir + ", file:" + promo,markLogUtil.LEVEL_CRITICAL); // informational for log
        int recordsRead = 0; // used otherwise the log pauses for too long - display counts record
        inputReader = new BufferedReader(new FileReader(new File(sNewDir,promo))); 
		while ((inputLine = inputReader.readLine()) != null) {			
			if(recordsRead % 50000 == 0) { // display current read count every 50,000 records
				markLogUtil.logmarkGeneral("Processed: " + recordsRead + " input records",markLogUtil.LEVEL_CRITICAL);
			}
			recordsRead++;
			try	{
				st = new StringTokenizer(inputLine, "|");
				actionType = st.nextToken();    
				recordType = st.nextToken();   
				f3 = st.nextToken();
				f4 = st.nextToken();
	            if (recordType.equalsIgnoreCase("PROMOTION")) {
	            	// key:11_0228T:P2865_PROMOTION_P169689558
	        		key = promo + "_" + recordType + "_" + f3;  
//	            	markLogUtil.logmarkGeneral("key:" + key);	            	
	        		hash_newPromo.put(key, inputLine);
	            }else if(recordType.equalsIgnoreCase("PROMOTION_CONDITION")){
	            	// key:11_0228T:P2865_PROMOTION_CONDITION_P169689558
	        		key = promo + "_" + recordType + "_" + f3;  
//	            	markLogUtil.logmarkGeneral("key:" + key);	            	
	        		hash_newPromoCond.put(key, inputLine);            	
	            }else if(recordType.equalsIgnoreCase("PROMOTION_DISCOUNT")) {
	            	// key:11_0228T:P2865_PROMOTION_DISCOUNT_P169689558
	        		key = promo + "_" + recordType + "_" + f3;  
//	            	markLogUtil.logmarkGeneral("key:" + key);	            	
	        		hash_newPromoDisc.put(key, inputLine);            	
	            }else if(recordType.equalsIgnoreCase("PROMOTION_DATE")) {
	            	// key:11_0228T:P2865_PROMOTION_DATE_P169689558
	        		key = promo + "_" + recordType + "_" + f3;  
//	            	markLogUtil.logmarkGeneral("key:" + key);	            	
	        		hash_newPromoDate.put(key, inputLine);            	
	            }else if(recordType.equalsIgnoreCase("ASSORTMENT")){
	            	// key:11_0228T:P2865_ASSORTMENT_AP169689558
	        		key = promo + "_" + recordType + "_" + f3;  
//	            	markLogUtil.logmarkGeneral("key:" + key);	            	
	        		hash_newAssortment.put(key, inputLine);
	            }else if(recordType.equalsIgnoreCase("ASSORTMENT_CONTENTS")){
	            	// key:11_0228T:P2865_ASSORTMENT_CONTENTS_/Assortments/Promotions/PromotionMaster_ASSORTMENT_/Assortments/Promotions/11_0
                    // key:11_0228T:P2865_ASSORTMENT_CONTENTS_/Assortments/Promotions/11_0228T/P2865/AP169689558_PRODUCT_124844	            		            	
	            	key = promo + "_" + recordType + "_" + f3 + "_" + f4 + "_" + st.nextToken();
//	            	markLogUtil.logmarkGeneral("key:" + key);	            	
	            	hash_newAssortmentContents.put(key, inputLine);
	            }else if(recordType.equalsIgnoreCase("OBJECT_ATTRIBUTE")) {
	            	//  key:11_0228T:P2865_OBJECT_ATTRIBUTE_ASSORTMENT_/Assortments/Promotions/11_0228T/P2865/AP169300678_Group_Pricing
                    //  key:11_0228T:P2865_OBJECT_ATTRIBUTE_PROMOTION_P169695444_Promo_Pricing_Type
	            	//  key:11_0228T:P2865_OBJECT_ATTRIBUTE_ASSORTMENT_/Assortments/Promotions/11_0228T/P2865/AP169288772_Group_Pricing
	        		key = promo + "_" + recordType + "_" + f3 + "_" + f4 + "|" + st.nextToken();
//	            	markLogUtil.logmarkGeneral("inputLine:" + inputLine);
//	            	markLogUtil.logmarkGeneral("key:" + key);	            	
	            	hash_newObjectAttribute.put(key, inputLine);
	            }else{
	            	// this actually currently happens - the current process just logs it -- 
	            	// this should really be emailed?  along with the counts
	            	markLogUtil.logmarkGeneral("unknown record type:" + inputLine,markLogUtil.LEVEL_CRITICAL);
	            }
			}catch(Exception e){
				e.printStackTrace();
			}            
		}     
	    markLogUtil.logmarkGeneral("NEW Promo Hash Size............:" + hash_newPromo.size(),markLogUtil.LEVEL_CRITICAL);    
	    markLogUtil.logmarkGeneral("NEW Promo Cond Hash Size.......:" + hash_newPromoCond.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("NEW Promo Disc Hash Size.......:" + hash_newPromoDisc.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("NEW Promo Date Hash Size.......:" + hash_newPromoDate.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("NEW Assortment Hash Size.......:" + hash_newAssortment.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("NEW Assortment Cont Hash Size..:" + hash_newAssortmentContents.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("NEW Object Attribute Hash Size.:" + hash_newObjectAttribute.size(),markLogUtil.LEVEL_CRITICAL);				
	}

	/**
	 * 
	 * buildOldPromoHash
	 * 
	 *  Get all the old and new data for the specified Promo ID
	 *  getdata creates a hash (OLD or NEW) of hashes for each record type 
	 *  ('PROMOTION', 'ASSORTMENT', etc.) 
     * 	getdata("$olddir/$pid","OLD"); 
     *
     * I|ASSORTMENT|AP169887581|/Assortments/Promotions/11_0309T/P2872|A|GRAND OPENING / P169887581|Percent Off: 40.000 SCL_484047||2011-03-09 00:00:00|2011-03-20 23:59:59|||
     * I|PROMOTION|P171003923|/Promotions/11_0309T/P2872|U|GRAND OPENING / P171003923|Price Point: 13.990 SKU_91242793
     * I|PROMOTION_CONDITION|P171003923|P|EQ|/PriceLists/USA_Standard|||743188||1||||PRODUCT
     * I|PROMOTION_CONDITION|P169163094|P|EQ|/PriceLists/USA_Standard|||512071||1||||PRODUCT
     * I|PROMOTION_CONDITION|P169164896|P|EQ|/PriceLists/USA_Standard|||AP169164896||1||||ASSORTMENT
     * I|PROMOTION_DISCOUNT|P171003923|SP|/PriceLists/USA_Standard||99999|||13.990|743188|99999|PA|PRODUCT
     * I|PROMOTION_DATE|P171003923|2011-03-09 00:00:00|2011-03-20 23:59:59
     * I|ASSORTMENT_CONTENTS|/Assortments/Promotions/PromotionMaster|ASSORTMENT|/Assortments/Promotions/11_0309T/P2872/AP170745823
     * S|OBJECT_ATTRIBUTE|PROMOTION|P170485656|Promo_Pricing_Type|SPO
     * S|OBJECT_ATTRIBUTE|PROMOTION|P169970103|Promo_Pricing_Type|SPP
     *
	 * @param promo
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void buildOldPromoHash(String promo) throws FileNotFoundException, IOException {
		
		BufferedReader inputReader = null;// for reading the promotion files created by ES_promo_parse.pl
		String inputLine;	              // input line for file read
		String key = null;                // key for the hash
        String actionType = null;         // record Action type - not used currently
        String recordType = null;         // RecordType - ie: promotion, assortment, etc
        String f3 = null;                 // varies based on recordtype
        String f4 = null;                 // varies based on recordtype
        StringTokenizer st = null;        // tokenizer to create unique key for hash  
        markLogUtil.logmarkGeneral("Working Directory:" + sOldDir + ", file:" + promo,markLogUtil.LEVEL_CRITICAL);// informational for log
        int recordsRead = 0;// used otherwise the log pauses for too long - display counts record
        inputReader = new BufferedReader(new FileReader(new File(sOldDir,promo)));
		while ((inputLine = inputReader.readLine()) != null) {			
			if(recordsRead % 50000 == 0) {// display current read count every 50,000 records
				markLogUtil.logmarkGeneral("Processed: " + recordsRead + " input records",markLogUtil.LEVEL_CRITICAL);
			}		
			recordsRead++;
			try	{
				st = new StringTokenizer(inputLine, "|");
				actionType = st.nextToken();    
				recordType = st.nextToken();
				f3 = st.nextToken();
				f4 = st.nextToken();
        		key = promo + "_" + f3;
            
	            if (recordType.equalsIgnoreCase("PROMOTION")) {    
	            	// key:11_0228T:P2865_PROMOTION_P169689558	            	
	        		key = promo + "_" + recordType + "_" + f3;  
	            	hash_oldPromo.put(key, inputLine);
	            }else if(recordType.equalsIgnoreCase("PROMOTION_CONDITION")){
	            	// key:11_0228T:P2865_PROMOTION_CONDITION_P169689558	            	
	        		key = promo + "_" + recordType + "_" + f3;  
	            	hash_oldPromoCond.put(key, inputLine);            	
	            }else if(recordType.equalsIgnoreCase("PROMOTION_DISCOUNT")) {
	            	// key:11_0228T:P2865_PROMOTION_DISCOUNT_P169689558	            	
	        		key = promo + "_" + recordType + "_" + f3;  
	            	hash_oldPromoDisc.put(key, inputLine);            	
	            }else if(recordType.equalsIgnoreCase("PROMOTION_DATE")) {
	            	// key:11_0228T:P2865_PROMOTION_DATE_P169689558	            	
	        		key = promo + "_" + recordType + "_" + f3;  
	            	hash_oldPromoDate.put(key, inputLine);            	
	            }else if(recordType.equalsIgnoreCase("ASSORTMENT")){
	            	// key:11_0228T:P2865_ASSORTMENT_AP169689558	            	
	        		key = promo + "_" + recordType + "_" + f3;  
	            	hash_oldAssortment.put(key, inputLine);
	            }else if(recordType.equalsIgnoreCase("ASSORTMENT_CONTENTS")){
	            	// key:11_0228T:P2865_ASSORTMENT_CONTENTS_/Assortments/Promotions/PromotionMaster_ASSORTMENT_/Assortments/Promotions/11_0
                    // key:11_0228T:P2865_ASSORTMENT_CONTENTS_/Assortments/Promotions/11_0228T/P2865/AP169689558_PRODUCT_124844	            		            	
	            	key = promo + "_" + recordType + "_" + f3 + "_" + f4 + "_" + st.nextToken();
	            	hash_oldAssortmentContents.put(key, inputLine);
	            }else if(recordType.equalsIgnoreCase("OBJECT_ATTRIBUTE")) {
	            	//  key:11_0228T:P2865_OBJECT_ATTRIBUTE_ASSORTMENT_/Assortments/Promotions/11_0228T/P2865/AP169300678_Group_Pricing
                    //  key:11_0228T:P2865_OBJECT_ATTRIBUTE_PROMOTION_P169695444_Promo_Pricing_Type
	            	//  key:11_0228T:P2865_OBJECT_ATTRIBUTE_ASSORTMENT_/Assortments/Promotions/11_0228T/P2865/AP169288772_Group_Pricing
	            	// promo=11_0228T:P2865  
	            	// recordType=OBJECT_ATTRIBUTE
	            	// f3=ASSORTMENT
	            	// f4=P169695444
	            	// st.nextToken=Promo_Pricing_Type
	        		key = promo + "_" + recordType + "_" + f3 + "_" + f4 + "|" + st.nextToken();
//	            	markLogUtil.logmarkGeneral("inputLine:" + inputLine);
//	            	markLogUtil.logmarkGeneral("key:" + key);
	            	hash_oldObjectAttribute.put(key, inputLine);
	            }else{
	            	markLogUtil.logmarkGeneral("unknown record type:" + inputLine,markLogUtil.LEVEL_CRITICAL);
	            }
			}catch(Exception e){
				e.printStackTrace();
			}
		}  			
	    markLogUtil.logmarkGeneral("OLD Promo Hash Size............:" + hash_oldPromo.size(),markLogUtil.LEVEL_CRITICAL);    
	    markLogUtil.logmarkGeneral("OLD Promo Cond Hash Size.......:" + hash_oldPromoCond.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("OLD Promo Disc Hash Size.......:" + hash_oldPromoDisc.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("OLD Promo Date Hash Size.......:" + hash_oldPromoDate.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("OLD Assortment Hash Size.......:" + hash_oldAssortment.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("OLD Assortment Cont Hash Size..:" + hash_oldAssortmentContents.size(),markLogUtil.LEVEL_CRITICAL);
	    markLogUtil.logmarkGeneral("OLD Object Attribute Hash Size.:" + hash_oldObjectAttribute.size(),markLogUtil.LEVEL_CRITICAL);				
	}	
}
