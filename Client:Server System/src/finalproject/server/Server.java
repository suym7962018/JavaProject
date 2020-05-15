package finalproject.server;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import finalproject.db.DBInterface;
import finalproject.entities.Person;

public class Server extends JFrame{

	public static final int DEFAULT_PORT = 8001;
	private static final int FRAME_WIDTH = 600;
	private static final int FRAME_HEIGHT = 800;
	final int AREA_ROWS = 10;
	final int AREA_COLUMNS = 40;
	private JPanel controlPanel;
	private JMenu menu;
	private JMenuItem exit;
	private JMenuBar menuBar;
	private JButton queryDB;
	private JLabel serverLabel;
	private JPanel middlePanel;
	private JPanel upperPanel;
	private JTextArea text;
	private Connection conn;
	private PreparedStatement queryAllData;
	private int clientNo = 0;
	
	ServerSocket server = new ServerSocket(8001);
	
	public Server() throws IOException, SQLException {
		this(DEFAULT_PORT, "server.db");
	}
	
	public Server(String dbFile) throws IOException, SQLException {
		this(DEFAULT_PORT, dbFile);
		conn = DriverManager.getConnection("jdbc:sqlite:server.db");
		queryAllData = conn.prepareStatement("Select * from People");
		new Thread(() -> {
			System.out.println("thread open");
			//text.append("starting thread for client" + clientNo + " at " + new Date() + '\n');
			while(true) {
				try {
					Socket socket = server.accept();
					clientNo++;
					InetAddress inetAddress = socket.getInetAddress();
					text.append("\n");
					text.append("starting thread for client" + clientNo + " at " + new Date() + '\n');
					text.append("client" + clientNo + " host name is " + inetAddress.getHostName() + '\n');
					text.append("client" + clientNo + " ip address is " + inetAddress.getAddress() + '\n');
					System.out.println("new thread");
					new Thread(new HandleClient(socket,clientNo)).start();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}).start(); 
	}

	public Server(int port, String dbFile) throws IOException, SQLException {

		this.setSize(Server.FRAME_WIDTH, Server.FRAME_HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		conn = DriverManager.getConnection("jdbc:sqlite:server.db");
		queryAllData = conn.prepareStatement("Select * from People");

	}
	private void createServerPanel(){
		this.setJMenuBar(createServerMenu());
		controlPanel = new JPanel();
		upperPanel = new JPanel();
		middlePanel = new JPanel();
		queryDB = new JButton("Query DB");
		queryDB.addActionListener(new queryButtonListener());
		serverLabel = new JLabel("DB : server.db");
		upperPanel.add(serverLabel);
		upperPanel.add(queryDB);
		middlePanel.add(createServerTextField());
		controlPanel.add(upperPanel);
		controlPanel.add(middlePanel);
		this.add(controlPanel);
	}
	private JMenuBar createServerMenu(){
		menuBar = new JMenuBar();
		menu = new JMenu("File");
		exit = new JMenuItem("Exit");
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menu.add(exit);
		menuBar.add(menu);
		return menuBar;
	}


	private JTextArea createServerTextField(){
		text = new JTextArea("Listening on port 8001",20,40);
		text.setEditable(false);
		return text;
	}
	class queryButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent event){
			try{
				PreparedStatement stmt = queryAllData;
				ResultSet result = stmt.executeQuery();
				ResultSetMetaData rsmd = result.getMetaData();
				int numColumns  = rsmd.getColumnCount();
				String rowString = "";
				boolean isHead = true;
				while(result.next()){
					if(isHead){
						for(int i = 1;i <= numColumns;i++){
							rowString += rsmd.getColumnName(i) + "\t";
						}
						rowString  += "\n";
						//debug1
						System.out.println("1 : " + rowString);
						isHead = false;
					}
					for(int i = 1;i <= numColumns;i++){
						Object o = result.getObject(i);
						//debug 2
						rowString += o.toString() + "\t";
						System.out.println("2 : " +rowString);
					}
					//debug 3
					rowString += "\n";
					System.out.println("3 : " +rowString);
				}
				System.out.println("rowString is : " + rowString);
				text.setText(rowString);
			}
			catch (SQLException e){
				e.printStackTrace();
			}
		}
	}
	public static void main(String[] args) {
		Server sv;
		try {
			sv = new Server("server.db");
			sv.createServerPanel();
			sv.setVisible(true);
		} catch (IOException | SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
//	@Override
//	public void run() {
//		try{
//			
//			text.append("starting thread for client" + clientNo + " at " + new Date() + '\n');
//			while (true){
//				Socket socket = server.accept();
//				clientNo ++;
//				InetAddress inetAddress = socket.getInetAddress();
//				text.append("client" + clientNo + " host name is " + inetAddress.getHostName() + '\n');
//				text.append("client" + clientNo + " ip address is " + inetAddress.getAddress() + '\n');
//				new Thread(new HandleClient(socket,clientNo)).start();
//			}
//		}
//		catch (IOException e){
//			System.out.println(e.getMessage());
//		}
//	}
	class HandleClient implements  Runnable{
		private Socket socket;
		private int clientNo;
		public HandleClient(Socket socket,int clientNo){
			this.socket = socket;
			this.clientNo = clientNo;
		}
		public void run(){
			try{
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
				BufferedWriter bw = new BufferedWriter(out);
				while(true){
					//TODO:insert successful or not
					try {
						Person p = (Person) in.readObject();
						//Object res = in.readObject();
						System.out.println(p.getName());
						//String res = "Success\n";
						//传送成功/失败
						//成功则insert
						PreparedStatement insertData = conn.prepareStatement("INSERT INTO People (first,last, age, city,sent) VALUES (?, ?, ?,?,?)");
						insertData.setString(1, p.getFirst());
						insertData.setString(2, p.getLast());
						insertData.setInt(3, p.getAge());
						insertData.setString(4, p.getCity());
						insertData.setBoolean(5,true);
						insertData.executeUpdate();
						System.out.println("Success");
						bw.write("Success");
						System.out.println("server sent");
						bw.close();
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (SQLException e){
						System.out.println(e.getErrorCode());
					}
				}
			}
			catch (IOException e){
				System.out.println(e.getMessage());
			}

		}
	}
}
