import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

@SuppressWarnings("unused")
public class POST1 //C.9.5
{
	private void serve(Socket s, ArrayList<Bid> offerte)
	{
		try{
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter out = new PrintWriter(s.getOutputStream(), true);
			StringBuffer request = new StringBuffer();
			
			String line = null;
			
			do{
				line = in.readLine();
				request.append(line);
				request.append("\n");
			}
			while(!line.equals(""));
			
			String response = processRequest(request.toString(), offerte);
			out.println(response);
		}
		catch(IOException ioe){
			ioe.printStackTrace();
		}
		finally{
			try{
				s.close();
			}
			catch(IOException ioe2){
				ioe2.printStackTrace();
			}
		}
	}
	
	private String processRequest(String request, ArrayList<Bid> offerte){
		String header = null;
		String body = null;
		StringBuffer response = new StringBuffer();
		
		if(request.startsWith("POST")){
			int begin = request.indexOf("POST") + 5;
			int end = request.indexOf(" HTTP/1.0");
			String resource = request.substring(begin, end);
			
			int idUtente;
			int idOggetto;
			int offerta;
			try {
				idUtente = Integer.parseInt(extract(request, "idUtente"));
				idOggetto = Integer.parseInt(extract(request, "idOggetto"));
				offerta = Integer.parseInt(extract(request, "offerta"));
				
				offerte.add(new Bid(idOggetto, idUtente, offerta, new Date()));
				
				header = buildHeader("200 OK");
				body = buildBody("Offerta inserita correttamente.");
				header = header + "\nContent-length: " + body.length();
		
			} catch (Exception e) {			
				e.printStackTrace();
				//dovrebbe inviare 400, ma non è richiesto nella traccia
			}			
		} 
		else {
			//non è una POST
			header = buildHeader("501 Not implemented");
			body = buildBody("Operazione non disponibile.");
			header = header + "\nContent-length: " + body.length();
		}
		
		response.append(header);
		response.append("\n");
		response.append(body);
		
		return response.toString();
	}

	private String extract(String request, String field){
		String value = null;
		int begin = request.indexOf(field) + field.length() + 1;
		int end = request.indexOf("&", begin);
		if(end == -1){
			value = request.substring(begin, request.indexOf("\n", begin));
		}else{		
			value = request.substring(begin, end);
		}
		return value;
	}
	
	private String buildHeader(String responseCode){
		StringBuffer header = new StringBuffer("HTTP/1.1 ");
		Date d = new Date();
		
		header.append(responseCode);
		header.append("\nDate: ");
		header.append(d.toString());
		//header.append("\n");
		
		return header.toString();
	}

	private String buildBody(String message){
		StringBuffer body = new StringBuffer();
		body.append("<html>\n<head>\n<h1>\n");
		body.append(message);
		body.append("\n</h1>\n</head>\n</html>");
		return body.toString();
	}
	


}
