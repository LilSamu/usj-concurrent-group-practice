package Storing;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.time.Instant;

import Main.Tuple;

public class OutputWriter
{
	private File file;
	private long offset = 0;
	private synchronized long getOffset(String data) {
		long off = offset;
		offset += data.length();
		return off;
	}
	
	public OutputWriter()
	{
		this.file = createFile();
	}
	
	// wrap call in a thread to not block the storer
	public void write(Tuple<URL, String> data)
	{
		System.out.println("Writing data from " + data.getFirst() + " to output");
		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "rw");
			String timestamp = Instant.now().toString();
			String content = "\n\n------------------------------------------------------------\n";
			content += "URL: " + data.getFirst() + " Time: " + timestamp;
			content += "\n------------------------------------------------------------\n";
			content += data;
            raf.seek(getOffset(content));
            raf.write(content.getBytes());
            raf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
		System.out.println("Finished writing data from " + data.getFirst() + " to output");
	}

	public File getFile(){
		return this.file;
	}

	private File createFile(){
		try {
	        file = new File("output.txt");
	        if (file.createNewFile()) {
	            System.out.println("File created: " + file.getName());
	        } else {
	            System.out.println("File already exists.");
	            file.delete();
	            file = new File("output.txt");
	        }
	    } catch (IOException e) {
	        System.err.println("An error occurred while creating the file: " + e.getMessage());
	        e.printStackTrace();
	    }
	    return file;
	}
}
