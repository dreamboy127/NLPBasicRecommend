package com.jerrychou.nlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;

import com.jerrychou.util.Printer;
import com.jerrychou.util.Sort;

import Jama.*; 

public class LSIModel {
	static final double threshlod=0.99;
	public SingularValueDecomposition s;
	public Matrix U;
	public Matrix S;
	public Matrix V;
	public Matrix VVT;
	Map<String, Integer> termToindexMap;
	Map<Integer,Double> idf;
	//ArrayList<Document> docs;
	//Integer docSize;
	//Map<String, Integer> termToindexMap;
	//ArrayList<String> indexTotermMap;
	//Map<Integer,Double> idf;
	//Integer termSize;
	public String CorpusDir;
	public Corpus corpus;
	public LSIModel(){
		CorpusDir=new String();
		corpus=new Corpus();
	}
	public void Initial(String orginDir) throws Exception{
		String preprocessDir=null;
		Initial(orginDir,preprocessDir);
	}
	public void Initial(String orginDir,String preprocessDir) throws Exception{
		if(preprocessDir!=null){
			DataPreProcess doPreProcess=new DataPreProcess();
			doPreProcess.doProcess(orginDir, preprocessDir);
			this.CorpusDir=preprocessDir;
		}
		else{
			this.CorpusDir=orginDir;
		}
		corpus.readDocs(this.CorpusDir);
	}
	public void train(String saveDir) throws Exception{
		File saveDirFile=new File(saveDir); 
		if(!saveDirFile.exists())
			saveDirFile.mkdir();
		long learnStart = System.currentTimeMillis();
		corpus.computeTFIDF();
		saveSerializedFile(corpus,saveDir+"\\Corpus.obj");
		double [][] tfidfmatrix = new double[corpus.getDocSize()][corpus.getTermSize()]; 
		//corpus.printTFIDF(saveDir+"\\TFIDF\\",tfidfmatrix);
		corpus.printTFIDF(tfidfmatrix);
		
		Matrix tfidfMatrix=new Matrix(tfidfmatrix);
		System.out.println("tfidfMatrix = U S V^T\nStart Decomposition\nPlease Wait...");
	    s = tfidfMatrix.transpose().svd();
	    
	    S = s.getS();
	    System.out.println("Sigma = "+S.getRowDimension()+"*"+S.getColumnDimension());
	    double orgintrace=S.trace();
		double threshlodTrace=threshlod*orgintrace;
		int reduceDimension;
		for(reduceDimension=S.getRowDimension()-1;S.getMatrix(0, reduceDimension, 0, reduceDimension).trace()>=threshlodTrace;reduceDimension--);
		reduceDimension++;
	    S=S.getMatrix(0, reduceDimension, 0, reduceDimension);
	    saveSerializedFile(S,saveDir+"\\S.obj");
	    System.out.println("After reduce dimension...\nSigma = "+S.getRowDimension()+"*"+S.getColumnDimension());
	    
	    U = s.getU();
	    System.out.println("U = "+U.getRowDimension()+"*"+U.getColumnDimension());
	    U=U.getMatrix(0, U.getRowDimension()-1, 0, reduceDimension);
	    saveSerializedFile(U,saveDir+"\\U.obj");
	    System.out.println("After reduce dimension...\nU = "+U.getRowDimension()+"*"+U.getColumnDimension());
	    
	    V = s.getV();
	    System.out.println("V = "+V.getRowDimension()+"*"+V.getColumnDimension());
	    V=V.getMatrix(0, V.getRowDimension()-1, 0, reduceDimension);
	    saveSerializedFile(V,saveDir+"\\V.obj");
	    System.out.println("After reduce dimension...\nV = "+V.getRowDimension()+"*"+V.getColumnDimension());

	    System.out.println("rank = " + s.rank());
	    System.out.println("condition number = " + s.cond());
	    System.out.println("2-norm = " + s.norm2());
	    long learnEnd = System.currentTimeMillis();
		System.out.println("train takes time:"+Printer.printTime(learnEnd - learnStart));
		
		termToindexMap=corpus.getTermToIndex();
		idf=corpus.getIdf();
		VVT=V.times(V.transpose());
	}
	public void load(String loadDir) throws Exception{
		corpus=(Corpus)readSerializedFile(loadDir+"\\Corpus.obj");
		S=(Matrix)readSerializedFile(loadDir+"\\S.obj");
		U=(Matrix)readSerializedFile(loadDir+"\\U.obj");
		V=(Matrix)readSerializedFile(loadDir+"\\V.obj");
		
		termToindexMap=corpus.getTermToIndex();
		idf=corpus.getIdf();
		VVT=V.times(V.transpose());
	}
	public void QueryRun(String queryDir) throws Exception{
		long queryStart = System.currentTimeMillis();
		Corpus Query = new Corpus();
	    Query.readDocs(queryDir);
	    
	    double [][] QuerytfidfMatrix = new double[Query.getDocSize()][Query.getTermSize()]; 

	    Query.printTF(QuerytfidfMatrix);
	    double [][] QueryMatrix = new double[Query.getDocSize()][U.getRowDimension()]; 
	    for(int j=0;j<Query.getDocSize();j++){
	    	for(int i=0;i<Query.getTermSize();i++){
	    		if(termToindexMap.containsKey(Query.getIndexToTerm().get(i)))
	    		{
	    			int termindex=termToindexMap.get(Query.getIndexToTerm().get(i));
	    			QueryMatrix[j][termindex]=QuerytfidfMatrix[j][i]/Query.getTermSize()*idf.get(termindex);
	    		}
	    	}
	    }
		Matrix QueryTfIdfMatrix= new Matrix(QueryMatrix);
		System.out.println("QueryTfIdfMatrix = "+QueryTfIdfMatrix.getRowDimension()+"*"+QueryTfIdfMatrix.getColumnDimension());
		//QueryTfIdfMatrix.print(9, 6);
		Matrix QueryTopicMatrix= QueryTfIdfMatrix.times(U.times(S.inverse()));
		System.out.println("QueryTopicMatrix = "+QueryTopicMatrix.getRowDimension()+"*"+QueryTopicMatrix.getColumnDimension());
		//QueryTopicMatrix.print(9, 6);
		Matrix length=new Matrix(Query.getDocSize(),V.getRowDimension());
		Matrix QueryTopicMatrixQueryTopicMatrixT=new Matrix(QueryTopicMatrix.times(QueryTopicMatrix.transpose()).getArray());
		for(int i=0;i<Query.getDocSize();i++)
			for(int j=0;j<V.getRowDimension();j++)
				length.set(i, j, Math.sqrt(VVT.get(j, j))*Math.sqrt(QueryTopicMatrixQueryTopicMatrixT.get(i, i)));
		Matrix sim = QueryTopicMatrix.times(V.transpose()).arrayRightDivideEquals(length);
		System.out.println("similarities = "+sim.getRowDimension()+"*"+sim.getColumnDimension());
		//sim.print(3, 7);
		
		double [][] ResultMatrix=sim.getArrayCopy();
		for(int i=0;i<ResultMatrix.length;i++)
		{
			/*int maxIndex=0;
			double maxSim=0.0;
			for(int j=0;j<ResultMatrix[i].length;j++)
			{
				if(maxSim<ResultMatrix[i][j]){
					maxSim=ResultMatrix[i][j];
					maxIndex=j;
				}
			}*/
			int[] index=new int[ResultMatrix[i].length];
			for(int j=0;j<index.length;j++)
				index[j]=j;
			Sort.quickSort(ResultMatrix[i], index, 0, ResultMatrix[i].length-1, false);
			for(int j=0;j<10;j++)
				System.out.println(corpus.getDocs().get(index[j]).getDocName()+":"+ResultMatrix[i][j]);
			System.out.println("\n");
		}
		long queryEnd = System.currentTimeMillis();
		System.out.println("query takes time:"+Printer.printTime(queryEnd - queryStart));
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
