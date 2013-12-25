package com.jerrychou.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class Corpus implements Serializable{
	private static final long serialVersionUID = 8351257560378572493L;
	private ArrayList<Document> docs;
	private Map<String, Integer> termToIndexMap;
	private ArrayList<String> indexToTermArray;
	private Map<Integer,Integer> indexToCountMap;//word count
	private Map<Integer,Set<Integer>> termInDocsMap;//term doc set
	private Map<Integer,Double> idf;//idf
	private String docsPath;
	private boolean docsPathflag=false;
	
	public Corpus(){
		docs = new ArrayList<Document>();
		termToIndexMap = new HashMap<String, Integer>();
		indexToTermArray = new ArrayList<String>();
		indexToCountMap = new HashMap<Integer,Integer>();
		termInDocsMap = new HashMap<Integer,Set<Integer>>();
		idf = new HashMap<Integer,Double>();
	}
	public Map<String, Integer> getTermToIndex(){
		return termToIndexMap;
	}
	public ArrayList<String> getIndexToTerm(){
		return indexToTermArray;
	}
	public int getTermSize(){
		return indexToTermArray.size();
	}
	public int getDocSize(){
		return docs.size();
	}
	public ArrayList<Document> getDocs(){
		return docs;
	}
	public Map<Integer,Double> getIdf(){
		return idf;
	}
	/*@param flag:true 已经预处理，否则会执行预处理*/
	public void readDocs(String docsPath) throws Exception{
		if(docsPathflag==false)
		{
			this.docsPath = new String(docsPath);
			docsPathflag=true;
		}
			
		for(File docFile : new File(docsPath).listFiles()){
			if(docFile.isDirectory()){
				readDocs(docFile.getCanonicalPath());
			}
			else
			{
				Integer index=docs.size();
				Document doc = new Document(docFile.getAbsolutePath(), index,termToIndexMap, indexToTermArray, indexToCountMap,termInDocsMap);
				docs.add(doc);
			}	
		}
		System.out.println("total file number:"+docs.size());
		//idf计算
		Double idfValue;
		Double coutDoc;
		Set<Map.Entry<Integer, Set<Integer>>> tempDF = termInDocsMap.entrySet();
		for(Iterator<Map.Entry<Integer, Set<Integer>>> mt = tempDF.iterator(); mt.hasNext();){
			Map.Entry<Integer, Set<Integer>> me = mt.next();
			coutDoc=(double)me.getValue().size();
			idfValue =  Math.log(docs.size() / coutDoc) / Math.log(10);
			idf.put(me.getKey(), idfValue);
		}	
	}
	public void computeTFIDF() throws Exception{
		int i=0;
		for(Document doc:docs){
			doc.computeTFIDF(idf);
			i++;
			System.out.println("get the tfidf of the "+i+"th file ...");
		}
	}
	public void printTFIDF(double[][] tfidfMatrix) throws Exception{ 
		int i=0;
		for(Document doc:docs){
			doc.printTFIDF(tfidfMatrix[i]);
			i++;
		}
	}
	public void printTFIDF(String outputdir) throws Exception{
		double[][] tfidfMatrix=null;
		printTFIDF(outputdir,tfidfMatrix);
	}
	public void printTFIDF(String outputdir,double[][] tfidfMatrix) throws Exception{
		File outPutdir=new File(outputdir);
		if(!outPutdir.exists())
			outPutdir.mkdir();
		//double [][] tfidfMatrix = new double[docs.size()][indexToTermArray.size()]; 
		int i=0;
		for(Document doc:docs){
			if(tfidfMatrix==null)
				doc.printTFIDF(outputdir,null);
			else
				doc.printTFIDF(outputdir,tfidfMatrix[i]);
			i++;
		}
	}
	public void printTF(String outputdir,double[][] tfMatrix) throws Exception{
		File outPutdir=new File(outputdir);
		if(!outPutdir.exists())
			outPutdir.mkdir();
		//double [][] tfMatrix = new double[docs.size()][indexToTermArray.size()]; 
		int i=0;
		for(Document doc:docs){
			if(tfMatrix==null)
				doc.printTF(outputdir,null);
			else
				doc.printTF(outputdir,tfMatrix[i]);
			i++;
		}
	}
	public void printTF(String outputdir) throws Exception{
		double[][] tfMatrix=null;
		printTF(outputdir,tfMatrix);
	}
	public void printTF(double[][] tfMatrix) throws Exception{
		int i=0;
		for(Document doc:docs){
			doc.printTF(tfMatrix[i]);
			i++;
		}
	}
	public void printIDF(String outputfile) throws Exception{
		File outPutFile=new File(outputfile);
		
		FileWriter outPutFileWriter = new FileWriter(outPutFile);
		Set<Map.Entry<Integer, Double>> tempTF = idf.entrySet();
		for(Iterator<Map.Entry<Integer, Double>> mt = tempTF.iterator(); mt.hasNext();){
			Map.Entry<Integer, Double> me = mt.next();
			outPutFileWriter.write(indexToTermArray.get(me.getKey())+" "+me.getValue()+"\n");
			System.out.println(indexToTermArray.get(me.getKey()) + ": " + me.getValue());
		}
		outPutFileWriter.close();
	}
	public class Document implements Serializable{	
		private static final long serialVersionUID = 8313257562328762093L;
		private String docName=null;
		private Double termcount=null;
		private Double MaxCount=null;
		private Map<Integer,Integer> indexToCountInDoc=null;//tf
		private Map<Integer,Double> tfidf=null;//tfidf
		
		public Document(String docName, Integer docIndex,Map<String, Integer> termToIndexMap, ArrayList<String> indexToTermMap, 
				Map<Integer,Integer> indexToCountMap,	Map<Integer,Set<Integer>> termInDocsMap) throws Exception{
			this.docName = docName;
			this.termcount=0.0;
			this.MaxCount=1.0;
			indexToCountInDoc=new HashMap<Integer,Integer>();
			
			//Read file and initialize word index array
			FileReader samReader = new FileReader(docName);
			BufferedReader samBR = new BufferedReader(samReader);
			String word;
			while((word = samBR.readLine()) != null){
				if(!word.isEmpty()){
					termcount++;
					if( termToIndexMap.containsKey(word)){
						Integer wordIndex=termToIndexMap.get(word);
						Integer count = indexToCountMap.get(wordIndex) + 1;
						if(!indexToCountInDoc.containsKey(wordIndex))
						{
							indexToCountInDoc.put(wordIndex,0);
						}
						Integer countInDoc = indexToCountInDoc.get(wordIndex) + 1;
						if(MaxCount<countInDoc)
							MaxCount=(double)countInDoc;
						termInDocsMap.get(wordIndex).add(docIndex);
						indexToCountInDoc.put(wordIndex,countInDoc);
						indexToCountMap.put(wordIndex, count);
					}
					else {
						termToIndexMap.put(word, termToIndexMap.size());
						indexToTermMap.add(word);
						
						HashSet<Integer> docset=new HashSet<Integer>();
						docset.add(docIndex);
						termInDocsMap.put(indexToCountMap.size(),docset);
						indexToCountInDoc.put(indexToCountMap.size(), 1);
						indexToCountMap.put(indexToCountMap.size(),1);
					}
				}
				
			}
			samBR.close();
		}
		public String getDocName(){
			return docName;
		}
		public void computeTFIDF(Map<Integer,Double> idf){
			tfidf=new HashMap<Integer,Double>();
			
			Double wordWeight;
			Set<Map.Entry<Integer, Integer>> tempTF = indexToCountInDoc.entrySet();
			for(Iterator<Map.Entry<Integer, Integer>> mt = tempTF.iterator(); mt.hasNext();){
				Map.Entry<Integer, Integer> me = mt.next();
				wordWeight =  (me.getValue() / MaxCount) * idf.get(me.getKey());
				tfidf.put(me.getKey(), wordWeight);
			}
		}
		public void printTFIDF(double[] tfidfArray) throws IOException{
			if(tfidf==null){
				System.out.println("please compute the tfidf first by using getTFIDF func!");
				return;
			}
			
			Set<Map.Entry<Integer, Double>> tempTF = tfidf.entrySet();
			for(Iterator<Map.Entry<Integer, Double>> mt = tempTF.iterator(); mt.hasNext();){
				Map.Entry<Integer, Double> me = mt.next();
				if(tfidfArray!=null)
					tfidfArray[me.getKey()]=me.getValue();
			}
		}
		public void printTFIDF(String outputdir,double[] tfidfArray) throws IOException{
			if(tfidf==null){
				System.out.println("please compute the tfidf first by using getTFIDF func!");
				return;
			}
			String target=this.docName.replace(docsPath, outputdir);
			String targetdir=target.substring(0,target.lastIndexOf('\\'));
			
			File outPutFile = new File(target);
			File targetDir = new File(targetdir);
			if(!targetDir.exists())
				targetDir.mkdir();
			FileWriter outPutFileWriter = new FileWriter(outPutFile);
			Set<Map.Entry<Integer, Double>> tempTF = tfidf.entrySet();
			for(Iterator<Map.Entry<Integer, Double>> mt = tempTF.iterator(); mt.hasNext();){
				Map.Entry<Integer, Double> me = mt.next();
				outPutFileWriter.write(indexToTermArray.get(me.getKey())+" "+me.getValue()+"\n");
				if(tfidfArray!=null)
					tfidfArray[me.getKey()]=me.getValue();
				System.out.println(indexToTermArray.get(me.getKey()) + ": " + me.getValue());
			}
			outPutFileWriter.close();
		}
		public void printTF(double[] tfArray) throws IOException{
			if(indexToCountInDoc==null){
				System.out.println("please compute the tf first by using getTF func!");
				return;
			}
			Set<Map.Entry<Integer, Integer>> tempTF = indexToCountInDoc.entrySet();
			for(Iterator<Map.Entry<Integer, Integer>> mt = tempTF.iterator(); mt.hasNext();){
				Map.Entry<Integer, Integer> me = mt.next();
				if(tfArray!=null)
					tfArray[me.getKey()]=me.getValue();
				System.out.println(indexToTermArray.get(me.getKey()) + ": " + me.getValue());
			}
		}
		public void printTF(String outputdir,double[] tfArray) throws IOException{
			if(this.indexToCountInDoc==null){
				System.out.println("please compute the tf first by using getTF func!");
				return;
			}
			String target=this.docName.replace(docsPath, outputdir);
			String targetdir=target.substring(0,target.lastIndexOf('\\'));
			
			File outPutFile = new File(target);
			File targetDir = new File(targetdir);
			if(!targetDir.exists())
				targetDir.mkdir();
			FileWriter outPutFileWriter = new FileWriter(outPutFile);
			Set<Map.Entry<Integer, Integer>> tempTF = indexToCountInDoc.entrySet();
			for(Iterator<Map.Entry<Integer, Integer>> mt = tempTF.iterator(); mt.hasNext();){
				Map.Entry<Integer, Integer> me = mt.next();
				outPutFileWriter.write(indexToTermArray.get(me.getKey())+" "+me.getValue()+"\n");
				if(tfArray!=null)
					tfArray[me.getKey()]=me.getValue();
				System.out.println(indexToTermArray.get(me.getKey()) + ": " + me.getValue());
			}
			outPutFileWriter.close();
		}
	}
		

	public static void saveSerializedFile(Object obj, String fileName) {
		try {
			FileOutputStream f = new FileOutputStream(fileName); 
			ObjectOutput s = new ObjectOutputStream(f); 
			s.writeObject(obj);
			s.flush();
			f.close();
		}
		catch(IOException e) {
			System.out.println(e);
		}
	}
		
	public static Object readSerializedFile(String fileName) {
		try {
			FileInputStream f = new FileInputStream(fileName);
			ObjectInput s = new ObjectInputStream(f);
			Object obj = s.readObject();
				
			f.close();
				
			return obj;
		}
		catch(IOException e) {
			System.out.println("IO Exception occured: " + e);
			return null;
		}
		catch(ClassNotFoundException e) {
			System.out.println("Class not found: " + e);
			return null;
		}
	}
}
