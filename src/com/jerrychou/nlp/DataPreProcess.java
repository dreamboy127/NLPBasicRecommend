package com.jerrychou.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DataPreProcess {
	/**�����ļ����ô������ݺ���
	 * @param strDir newsgroup�ļ�Ŀ¼�ľ���·��
	 * @throws IOException 
	 */
	public void doProcess(String strDir,String targetDir) throws IOException{
		strDir = strDir.replace("\\","/");
		File fileDir = new File(strDir);
		if(!fileDir.exists()){
			System.out.println("File not exist:" + strDir);
			return;
		}
		File targetDirFile = new File(targetDir);
		if(!targetDirFile.exists()){
			targetDirFile.mkdir();
		}
		File[] srcFiles = fileDir.listFiles();
		String[] stemFileNames = new String[srcFiles.length];
		int j=0;
		for(int i = 0; i < srcFiles.length; i++){
			String fileFullName = srcFiles[i].getCanonicalPath();
			String fileShortName = srcFiles[i].getName();
			String target=targetDir + "/"  + fileShortName;
			if(!new File(fileFullName).isDirectory()){//ȷ�����ļ�������Ŀ¼����ǿ����ٴεݹ����
				System.out.println("Begin preprocess:"+fileFullName);
				createProcessFile(fileFullName, target);
				stemFileNames[j] = target;
				j++;
			}
			else {
				doProcess(fileFullName,target);
			}
		}
		//�������stem�㷨
		if(stemFileNames.length > 0 && stemFileNames[0] != null){
			System.out.println("\nStemming...");
			Stemmer.porterMain(stemFileNames);
		}
	}
	/**   
     * ɾ�������ļ�   
     * @param   fileName    ��ɾ���ļ����ļ���   
     * @return �����ļ�ɾ���ɹ�����true,���򷵻�false   
     */    
    public static boolean deleteFile(String fileName){     
        File file = new File(fileName);     
        if(file.isFile() && file.exists()){     
            file.delete();     
            System.out.println("ɾ�������ļ�"+fileName+"�ɹ���");     
            return true;     
        }else{     
            System.out.println("ɾ�������ļ�"+fileName+"ʧ�ܣ�");     
            return false;     
        }     
    } 
	/**�����ı�Ԥ��������Ŀ���ļ�
	 * @param srcDir Դ�ļ��ļ�Ŀ¼�ľ���·��
	 * @param targetDir ���ɵ�Ŀ���ļ��ľ���·��
	 * @throws IOException 
	 */
	private static void createProcessFile(String srcDir, String targetDir) throws IOException {
		// TODO Auto-generated method stub
		FileReader srcFileReader = new FileReader(srcDir);
		FileWriter targetFileWriter = new FileWriter(targetDir);	
		BufferedReader srcFileBR = new BufferedReader(srcFileReader);//װ��ģʽ
		String line, resLine;
		while((line = srcFileBR.readLine()) != null){
			resLine = lineProcess(line);
			if(!resLine.isEmpty()){
				//����д��һ��дһ������
				String[] tempStr = resLine.split(" ");//\s
				for(int i = 0; i < tempStr.length; i++){
					if(!tempStr[i].isEmpty()){
						targetFileWriter.append(tempStr[i]+"\n");
					}
				}
			}
		}
		targetFileWriter.flush();
		targetFileWriter.close();
		srcFileReader.close();
		srcFileBR.close();
	}
	/**��ÿ���ַ������д�����Ҫ�Ǵʷ�������ȥͣ�ôʺ�stemming
	 * @param line �������һ���ַ���
	 * @param ArrayList<String> ͣ�ô�����
	 * @return String ����õ�һ���ַ��������ɴ���õĵ����������ɣ��Կո�Ϊ�ָ���
	 * @throws IOException 
	 */
	private static String lineProcess(String line) throws IOException {
		// TODO Auto-generated method stub
		//step1 Ӣ�Ĵʷ�������ȥ�����֡����ַ��������š������ַ������д�д��ĸת����Сд�����Կ�����������ʽ
		String res[] = line.split("[^a-zA-Z]");
		String resString = new String();
		//step2ȥͣ�ô�
		//step3stemming,���غ�һ����
		for(int i = 0; i < res.length; i++){
			if(!res[i].isEmpty() && !Stopwords.isStopword(res[i].toLowerCase())/*&& !isNoiseWord(res[i].toLowerCase())*/){
				resString += " " + res[i].toLowerCase() + " ";
			}
		}
		return resString;
	}
	public static boolean isNoiseWord(String string) {
		// TODO Auto-generated method stub
		string = string.toLowerCase().trim();
		Pattern MY_PATTERN = Pattern.compile(".*[a-zA-Z]+.*");
		Matcher m = MY_PATTERN.matcher(string);
		// filter @xxx and URL
		if(string.matches(".*www\\..*") || string.matches(".*\\.com.*") || 
				string.matches(".*http:.*") )
			return true;
		if (!m.matches()) {
			return true;
		} else
			return false;
	}
}
