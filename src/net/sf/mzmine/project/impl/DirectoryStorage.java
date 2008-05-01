package net.sf.mzmine.project.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sf.mzmine.project.ProjectType;

import com.thoughtworks.xstream.XStream;

public class DirectoryStorage {
	private File outDir;
	private String indexFile = "index.xml";
	private String suffix;
	private XStream xstream;
	private ProjectType type;

	public DirectoryStorage(XStream xstream, File outDir) {
		this(xstream, outDir, ProjectType.xml);
	}

	public DirectoryStorage(XStream xstream, File outDir, ProjectType type) {
		this.type = type;
		this.outDir = outDir;
		this.xstream = xstream;
		
		if (type == ProjectType.zippedXML) {
			this.suffix = ".xml.zip";
		} else {
			this.suffix = "xml";
		}
		if (!outDir.exists()){
			outDir.mkdir();
		}
	}

	private String getFileName(String name) {
		return name + "." + suffix;
	}

	private String getItemName(String fileName) {
		return fileName.substring(0, fileName.length() - suffix.length() - 1);
	}

	private OutputStream getOutputStream(File outFile) throws IOException,
			FileNotFoundException {
		OutputStream os;
		OutputStream fos = new FileOutputStream(outFile);
		if (this.type == ProjectType.zippedXML) {
			ZipOutputStream zos = new ZipOutputStream(fos);
			zos.putNextEntry(new ZipEntry(this.getItemName(outFile.getName())));
			os = zos;
		} else {
			os = fos;
		}
		return os;
	}

	private InputStream getInputStream(File file) throws IOException,
			FileNotFoundException {
		InputStream is;
		InputStream fis = new FileInputStream(file);
		if (this.type == ProjectType.zippedXML) {
			ZipInputStream zis = new ZipInputStream(fis);
			zis.getNextEntry();
			is = zis;
		} else {
			is = fis;
		}
		return is;
	}

	public ArrayList<String> readIndex() throws IOException {
		File inFile = new File(this.outDir, this.indexFile);
		ArrayList<String> items;
		if (!inFile.exists()) {
			inFile.createNewFile();
			items = new ArrayList<String>(0);
		} else {
			items = (ArrayList<String>) xstream.fromXML(new FileInputStream(
					inFile));
		}
		return items;
	}

	private void addIndex(String itemName) throws IOException {
		ArrayList<String> items = readIndex();
		items.add(itemName);
		File outFile = new File(this.outDir, this.indexFile);
		if (outFile.exists()) {
			outFile.delete();
		}
		outFile.createNewFile();
		xstream.toXML(items, new FileOutputStream(outFile));
	}

	public void add(Object obj, String itemName) throws IOException {
		File outFile = new File(this.outDir, getFileName(itemName));
		outFile.createNewFile();
		xstream.toXML(obj, getOutputStream(outFile));
		addIndex(itemName);
	}

	public Object get(int index) throws IOException {
		ArrayList<String> items = readIndex();
		if (items.size() < index) {
			throw new FileNotFoundException();
		}
		String itemName = items.get(index);
		String fileName = getFileName(itemName);
		File objFile = new File(this.outDir, fileName);
		return xstream.fromXML(this.getInputStream(objFile));
	}
}
