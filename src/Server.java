import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

import static java.nio.file.StandardOpenOption.*;

import javax.swing.*;

public class Server extends JFrame 
{	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Socket socket;
	private ServerSocket ss;
	private JTextArea display;
	//Panels
	private JPanel bottom = new JPanel();
	//Buttons
	private JButton destination = new JButton("Destination");
	private JButton help = new JButton("Help");
	private JButton exit = new JButton("Exit");
	    
	private JFileChooser fileLocation = new JFileChooser();
	private JScrollPane scrollPane = new JScrollPane();
	//File Streams
	private OutputStream fileOutput;
	
	//streams for network IO
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private int counter = 1, openFile;
	private String pathToString, message;
	private File f;
	
	    public Server()
	    {
	        super("FTP Server");
	        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
	        this.setLayout(new BorderLayout());
	        this.setLocationRelativeTo(null);
	        this.setResizable(false);
	        this.add(bottom, BorderLayout.SOUTH);
	        
	        Container cont = getContentPane();
	        display = new JTextArea();
	        display.setEditable(false);
	        cont.add(display, BorderLayout.CENTER);
	        cont.add(new JScrollPane(display), BorderLayout.CENTER);
	        
	        bottom.add(destination);
	        bottom.add(help);
	        bottom.add(exit);
	        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	        
	        setSize(500, 300);
	        
	        f = new File("C:\\");
	        fileLocation.setCurrentDirectory(f);
	        
	        destination.addActionListener(new ActionListener()
	        {
	        	public void actionPerformed(ActionEvent e)
	        	{
	        		fileLocation = new JFileChooser();
	        		fileLocation.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	        		fileLocation.setCurrentDirectory(f);
	        		fileLocation.setAcceptAllFileFilterUsed(false);
	        		openFile = fileLocation.showOpenDialog(null);
	        		if(openFile == JFileChooser.APPROVE_OPTION)
	        		{
	        			pathToString = fileLocation.getSelectedFile().toString();
		        		f = new File(pathToString);
		        		fileLocation.setCurrentDirectory(f);
		        		displayMessage("Files will be located in " + pathToString);
	        		}
	        		else
	        		{
	        			displayMessage("Files will be located in " + f);
	        		}
	        	}
	        });
	        
	        help.addActionListener(new ActionListener()
	        {
	        	public void actionPerformed(ActionEvent e)
	        	{
	        		JOptionPane.showMessageDialog(null, "1. Start the server befor transfering any files.\n"
	        				+ "2. Use the destination button to select where you would like your files to be transfered to.\n"
	        				+ "3. Click exit to close the server.\n"
	        				+ "NOTE: The server will not close if there are clients connected.", "Instructions", JOptionPane.INFORMATION_MESSAGE);
	        	}
	        });
	        
	        exit.addActionListener(new ActionListener()
	        {
	        	public void actionPerformed(ActionEvent e)
	        	{
	        		if(input != null)
	        		{
	        			displayMessage("Client still connected. Please wait for connection to be terminated.");
	        		}
	        		else
	        		{
	        			closeConnection();
	        			System.exit(-1);
	        		}
	        	}
	        });
	        
	        this.addWindowListener(new WindowAdapter() 
	        {
	            public void windowClosing(WindowEvent ev) 
	            {
	            	if(input != null)
	        		{
	            		displayMessage("Client still connected. Please wait for connection to be terminated.");
	        		}
	        		else
	        		{
	        			closeConnection();
	        			System.exit(-1);
	        		}
	            }
	        });
	    }
	    
	    public void runServer()
	    {
			try
			{
				ss = new ServerSocket(21);
				
				while(true)
				{
					try
					{
						waitForConnection();
						getStreams();
						processConnection();
					}
					catch(EOFException e)
					{
						displayMessage("Connection terminated.");
						input = null;
					}
					finally
					{
						++counter;
					}
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	    
	    private void waitForConnection() throws IOException
	    {
	    	displayMessage("Waiting for connection.");
	    	socket = ss.accept();
	    	displayMessage("Connection " + counter + " recieved from " + socket.getInetAddress().getHostName());
	    }
	    
	    private void getStreams() throws IOException
	    {
	    	output = new ObjectOutputStream(socket.getOutputStream());
	    	output.flush();
	    	
	    	input = new ObjectInputStream(socket.getInputStream());
	    	
	    	displayMessage("Got I/O streams.");
	    }
	    
	    private void processConnection() throws IOException
	    {
	    	while(socket.isBound())
	    	{
	    		try
	    		{	
	    			//read first line of input for file name
	    			String clientInput = (String) input.readObject();
	    			//create file path
	    			String abFilePath = pathToString + "\\" + clientInput;
	    			// create file to write in
	    			FileSystem fs = FileSystems.getDefault();
	    			Path path = fs.getPath(abFilePath);
	    			/*I don't want to overwrite files so I will create a duplicate and inform the user
	    			 * I know there is a way to add the (1) at the end of the file but I don't know 
	    			 * how at the moment. Maybe you could give me a hint in the comments.
	    			 */
	    			fileOutput = new BufferedOutputStream(Files.newOutputStream(path, CREATE_NEW));
	    			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fileOutput));
	    			//read rest of data
	    			String s = null;
    				do
	    			{
    					s = (String)input.readObject();
    					if(!s.equals("END"))
    					{
	        				writer.write(s);
	        				writer.flush();
	        				writer.newLine();
    					}
    					else
    					{
    						s.equals("END");
    					}
	    			}//Check for end of file
    				while(!s.equals("END"));
	    			writer.close();
	    			displayMessage("File " + clientInput + " was recieved.");
	    			//inform user the file was sent
	    			message = clientInput + " was sent to server.";
	    			sendData(message);
	    			
	    		}
	    		catch(SocketException | NullPointerException e)
	    		{
	    			displayMessage("Client disconnected");
	    			input = null;
	    		}
	    		catch(FileAlreadyExistsException e)
	    		{
	    			sendData("File already exist. Duplicate created.");
	    		} 
	    		catch (ClassNotFoundException e) 
	    		{
	    			//just in case the user somehow sent nothing
					displayMessage("File not found.");
				}
	    	}
	    }
	    
	    private void closeConnection()
	    {
	    	displayMessage("Terminating connection.");
	    	
	    	try
	    	{
	    		output.close();
	    		input.close();
	    		socket.close();
	    		input = null;
	    	}
	    	catch(IOException e)
	    	{
	    		e.printStackTrace();
	    	}
	    	catch(NullPointerException e)
	    	{
	    		input = null;
	    	}
	    }
	    
	    private void sendData(String message)
	    {
	    	try
	    	{
	    		output.writeObject(message);
	    		output.flush();
	    	}
	    	catch(IOException e)
	    	{
	    		e.printStackTrace();
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
	    
	    public static void main(String [] args) throws Exception
	    {
	       Server theApp = new Server();
	       theApp.setLocationRelativeTo(null);
	       theApp.setVisible(true);
	       theApp.runServer();
	    } 
	}
