/*
 * Copyright (c) 2008-2009, Intel Corporation.
 * Copyright (c) 2006-2007, The Trustees of Stanford University.
 * All rights reserved.
 */
package chord.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;

/**
 * File related utilities.
 *  
 * @author Mayur Naik (mhn@cs.stanford.edu)
 */
public class FileUtils {
	public static String getAbsolutePath(String fileName, String baseName) {
		File file = new File(fileName);
		if (file.isAbsolute())
			return fileName;
		file = new File(baseName, fileName);
		return file.getAbsolutePath();
	}
	public static void copy(String fromFileName, String toFileName) {
		try {
			FileInputStream fis = new FileInputStream(fromFileName);
			FileOutputStream fos = new FileOutputStream(toFileName);
			byte[] buf = new byte[1024];
			int i = 0;
			while((i = fis.read(buf)) != -1) {
				fos.write(buf, 0, i);
			}
			fis.close();
			fos.close();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	public static boolean mkdir(String dirName) {
		return mkdir(new File(dirName));
	}
	public static boolean mkdir(String parentName, String childName) {
		return mkdir(new File(parentName, childName));
	}
	public static boolean mkdir(File file) {
		if (file.exists()) {
			if (!file.isDirectory()) {
				throw new RuntimeException(
					"File '" + file + "' is not a directory.");
			}
			return false;
		}
		if (file.mkdirs())
			return true;
		throw new RuntimeException("Failed to create directory '" +
			file + "'"); 
	}
	public static PrintWriter newPrintWriter(String fileName) {
		PrintWriter out;
		try {
			out = new PrintWriter(fileName);
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		}
		return out;
	}
    public static Object readSerialFile(String serialFileName) {
		try {
			FileInputStream fs = new FileInputStream(serialFileName);
			ObjectInputStream os = new ObjectInputStream(fs);
			Object o = os.readObject();
			os.close();
			return o;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
    }
    public static void writeSerialFile(Object o, String serialFileName) {
		try {
			FileOutputStream fs = new FileOutputStream(serialFileName);
			ObjectOutputStream os = new ObjectOutputStream(fs);
			os.writeObject(o);
			os.close();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public static List<String> readFileToList(String fileName) {
		return readFileToList(new File(fileName));
	}
	public static List<String> readFileToList(File file) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            List<String> list = new ArrayList<String>();
            String s;
            while ((s = in.readLine()) != null) {
                list.add(s);
            }
            in.close();
            return list;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
	public static IndexMap<String> readFileToMap(String fileName) {
		return readFileToMap(new File(fileName));
	}
	public static IndexMap<String> readFileToMap(File file) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            IndexMap<String> map = new IndexMap<String>();
            String s;
            while ((s = in.readLine()) != null) {
                map.set(s);
            }
            in.close();
            return map;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
	public static void writeListToFile(List<String> list, String fileName) {
		try {
            PrintWriter out = new PrintWriter(fileName);
            for (String s : list) {
                out.println(s);
            }
            out.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
	public static void writeMapToFile(IndexMap<String> map, String fileName) {
		try {
            PrintWriter out = new PrintWriter(fileName);
            for (String s : map) {
                out.println(s);
            }
            out.close();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
