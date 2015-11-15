import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Client extends JFrame 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Socket socket;
	private int openFile;
	private JFileChooser fileChooser = new JFileChooser();
	private String path;
	
	private JTextArea display = new JTextArea();
	
	private JButton connect = new JButton("Connect");
	private JButton addFiles = new JButton("Add Files");
	private JButton disconnect = new JButton("Disconnect");
	private JButton send = new JButton("Send");
	private JButton help = new JButton("Help");
	private JButton exit = new JButton("Exit");
	
	private JPanel buttonPanel = new JPanel();
	private JScrollPane scrollPane = new JScrollPane();
	//File Streams
	private InputStream fileToSend;
	private BufferedReader reader;
	//streams for network IO
	private ObjectInputStream input;
	private ObjectOutputStream output;
	private File file;
	private String inputpath;
	private int counter = 0;

	public Client() 
	{
		super("FTP Client");
		Container cont = getContentPane();
		//Add panels
		cont.add(buttonPanel, BorderLayout.SOUTH);
		//Add Display
		display.setEditable(false);
		cont.add(display, BorderLayout.CENTER);
		cont.add(new JScrollPane(display), BorderLayout.CENTER);
		//Add Buttons
		buttonPanel.add(connect);
		buttonPanel.add(addFiles);
		buttonPanel.add(send);
		buttonPanel.add(disconnect);
		buttonPanel.add(help);
		buttonPanel.add(exit);
	
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setSize(500,500);
        
        connect.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		//Counter for how many times the user clicked connect and only allow one connection 
        		++counter;
        		if(counter <= 1)
        		{	
	        		try 
	        		{
						connectToServer();
						getStreams();
					} 
	        		catch (IOException | NullPointerException e1) 
	        		{
	        			counter = 0;
						displayMessage("Could not connect to server.");
					} 
        		}
        		else
        		{
        			displayMessage("Already connected.");
        		}
        	}
        });
        
        addFiles.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		fileChooser = new JFileChooser();
    			FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Document", "txt", "html", "htm", "xhtml");
    			fileChooser.setFileFilter(filter);
    			fileChooser.setMultiSelectionEnabled(false);
    			openFile = fileChooser.showOpenDialog(null);
    			if(openFile == JFileChooser.APPROVE_OPTION)
    			{
    				file = fileChooser.getSelectedFile();
    				inputpath = file.getAbsolutePath().toString();
    				path = file.getName().toString();
    				displayMessage("File to send: " + inputpath);
    			}
    			else
    			{
    				displayMessage("No file was selected.");
    			}
        	}
        });
        
        send.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		try 
        		{
					processConnection();
				} 
        		catch (Exception e1) 
        		{
					e1.printStackTrace();
				}
        	}
        });
        
        disconnect.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		closeConnection();
        		counter = 0;
        	}
        });
        
        help.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		JOptionPane.showMessageDialog(null, "To start sending files to the server: \n"
        				+ "1. Connect to server.\n"
        				+ "2. Add files.\n"
        				+ "3. Send files.\n"
        				+ "When you are ready to disconnect click disconect, or exit the application.", "Help" , JOptionPane.INFORMATION_MESSAGE);
        	}
        });
        
        exit.addActionListener(new ActionListener()
        {
        	public void actionPerformed(ActionEvent e)
        	{
        		closeConnection();
        		System.exit(-1);
        	}
        });
        
        this.addWindowListener(new WindowAdapter() 
        {
            public void windowClosing(WindowEvent ev) 
            {
            	closeConnection();
            }
        });
        
	}
	
	private void connectToServer()
	{
		try
		{
			int port = 21;
			socket = new Socket("127.0.0.1", port);
			
			displayMessage("Connected to: " + socket.getInetAddress().getHostName());
		}
		catch(IOException e)
		{
			displayMessage("Could not connect to server");
			counter = 0;
		}
	}
	
	private void getStreams() throws IOException 
	{
		output = new ObjectOutputStream(socket.getOutputStream());
		
		input = new ObjectInputStream(socket.getInputStream());
		displayMessage("Got I/O streams.");
	}
	
	private void processConnection() throws Exception
	{	
		//read file
		Path filePath = null;
		try
		{
			filePath = Paths.get(inputpath);
			fileToSend = Files.newInputStream(filePath);
			reader = new BufferedReader(new InputStreamReader(fileToSend));
			//send file name
			output.writeObject(path);
			output.flush();
			
			//send file data
			String linesToSend = reader.readLine();
			while(linesToSend != null)
			{
				output.writeObject(linesToSend);
				output.flush();
				linesToSend = reader.readLine();
			}
			//Tell server when data has finished
			output.writeObject("END");
			output.flush();
			displayMessage("File " + path + " is transfering");
			//get message from server indicating file was sent
			String serverip = (String)input.readObject();
			displayMessage(serverip);
		}
		catch(NullPointerException e)
		{
			displayMessage("No file to send.");
		}
		catch(SocketException e)
		{
			displayMessage("Not connected to server.");
			counter = 0;
		}
	}
	
	private void closeConnection()
	{
		try
		{
			output.close();
			input.close();
			socket.close();
			displayMessage("Connection closed.");
		}
		catch(NullPointerException | IOException e)
		{
			displayMessage("Not connected to server.");
		}
	}
	
	public void displayMessage(final String messageToDisplay)
	{
		SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run()
					{
						display.append(messageToDisplay + "\n");
					}
				});
	}

	public static void main(String [] args)
	{
		Client theApp = new Client();
		theApp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		theApp.setLocationRelativeTo(null);
		theApp.setVisible(true);
	}
} 
