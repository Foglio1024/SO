import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public class GET1 //C.9.4
{
	public static final String DOC_ROOT = "/var/www/html";

	@SuppressWarnings("unused")
	private void serve(Socket s)
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter out = new PrintWriter(s.getOutputStream(),true);
			StringBuffer request = new StringBuffer();
			
			String line = null;
			
			do
			{
				line = in.readLine();
				request.append(line);
				request.append("\n");
			} 
			while (!line.equals(""));
			
			String response = processRequest(request.toString());
			out.println(response);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		finally
		{
			try
			{
				s.close();
			} 
			catch(IOException ioe2)
			{
				ioe2.printStackTrace();
			}
		}
	}
	
	private String processRequest(String req)

	{
		String header = null;
		String body = null;
		StringBuffer response = new StringBuffer();
		
		if(req.startsWith("GET"))  
		{
			//se è una GET
			int begin = 4 + req.indexOf("GET");
			int end = req.indexOf(" HTTP/1.0");
			String resourcePath = req.substring(begin, end);
			File resource = new File(DOC_ROOT + resourcePath);
			
			if(resource.exists())
			{
				//se la risorsa esiste
				boolean sendNotModified;
				Date lastModified = null;
				String field = "If-modified-since: ";
				begin = req.indexOf(field);
				if(begin == -1)
				{
					sendNotModified = false;
				}
				else
				{
					begin += field.length();
					end = req.indexOf("\n", begin);
					String sinceString = req.substring(begin, end);
					
					Date since = null;
					
					try
					{
						since = DateFormat.getDateInstance().parse(sinceString);
					}
					catch(ParseException pe)
					{
						pe.printStackTrace();
					}
					lastModified = new Date(resource.lastModified());
					
					if(since == null || since.before(lastModified))
					{
						//risorsa modificata, quindi da inviare
						sendNotModified = false;
					}
					else
					{
						sendNotModified = true;
					}
				}
				if(sendNotModified)
				{
					//non è stata modificata, quindi 304
					header = buildDefaultHeader("304 Not Modified", null);
					header = header + "\nLast-modified: " + lastModified.toString();
					body = "";
				}
				else
				{
					//è stata modificata, invio
					header = buildDefaultHeader("200 OK", resource);
					body = readResource(resource);
				}
			}
			else
			{
				//risorsa non trovata, 404
				header = buildDefaultHeader("404 Not found", null);
				body = buildErrorBody("Resource not found: bad URL");
				header = header + "\nContent-Length: " + body.length();
			}
			
		}
		else
		{
			//non è una GET, 501
			header = buildDefaultHeader("501 Not implemented", null);
			body = buildErrorBody("Command not implemented");
			header = header + "\nContent-length: " + body.length();
		}
		
		response.append(header);
		response.append("\n");
		response.append(body);
		return response.toString();	
	}

	private String buildDefaultHeader(String code, File resource)
	{
		StringBuffer header = new StringBuffer("HTTP/1.0 ");
		Date d = new Date();
		
		header.append(code);
		header.append("\nDate: ");
		header.append(d.toString());
		header.append("\n");
		
		if(resource != null)
		{
			Date lm = new Date(resource.lastModified());
			header.append("Last-modified: ");
			header.append(lm.toString());
			header.append("\nContent-length: ");
			header.append(resource.length());
			header.append("\n");
		}
		
		return header.toString();
	}

	private String buildErrorBody(String message)
	{
		StringBuffer body = new StringBuffer("<html>\n<head>\n</head>\n<body>\n<h1>");
		body.append(message);
		body.append("\n</h1>\n</body>\n</html>\n");
		return body.toString();
	}

	private String readResource(File resource)
	{
		StringBuffer buf = new StringBuffer();
		BufferedReader reader = null;
		
		try
		{
			reader = new BufferedReader(new FileReader(resource));
			String line = reader.readLine();
			
			while(line != null)
			{
				buf.append(line);
				buf.append("\n");
				line = reader.readLine();
			}
		}
		catch (IOException ioe)
		{
			return ""; //si dovrebbe rispondere con 500 Internal server error
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(IOException ioe2)
				{
					ioe2.printStackTrace();
				}
			}
		}
		return buf.toString();
	}
}



