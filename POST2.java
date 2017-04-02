import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

@SuppressWarnings("unused")
public class POST2 //C.9.9
{
	private Cinguettio serve(Socket s)
	{
		Cinguettio c = new Cinguettio(null, null, null);
		try
		{	
			//parse request
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter out = new PrintWriter(s.getOutputStream());
			String line = null;
			StringBuffer request = new StringBuffer();
			
			do
			{
				line = in.readLine();
				request.append(line);
				request.append("\n");
			}
			while(!line.equals(""));
			
			//build response
			
			String response = processRequest(request.toString(), c);
			out.println(response);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				s.close();
			}
			catch(IOException e2)
			{
				e2.printStackTrace();
			}
		}
		
		//return Cinguettio
		return c;

	}
	
	private String processRequest(String request, Cinguettio cing)
	{
		String header = null;
		String body = null;
		
		if(request.startsWith("POST"))
		{		
			//handle request
			try
			{
				//200
				//ignoro il parsing della risorsa perché non è richiesta l'implementazione del 404
				String encodedUser = extractFieldData(request, "user");
				String encodedText = extractFieldData(request, "text");
				
				String user = URLDecoder.decode(encodedUser, "UTF-8");
				String text = URLDecoder.decode(encodedText, "UTF-8");
				Date d = new Date();
	
				cing.date = d;
				cing.user = user;
				cing.content = text;
				
				header = buildHeader("200 OK");
				body = buildBody("Post succeful.");
				header = header + "\nContent-length: " + body.length();
				
			}
			catch(Exception e)
			{
				//400
				header = buildHeader("400 Bad request");
				body = buildBody("Invalid data.");
				header = header + "\nContent-length: " + body.length();
			}
		}
		else
		{
			//501
			header = buildHeader("501 Not implemented");
			body = buildBody("Command not available.");
			header = header + "\nContent-length: " + body.length();
		}
		
		//create and return response
		StringBuffer response = new StringBuffer();
		response.append(header);
		response.append("\n");
		response.append(body);
		return response.toString();
		
	}
	
	private String buildHeader(String code)
	{
		//build http response header
		Date d = new Date();
		StringBuffer header = new StringBuffer("HTTP/1.0 ");
		header.append(code);
		header.append("\nDate: ");
		header.append(d.toString());
		
		return header.toString();
	}
	
	private String buildBody(String message)
	{
		//build http response body
		StringBuffer body = new StringBuffer("<html><head/><body><h1>\n");
		body.append(message);
		body.append("\n</h1></body></html>");
		
		return body.toString();
	}
	
	private String extractFieldData(String request, String field)
	{
		//return string data from request
		int begin = request.indexOf(field) + field.length()+1;
		int end = request.indexOf("&", begin);
		String val = null;
		if(end != -1)
		{
			val = request.substring(begin, end);
		}
		else
		{
			val = request.substring(begin, request.indexOf("\n"));
		}
		
		return val;
	}
}
