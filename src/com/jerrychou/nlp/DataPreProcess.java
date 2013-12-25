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
	/**输入文件调用处理数据函数
	 * @param strDir newsgroup文件目录的绝对路径
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
			if(!new File(fileFullName).isDirectory()){//确认子文件名不是目录如果是可以再次递归调用
				System.out.println("Begin preprocess:"+fileFullName);
				createProcessFile(fileFullName, target);
				stemFileNames[j] = target;
				j++;
			}
			else {
				doProcess(fileFullName,target);
			}
		}
		//下面调用stem算法
		if(stemFileNames.length > 0 && stemFileNames[0] != null){
			System.out.println("\nStemming...");
			Stemmer.porterMain(stemFileNames);
		}
	}
	/**   
     * 删除单个文件   
     * @param   fileName    被删除文件的文件名   
     * @return 单个文件删除成功返回true,否则返回false   
     */    
    public static boolean deleteFile(String fileName){     
        File file = new File(fileName);     
        if(file.isFile() && file.exists()){     
            file.delete();     
            System.out.println("删除单个文件"+fileName+"成功！");     
            return true;     
        }else{     
            System.out.println("删除单个文件"+fileName+"失败！");     
            return false;     
        }     
    } 
	/**进行文本预处理生成目标文件
	 * @param srcDir 源文件文件目录的绝对路径
	 * @param targetDir 生成的目标文件的绝对路径
	 * @throws IOException 
	 */
	private static void createProcessFile(String srcDir, String targetDir) throws IOException {
		// TODO Auto-generated method stub
		FileReader srcFileReader = new FileReader(srcDir);
		FileWriter targetFileWriter = new FileWriter(targetDir);	
		BufferedReader srcFileBR = new BufferedReader(srcFileReader);//装饰模式
		String line, resLine;
		while((line = srcFileBR.readLine()) != null){
			resLine = lineProcess(line);
			if(!resLine.isEmpty()){
				//按行写，一行写一个单词
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
	/**对每行字符串进行处理，主要是词法分析、去停用词和stemming
	 * @param line 待处理的一行字符串
	 * @param ArrayList<String> 停用词数组
	 * @return String 处理好的一行字符串，是由处理好的单词重新生成，以空格为分隔符
	 * @throws IOException 
	 */
	private static String lineProcess(String line) throws IOException {
		// TODO Auto-generated method stub
		//step1 英文词法分析，去除数字、连字符、标点符号、特殊字符，所有大写字母转换成小写，可以考虑用正则表达式
		String res[] = line.split("[^a-zA-Z]");
		String resString = new String();
		//step2去停用词
		//step3stemming,返回后一起做
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
