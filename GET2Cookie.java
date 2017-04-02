import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

@SuppressWarnings("unused")
public class GET2Cookie //C.9.6
{
	final long expireInterval = 30L; //30 giorni
	final String cookieName = "RECENT-VISITS";
	private void serve(Socket s)
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter out = new PrintWriter(s.getOutputStream());
			StringBuffer request = new StringBuffer();
			
			String line = null;
			do
			{
				line = in.readLine();
				request.append(line);
				request.append("\n");
			} 
			while(!line.equals(""));
			
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
	
	private String processRequest(String request)
	{
		String header = null;
		String body = null;
		StringBuffer response = new StringBuffer();
		
		if(request.startsWith("GET"))
		{
			// decifra la risorsa
			int begin = request.indexOf("GET ") + 4;
			int end = request.indexOf(" HTTP/1.");
			String resourcePath = request.substring(begin, end);
			File resource = new File("/var/www/html/" + resourcePath);
			
			if(resource.exists())
			{
				//200
				header = buildHeader("200 OK");
				body = readResource(resource);
				header = header + "\nContent-length: " + body.length();
				
				String recentVisits = null;
				String cookieVal = null;
				Date d = new Date();
				//verifico se il cookie è già presente
				begin = request.indexOf("Cookie: ");
				if(begin != -1)
				{
					//il messaggio contiene cookie
					begin = request.indexOf(cookieName);
					if(begin != 1)
					{
						//il messaggio contiene il cookie che riguarda le utlime due visite, lo devo aggiornare
						end = request.indexOf(";", begin);
						recentVisits = request.substring(begin, end);
						
						//il campo comment del cookie ha formato RECENT-VISITS=data1-data0
						
						String date1 = recentVisits.substring(0, recentVisits.indexOf("-"));

						cookieVal = d.toString() + "-" + date1;
					}
					else
					{
						//il messaggio non contiene il cookie RECENT-VISITS
						cookieVal = d.toString() + "-";
					}	
				}
				else
				{
					//il messaggio non contiene cookie
					cookieVal = d.toString() + "-";
				}
				//crea cookie
				Date expire = new Date(d.getTime() + expireInterval *24L * 3600L * 1000L);
				String cookie = "Set-Cookie: " + cookieName + "=" + cookieVal + "; path=/; expires=" + expire.toString();
				header = header + "\n" + cookie;
			}
			else
			{
				//404
				header = buildHeader("404 Not found");
				body = buildErrorBody("Resource not found: Bad URL");
				header = header + "\nContent-length: " + body.length();
			}
		}
		else
		{
			//501
			header = buildHeader("501 Not implemented");
			body = buildErrorBody("Command not available.");
			header = header + "\nContent-length: " + body.length();
		}
		
		//costruisci e ritorna risposta
		response.append(header);
		response.append("\n");
		response.append(body);
		return response.toString();
	}
	
	private String readResource(File resource)
	{
		StringBuffer fileString = new StringBuffer();
		BufferedReader reader = null;
		
		try
		{
			reader = new BufferedReader(new FileReader(resource));
			String line = reader.readLine();
			
			while(line != null)
			{
				fileString.append(line);
				fileString.append("\n");
				line = reader.readLine();
			}
			
		}
		catch(Exception e)
		{
			return ""; //500, ma non implementato
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					
					reader.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return fileString.toString();
	}
	private String buildErrorBody(String message)
	{
		StringBuffer sb = new StringBuffer("<html><head></head><h1>\n");
		sb.append(message);
		sb.append("\n</h1><body><html>");
		
		return sb.toString();
	}
	private String buildHeader(String code)
	{
		StringBuffer header = new StringBuffer("HTTP/1.0 ");
		Date d = new Date();
		
		header.append(code);
		header.append("\n");
		header.append(d.toString());
		header.append("\n");
		
		return header.toString();
	}
}
