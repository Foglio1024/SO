import java.io.*;
import java.net.*;
import java.util.*;

@SuppressWarnings("unused")
public class FTP1 
{
	private boolean session, user, login, passive;
	private final String ROOT = "/var/ftp";
	private final String USERNAME = "utente";
	private final String PASSWORD = "passutente";
	private final int ACT_DATAPORT = 20;
	private final int PASV_DATAPORT = 25601;
	private InetAddress clientAddr;
	private String username, password, cwd;
	private ServerSocket dataSrv = null;
	
	private void serve(Socket s, Map<String, String> accounts)
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			PrintWriter out = new PrintWriter(s.getOutputStream());
			
			cwd = ROOT;
			clientAddr = s.getInetAddress();
			session = true;
			user = false;
			login = false;
			passive = false;
			
			out.println("220 Java FTP Server ready.");
			
			while(session)
			{
				String request = in.readLine();
				process(request, out, accounts);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(s != null)
			{
				try
				{
					s.close();
					if(dataSrv !=null)
					{
						dataSrv.close();
						dataSrv = null;
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	private void process(String req, PrintWriter out, Map<String, String> acc)
	{
		if(req.equals("QUIT"))
		{
			//handle QUIT
			out.println("221 Goodbye.");
			session = false;
		}
		else if(req.startsWith("USER "))
		{
			//handle USER
			username = req.substring(5);
			user = true;
			login = false;
			out.println("331 User name OK, need password");
		}
		else if(req.startsWith("PASS "))
		{
			//handle PASS
			if(user)
			{
				password = req.substring(5);
				
				//caso singolo utente
				if(username.equals(USERNAME) && password.equals(PASSWORD))
				{
					login = true;
					out.println("230 Logged in.");
				}
				//caso dizionario di utenti
				String p = acc.get(username);
				if(p != null && p.equals(password))
				{
					login = true;
					out.println("230 Logged in.");					
				}
				//caso accesso anonimo
				if(username.equalsIgnoreCase("anonymous"))
				{
					login = true;
					out.println("230 Logged in.");
				}																	//i tre casi sono separati					
				else
				{
					login = false;
					user = false;
					out.println("530 Not logged in");
				}								
			}
			else
			{
				out.println("503 Bad sequence of commands");
			}
		}
		else if(req.startsWith("STOR "))
		{
			//handle STOR
			String f = ROOT + req.substring(5);
			if(login)
			{
				out.println("150 File status okay; about to open data connection.");
				
				try
				{
					receiveFile(f);
					out.println("226 File send OK; closing data connection."); 
				}
				catch(Exception e)
				{
					e.printStackTrace();
					out.println("426 Connection closed; tansfer aborted.");
				}
			}
			else
			{
				out.println("532 Need account for storing files.");
			}
			
		}
		else if(req.startsWith("RETR "))
		{
			//handle RETR
			File f = new File(ROOT + req.substring(5));
			if(login)
			{
				if(f.exists())
				{
					out.println("150 File status okay; about to open data connection.");
					try
					{
						sendFile(f);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		else if(req.equals("PASV"))
		{
			//handle PASV
			if(login)
			{
				passive = true;
				out.println("227 Entering passive mode (193, 204, 59, 75, A, B)");
			}
			else
			{
				out.println("530 Not logged in.");
			}
		}
		else if(req.startsWith("LIST "))
		{
			//handle LIST
			if(login)
			{
				File f = new File(ROOT + req.substring(5));
				
				if(f.exists() && f.isDirectory())
				{
					out.println("150 File status ok; about to open data connection.");
					try
					{
						sendListing(f);
						out.println("226 File send OK; Closing data connection.");
					}
					catch(Exception e)
					{
						e.printStackTrace();
						out.println("426 Connection closed; transfer aborted.");
					}
				}
				else
				{
					out.println("550 File not available.");
				}
			}
			else
			{
				out.println("530 Not logged in.");
			}
		}
		else if(req.startsWith("CWD "))
		{
			//handle CWD
			String newDir = cwd + File.separator + req.substring(5);
			File f = new File(newDir);
			
			if(f.exists() && f.isDirectory())
			{
				cwd = newDir;
				out.println("250 Requested action okay, completed.");
			}
			else
			{
				out.println("550 Requested action not taken.");
			}
		}
		else
		{
			//handle 502
			out.println("502 Not implemented.");
		}
	}
	
	private void receiveFile(String f) throws Exception		//per STOR
	{
		Socket data;
		if(passive)
		{
			if(dataSrv == null)
			{
				dataSrv = new ServerSocket(PASV_DATAPORT);
			}
			data = dataSrv.accept();
		}
		else
		{
			data = new Socket(clientAddr, ACT_DATAPORT);
		}
		BufferedReader r = new BufferedReader(new InputStreamReader(data.getInputStream()));
		PrintWriter w = new PrintWriter(f);
		
		String l = r.readLine();
		
		while(l != null)
		{
			w.println(l);
			l = r.readLine();
		}
		w.flush();
		w.close();
		r.close();
		data.close();
	}
	
	private void sendFile(File f)	 throws IOException						//per RETR
	{
		Socket data;
		if(passive)
		{
			if(dataSrv == null)
			{
				dataSrv = new ServerSocket(PASV_DATAPORT);
			}
			data = dataSrv.accept();
		}
		else
		{
			data = new Socket(clientAddr, ACT_DATAPORT);
		}
		
		BufferedReader r = new BufferedReader(new FileReader(f));
		PrintWriter w = new PrintWriter(data.getOutputStream());
		String l = r.readLine();
		
		while(l != null)
		{
			w.println();
			l = r.readLine();
		}
		w.flush();
		w.close();
		r.close();
		data.close();
	}
	
	private void sendListing(File f) throws IOException				//per LIST
	{
		Socket data;
		if(passive)
		{
			if(dataSrv == null)
			{
				dataSrv = new ServerSocket(PASV_DATAPORT);
			}
			data = dataSrv.accept();
		}
		else
		{
			data = new Socket(clientAddr, ACT_DATAPORT);
		}
		
		PrintWriter w = new PrintWriter(data.getOutputStream());
		String[] l = f.list();
		
		for (int i = 0; i < l.length; i++)
		{
			w.println(l[i]);
		}
		w.flush();
		w.close();
		data.close();
	}
}
