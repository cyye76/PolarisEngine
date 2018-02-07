package Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

public class TarArchiveFiles {
	private static final String BASE_DIR = "";
	private static final int BUFFER = 1024;  
    private static final String EXT = ".tar";
    
    public static String archive(String srcPath) throws Exception {  
        File srcFile = new File(srcPath);  
  
        return archive(srcFile);  
    }  
  
    public static String archive(File srcFile) throws Exception {  
        String name = srcFile.getName();  
        String basePath = srcFile.getParent(); 
        if(basePath==null) basePath = ".";
        String destPath = basePath + "/" + name + EXT;  
        archive(srcFile, destPath);  
        return destPath;
    }  
    
    public static void archive(String srcPath, String destPath)  
            throws Exception {  
  
        File srcFile = new File(srcPath);  
  
        archive(srcFile, destPath);  
  
    }  
    
    public static void archive(File srcFile, String destPath) throws Exception {  
        archive(srcFile, new File(destPath));  
    }  
    
    public static void archive(File srcFile, File destFile) throws Exception {  
    	  
        TarArchiveOutputStream taos = new TarArchiveOutputStream(  
                new FileOutputStream(destFile));  
  
        archive(srcFile, taos, BASE_DIR);  
  
        taos.flush();  
        taos.close();  
    }  
    
	private static void archive(File file, TarArchiveOutputStream taos, String dir) throws Exception {  
		TarArchiveEntry entry = new TarArchiveEntry(dir + file.getName());  
		  
        entry.setSize(file.length());  
  
        taos.putArchiveEntry(entry);  
  
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));  
        int count;  
        byte data[] = new byte[BUFFER];  
        while ((count = bis.read(data, 0, BUFFER)) != -1) {  
            taos.write(data, 0, count);  
        }  
  
        bis.close();  
  
        taos.closeArchiveEntry();  
	}
	
	public static void dearchive(File srcFile) throws Exception {  
        String basePath = srcFile.getParent(); 
        if(basePath==null) basePath=".";
        dearchive(srcFile, basePath);  
    }  
	
	public static void dearchive(File srcFile, File destFile) throws Exception {  
		  
        TarArchiveInputStream tais = new TarArchiveInputStream(  
                new FileInputStream(srcFile));  
        dearchive(destFile, tais);  
  
        tais.close();  
  
    }  
	
	public static void dearchive(File srcFile, String destPath)  
            throws Exception {  
        dearchive(srcFile, new File(destPath));  
  
    }  
	
	private static void dearchive(File destFile, TarArchiveInputStream tais)  
            throws Exception {  
  
        TarArchiveEntry entry = null;  
        while ((entry = tais.getNextTarEntry()) != null) {  
  
            // 文件  
            String dir = destFile.getPath() + File.separator + entry.getName();  
  
            File dirFile = new File(dir);  
  
            // 文件检查  
            fileProber(dirFile);  
  
            if (entry.isDirectory()) {  
                dirFile.mkdirs();  
            } else {  
                dearchiveFile(dirFile, tais);  
            }  
  
        }  
    }  
	
	public static void dearchive(String srcPath) throws Exception {  
        File srcFile = new File(srcPath);  
  
        dearchive(srcFile);  
    }  
	
	private static void dearchiveFile(File destFile, TarArchiveInputStream tais)  
            throws Exception {  
  
        BufferedOutputStream bos = new BufferedOutputStream(  
                new FileOutputStream(destFile));  
  
        int count;  
        byte data[] = new byte[BUFFER];  
        while ((count = tais.read(data, 0, BUFFER)) != -1) {  
            bos.write(data, 0, count);  
        }  
  
        bos.close();  
    }  

	private static void fileProber(File dirFile) {  
		  
        File parentFile = dirFile.getParentFile();  
        if (!parentFile.exists()) {  
  
            // 递归寻找上级目录  
            fileProber(parentFile);  
  
            parentFile.mkdir();  
        }  
  
    }  
	
	public static File compress(String sourcename) {
		  File source = new File(sourcename);
		  File target = new File(sourcename + ".gz");
		  FileInputStream in = null;
		  GZIPOutputStream out = null;
		  
		  try {
		       in = new FileInputStream(source);
		       out = new GZIPOutputStream(new FileOutputStream(target));
		       byte[] array = new byte[1024];
		       int number = -1;
		       while((number = in.read(array, 0, array.length)) != -1) {
		            out.write(array, 0, number);
		       }
		  } catch (FileNotFoundException e) {
		       e.printStackTrace();
		       return null;
		  } catch (IOException e) {
		       e.printStackTrace();
		       return null;
		  } finally {
		       if(in != null) {
		            try {
		                 in.close();
		            } catch (IOException e) {
		                 e.printStackTrace();
		                 return null;
		            }
		       }
		   
		       if(out != null) {
		           try {
		                out.close();
		           } catch (IOException e) {
		                e.printStackTrace();
		                return null;
		           }
		       }
		  }
		  
		  return target;
	}
	
	public static File uncompress(String sourcename) {
		  String filename = sourcename.substring(0, sourcename.length()-3);
		  File target = new File(filename);
		  FileOutputStream out = null;
		  GZIPInputStream in = null;
		  
		  try {
		       in = new GZIPInputStream(new FileInputStream(sourcename));
		       out = new FileOutputStream(target);
		       byte[] array = new byte[1024];
		       int number = -1;
		       while((number = in.read(array, 0, array.length)) != -1) {
		            out.write(array, 0, number);
		       }
		  } catch (FileNotFoundException e) {
		       e.printStackTrace();
		       return null;
		  } catch (IOException e) {
		       e.printStackTrace();
		       return null;
		  } finally {
		       if(in != null) {
		            try {
		                 in.close();
		            } catch (IOException e) {
		                 e.printStackTrace();
		                 return null;
		            }
		       }
		   
		       if(out != null) {
		           try {
		                out.close();
		           } catch (IOException e) {
		                e.printStackTrace();
		                return null;
		           }
		       }
		  }
		  
		  return target;
	}
}
