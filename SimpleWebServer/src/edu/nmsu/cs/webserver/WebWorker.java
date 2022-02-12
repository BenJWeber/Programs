package edu.nmsu.cs.webserver;

/**
 * "Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format)." - original description by Jon Cook, Ph.D.
 * 
 * Some details have been changed, including how run() works and additional functionality like 
 * error 404 handling, and HTML file serving. run() now hands off processing to readHTTPRequest().
 * 
 * @author Jon Cook, Ph.D.
 * @author Benjamin Weber
 *
 *
 **/

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			
			// removed calls to writeHTTPRequest() and writeContent()
			// readHTTPRequest() will process these calls now
			readHTTPRequest(is, os);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header and decide how to respond.
	 **/
	private void readHTTPRequest(InputStream is, OutputStream os)
	{
		String line;
		String fileName;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
				
				// check if GET request
				if (line.substring(0, 3).equals("GET")) {
					
					// store path of GET request
					fileName = line.substring(4, line.lastIndexOf(" "));
					
					// if fileName is not root then attempt to open with absolute path
					// otherwise call default writeContent()
					if (!(fileName.equals("/"))) {
						fileName = System.getProperty("user.dir") + fileName;
						File checkFile = new File(fileName);
						if (checkFile.exists()) {
							writeHTTPHeader(os, "text/html", 200);
							writeContentFile(os, checkFile);
						}
						
						// if file didn't exist then send error 404
						else {
							writeHTTPHeader(os, "text/html", 404);
							write404(os);
						}
					}
					else {
						writeHTTPHeader(os, "text/html", 200);
						writeContent(os);
					}
				}
				
				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		return;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 * @
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, int code) throws Exception
	{
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		// check for error code
		if (code == 404)
			os.write("HTTP/1.1 404\n".getBytes());
		else
			os.write("HTTP/1.1 200 OK\n".getBytes());
		
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Ben's server\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os) throws Exception
	{
		os.write("<html><head></head><body>\n".getBytes());
		os.write("<h3>My web server works!</h3>\n".getBytes());
		os.write("</body></html>\n".getBytes());
	}
	private void writeContentFile(OutputStream os, File file) throws Exception {
		// get date
		Date d = new Date();
		DateFormat df = DateFormat.getDateInstance();
		df.setTimeZone(TimeZone.getTimeZone("MST"));
		
		// pull file into string for processing and replace custom tags
		String fileString = new String(Files.readAllBytes(file.toPath()));
		fileString = fileString.replace("<cs371date>", df.format(d));
		fileString = fileString.replace("<cs371server>", "Ben's Server");
		
		// send file to output
		os.write("<html><head></head><body>\n".getBytes());
		os.write(("<p>" + fileString + "</p>\n").getBytes());
		os.write("</body></html>\n".getBytes());

	}
	private void write404(OutputStream os) throws Exception {
		os.write("<html><head></head><body>\n".getBytes());
		os.write("<h3>404 Not Found</h3>\n".getBytes());
		os.write("</body></html>\n".getBytes());
	}

} // end class
