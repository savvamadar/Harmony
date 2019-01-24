import java.awt.AWTException;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class HarmonyApp extends JFrame{
		
	private JButton server_button;
	private JButton client_button;
	private JPanel firstPanel;
	
	private JLabel client_label;
	private JTextField client_input;
	private JButton client_connect_button;
	private JPanel clientPanel;
	
	private JLabel server_label;
	
	DefaultListModel<String> listModel;
	private JList<String> server_queue_list;
	
	private JScrollPane server_queue_pane;
	private JButton[] server_buttons;
	private JPanel server_side_panel;
	private JPanel server_button_panel;
	private JPanel serverPanel;
	
	public String getLocalIPAddressBAK()
    {
		try {
			return InetAddress.getLocalHost().toString();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			return "Error: "+e.toString();
		}
    }
	
	public String getLocalIPAddress()
    {
		try {
			DatagramSocket socket = new DatagramSocket(null);
			InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("8.8.8.8"), 65530);
			socket.connect(address);
			//socket.bind(address);
			String ip = socket.getLocalAddress().getHostAddress().toString();
			socket.close();
			if(ip.equals("0.0.0.0")) {
				return getLocalIPAddressBAK();
			}
			return ip;
		} catch(Exception ex) {
			return getLocalIPAddressBAK();
		}
    }
	
	DatagramSocket dsocket;
	
	DatagramPacket outBoundPacket;
	
	DatagramPacket inBoundPacket;
	byte[] receiveData = new byte[1024];
	
	private void sendMessage(String msg, String ip)
    {
		try {
			byte[] msgbytes = msg.getBytes();
			outBoundPacket = new DatagramPacket(msgbytes, msgbytes.length, InetAddress.getByName(ip), applicationPort);
			if(dsocket==null) {
				dsocket = new DatagramSocket(applicationPort);
			}
		    dsocket.send(outBoundPacket);
		}
		catch(Exception ex) {
			System.out.println("Error: "+ex.toString());
			//JOptionPane.showMessageDialog(null,"Error: "+ex.toString());
		}
    }
	
	private UDPMessage receiveMessage() {
		try {
			if(dsocket==null) {
				dsocket = new DatagramSocket(applicationPort);
			}
			byte[] tempReceiveData = new byte[1024];
			inBoundPacket = new DatagramPacket(tempReceiveData, tempReceiveData.length);
			dsocket.receive(inBoundPacket);
			String ipToString = inBoundPacket.getAddress().toString();
			if(!Character.isDigit(ipToString.charAt(0))) {
				ipToString = ipToString.substring(1);
			}
			return new UDPMessage(new String(inBoundPacket.getData()).trim(), ipToString, inBoundPacket.getPort());
		}catch(Exception ex) {
			System.out.println("Error: "+ex.toString());
			//JOptionPane.showMessageDialog(null,"Error: "+ex.toString());
		}
		return null;
	}
	
	private int applicationPort = 25002;
	private String serverIP = "";
	private boolean isServer = false;
	private boolean isActivePC = false;
	
    private int xMin = 0;
    private int yMin = 0;
    private int xMax = 0;
    private int yMax = 0;
    private int envWidth = 0;
    private int envHeight = 0;
    private int envWidthMidPoint = 0;
    private int envHeightMidPoint = 0;
    

	
    
	Point currentMousePoint;
	HarmonyApp(){
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice arrGD[] = ge.getScreenDevices();
		for(int i=0;i<arrGD.length;i++) {
			Rectangle r = arrGD[i].getDefaultConfiguration().getBounds();
			if(r.y<yMin) {
				yMin = r.y;
			}
			if((r.y+r.height)>yMax) {
				yMax = r.y+r.height;
			}
			if(r.x<xMin) {
				xMin = r.x;
			}
			if((r.x+r.width)>xMax) {
				xMax = r.x+r.width;
			}
		}

		envWidthMidPoint = (xMax+xMin)/2;
		envHeightMidPoint = (yMax+yMin)/2;
		
		envWidth = xMax - xMin;
		envHeight = yMax - yMin;
		
		firstPanel = new JPanel();
		firstPanel.setLayout(new FlowLayout());
		
		server_button = new JButton("Server (Share this Mouse/ Keyboard)");
		server_button.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        becomeServer();
		    }
		});
		firstPanel.add(server_button);
		
		client_button = new JButton("Client (Use another Mouse/ Keyboard)");
		client_button.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		    	becomeClient();
		    }
		});
		firstPanel.add(client_button);
		
		this.setContentPane(firstPanel);

		this.setSize(300, 150);
		this.setResizable(false);
		this.setLocation(MouseInfo.getPointerInfo().getLocation());
		this.setTitle("Harmony");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.validate();
	}
	
	void becomeClient() {
		try {
			dsocket = new DatagramSocket(applicationPort);
		} catch (SocketException ex) {
			JOptionPane.showMessageDialog(null,"Error: "+ex.toString());
			System.exit(0);
		}
		
		isServer = false;
		isActivePC = false;
		
		clientPanel = new JPanel();
		clientPanel.setLayout(new FlowLayout());
		
		client_label = new JLabel("IP: ");
		clientPanel.add(client_label);
		
		client_input = new JTextField(20);
		clientPanel.add(client_input);
		
		client_connect_button = new JButton("Connect");
		client_connect_button.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
		        serverIP = client_input.getText();
		        sendMessage("j|"+System.getProperty("user.name")+"",serverIP);
		        if(receiveMessage().IP.contains(serverIP)) {
		        	client_input.setEnabled(false);
		        	client_connect_button.setEnabled(false);
		        	recvThread = new UDPThread();
		        	recvThread.start();
		        	hbThread = new HeartBeatThread();
		        	hbThread.start();
		        }
		        else {
		        	JOptionPane.showMessageDialog(null,"Received response from unexpected IP.");
		        }
		    }
		});
		clientPanel.add(client_connect_button);
		
		this.setContentPane(clientPanel);
		
		this.setSize(300, 150);
		
		this.setTitle("Harmony - Client");
		this.validate();
	}
	
	ArrayList<String> ipList = new ArrayList<String>();
	void becomeServer() {
		try {
			dsocket = new DatagramSocket(applicationPort);
		} catch (SocketException ex) {
			JOptionPane.showMessageDialog(null,"Error: "+ex.toString());
			System.exit(0);
		}
		
		isServer = true;
		isActivePC = true;
		
		serverPanel = new JPanel();
		serverPanel.setLayout(new FlowLayout());
		
		server_button_panel = new JPanel();
		server_button_panel.setLayout(new GridLayout(0,5));
		server_buttons = new JButton[25];
		
		for(int i=0;i<25;i++) {
			server_buttons[i] = new JButton("[EMPTY]");
			server_buttons[i].setEnabled(false);
			server_button_panel.add(server_buttons[i]);
		}
		String tempIP = getLocalIPAddress();
		while(tempIP.contains("/")) {
			tempIP = tempIP.substring(tempIP.indexOf('/')+1);
		}
		server_label = new JLabel("IP: "+tempIP);
		server_button_panel.add(server_label);
		
		server_side_panel = new JPanel();
		server_side_panel.setLayout(new FlowLayout());


		listModel = new DefaultListModel<String>();
		listModel.addElement("Queue");
		listModel.addElement("------------------------------------");

		
		server_queue_list = new JList<String>(listModel);
		
		server_queue_pane = new JScrollPane();
		server_queue_pane.setViewportView(server_queue_list);
		server_side_panel.add(server_queue_pane);
		
		

		
		serverPanel.add(server_button_panel);
		serverPanel.add(server_side_panel);
		
		this.setContentPane(serverPanel);
		
		this.setSize(800, 200);
		
		this.setTitle("Harmony - Server");

    	recvThread = new UDPThread();
    	recvThread.start();
    	
    	hbThread = new HeartBeatThread();
    	hbThread.start();
    	
        mThread = new MouseThread();
		mThread.start();
		
		addNewPC("This PC","0");

	}
	
	private float lerp(float min, float max, float lerpAmount) {
		return min+(max-min)*lerpAmount;
	}
	
	private float invertedLerp(float min, float max, float value) {
		return (value - min) / (max - min);
	}
	
	private void server_BecomeMain()
    {
        isActivePC = true;
        
        this.removeWindowStateListener(hwsl);
        this.removeComponentListener(hwm);
        this.removeWindowListener(hwcl);
        this.removeKeyListener(hkl);
        this.removeMouseListener(hml);
        this.removeMouseWheelListener(hmwl);
        
        this.dispose();
        this.setUndecorated(true);
        this.setOpacity(1f);

        this.setAlwaysOnTop(false);

        this.setSize(800, 200);
        if(oldPos==null) {
        	oldPos = MouseInfo.getPointerInfo().getLocation();
        }
        this.setLocation(oldPos);

        
        if (listModel.getSize() > 2)
        {
            enableSomeButtons();
        }
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setUndecorated(false);
        this.setVisible(true);
    }
	
	private boolean stopMouseThread = false;
	class MouseThread extends Thread{
		public void run() {
			while(!closeProgram && !stopMouseThread) {
				int xFast = MouseInfo.getPointerInfo().getLocation().x;
				int yFast = MouseInfo.getPointerInfo().getLocation().y;
				
				xMoved = xFast - xPrev;
				xPrev = xFast;
				
	            yMoved = yFast - yPrev;
	            yPrev = yFast;
	            if (!isActivePC && isServer)
	            {
	                xPrev = envWidthMidPoint;
	                yPrev = envHeightMidPoint;
	                
	                r.mouseMove(envWidthMidPoint, envHeightMidPoint);

	                if (!(xMoved == 0 && yMoved == 0))
	                {
	                    sendMessage("p|" + xMoved + "|" + yMoved+"", currentPC.pc_ip);
	                }

	            }
	            if (xFast <= (xMin+2))
	            {
	                if (isServer)
	                {
	                    if (switchPC(4,(int)(invertedLerp(yMin,yMax,yFast)*100)))
	                    {
	                    	r.mouseMove(envWidthMidPoint, yFast);
	                    }
	                }
	                else
	                {
	                    sendMessage("w|4|"+((int)(invertedLerp(yMin,yMax,yFast)*100)), serverIP);
	                }
	                xPrev = envWidthMidPoint;
	            }
	            else if (xFast >= (xMax-2))
	            {
	                if (isServer)
	                {
	                    if (switchPC(2,((int)(invertedLerp(yMin,yMax,yFast)*100))))
	                    {
	                    	r.mouseMove(envWidthMidPoint, yFast);
	                    }
	                }
	                else
	                {
	                    sendMessage("w|2|"+((int)(invertedLerp(yMin,yMax,yFast)*100)), serverIP);
	                }
	                xPrev = envWidthMidPoint;
	            }
	            else if (yFast <= (yMin+2))
	            {
	                if (isServer)
	                {
	                    if (switchPC(1,((int)(invertedLerp(xMin,xMax,xFast)*100))))
	                    {
	                    	r.mouseMove(xFast, envHeightMidPoint);
	                    }
	                }
	                else
	                {
	                    sendMessage("w|1|"+((int)(invertedLerp(xMin,xMax,xFast)*100)), serverIP);
	                }
	                yPrev = envHeightMidPoint;
	            }
	            else if (yFast >= (yMax-2))
	            {
	                if (isServer)
	                {
	                    if (switchPC(3,((int)(invertedLerp(xMin,xMax,xFast)*100))))
	                    {
	                    	r.mouseMove(xFast, envHeightMidPoint);
	                    }
	                }
	                else
	                {
	                    sendMessage("w|3|"+((int)(invertedLerp(xMin,xMax,xFast)*100)), serverIP);
	                }
	                yPrev = envHeightMidPoint;
	            }
	            try {
					this.sleep(33);
				} catch (InterruptedException e) {
					System.out.println("Sleep error: "+e.toString());
				}
			}
		}
	}
	
	class HeartBeatThread extends Thread {
		public void run() {
            while(!closeProgram) {
            	if(!isServer) {
            		sendMessage("h|", serverIP);
            	}
            	else if(pcList.size()>1){
            		for(PC pc_i : pcList) {
            			if(!pc_i.pc_ip.equals("0") && ((System.currentTimeMillis() - pc_i.lastHeartBeat)>10000)) {
            				pc_i.isOnline = false;
            			}
            		}
            		if(currentPC.isOnline==false) {
            			switchPC(-1,50);
            		}
            	}
            	try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
		}
	}
	
	private boolean closeProgram = false;
	class UDPThread extends Thread {
        UDPMessage msg;

        public void run() {
        	int framePreState = -1;
            while(!closeProgram) {
            	msg = receiveMessage();
            	String clientMessage = msg.Content;
                if (clientMessage.substring(0, 2).equals("j|"))
                {
                    addNewPC(clientMessage.substring(2), msg.IP);
                }
                else if (clientMessage.substring(0, 2).equals("s|"))
                {
                	if (isServer == false)
                    {
                		if(r==null) {
                			try {
								r = new Robot();
							} catch (AWTException e) {
								JOptionPane.showMessageDialog(null,"Unable to simulate input: "+e.toString());
								System.exit(0);
							}
                		}
                        String[] sep = clientMessage.split("\\|",-1);
                        if (sep[1].equals("1"))
                        {
                        	r.mouseMove((int)lerp(xMin,xMax,(Float.parseFloat(sep[2])/100)), yMax-50);
                        }
                        else if (sep[1].equals("3"))
                        {
                        	r.mouseMove((int)lerp(xMin,xMax,(Float.parseFloat(sep[2])/100)), yMin+50);
                        }
                        else if(sep[1].equals("2"))
                        {
                        	r.mouseMove(xMin+50, (int)lerp(yMin,yMax,(Float.parseFloat(sep[2])/100)));
                        }
                        else if (sep[1].equals("4"))
                        {
                        	r.mouseMove(xMax-50, (int)lerp(yMin,yMax,(Float.parseFloat(sep[2])/100)));
                        }
                        
                        framePreState = thisFrame.getState();
                        thisFrame.setState(Frame.ICONIFIED);

                        mThread = new MouseThread();
                        stopMouseThread = false;
                        mThread.start();
                    }
                }
                else if (clientMessage.substring(0, 2).equals("t|"))
                {
                    if (isServer == false)
                    {
                    	stopMouseThread = true;
                    	thisFrame.setState(framePreState);
                    	
            			int xFast = MouseInfo.getPointerInfo().getLocation().x;
            			int yFast = MouseInfo.getPointerInfo().getLocation().y;
            			
            			if(xFast<(xMin+50)) {
            				r.mouseMove((xMin+50), yFast);
            			}
            			else if(xFast> (xMax - 50)) {
            				r.mouseMove((xMax - 50), yFast);
            			}
            			if(yFast<(yMin+50)) {
            				r.mouseMove(xFast, (yMin+50));
            			}
            			else if(yFast > (yMax - 50)) {
            				r.mouseMove(xFast, (yMax - 50));
            			}
                    }
                }
                else if (clientMessage.substring(0, 2).equals("m|"))
                {
                    String mCommand = clientMessage.substring(2);
                    if (mCommand.equals("1d"))
                    {
                    	r.mousePress(InputEvent.BUTTON1_MASK);
                    }
                    else if (mCommand.equals("2d"))
                    {
                    	r.mousePress(InputEvent.BUTTON2_MASK);
                    }
                    else if (mCommand.equals("3d"))
                    {
                    	r.mousePress(InputEvent.BUTTON3_MASK);
                    }
                    else if (mCommand.equals("1u"))
                    {
                        r.mouseRelease(InputEvent.BUTTON1_MASK);
                    }
                    else if (mCommand.equals("2u"))
                    {
                    	r.mouseRelease(InputEvent.BUTTON2_MASK);
                    }
                    else if (mCommand.equals("3u"))
                    {
                    	r.mouseRelease(InputEvent.BUTTON3_MASK);
                    }
                    else
                    {
                        r.mouseWheel(Integer.parseInt(mCommand));
                    }
                }
                else if(clientMessage.substring(0,2).equals("p|"))
                {
        			int xFast = MouseInfo.getPointerInfo().getLocation().x;
        			int yFast = MouseInfo.getPointerInfo().getLocation().y;
                    String[] sep = clientMessage.split("\\|",-1);
                    r.mouseMove(xFast+Integer.parseInt(sep[1]), yFast + Integer.parseInt(sep[2]));
                }
                else if (clientMessage.substring(0, 2).equals("w|"))
                {
                    if (isServer)
                    {
                    	if(msg.IP.contains(currentPC.pc_ip)) {
                    		String[] sep = clientMessage.split("\\|",-1);
                    		switchPC(Integer.parseInt(sep[1]), Integer.parseInt(sep[2]));
                    	}
                    }
                }
                else if (clientMessage.substring(0, 2).equals("h|"))
                {
                	//receive heartbeat
                    if (isServer)
                    {
                    	
                    	for(PC pc_i : pcList) {
                    		if(msg.IP.contains(pc_i.pc_ip)) {
                    			//System.out.println("HB: "+pc_i.pc_ip);
                    			pc_i.setHeartBeat();
                    		}
                    	}
                    	/*for(int i=0;i<pcList.size();i++) {
                    		if(msg.IP.contains(pcList.get(i).pc_ip)) {
                    			pcList.get(i).setHeartBeat();
                    			i = pcList.size();
                    		}
                    	}*/
                    }
                }
                else if (clientMessage.substring(0, 3).equals("kd|"))
                {
                	try {
                    r.keyPress(Integer.parseInt(clientMessage.substring(3)));
                	} catch(Exception ex) {
                		System.out.println("Invalid: "+clientMessage.substring(3));
                	}
                }
                else if (clientMessage.substring(0, 3).equals("ku|"))
                {
                	try {
                		r.keyRelease(Integer.parseInt(clientMessage.substring(3)));
	                } catch(Exception ex) {
	            		System.out.println("Invalid: "+clientMessage.substring(3));
	            	}
                }
            }
            System.exit(0);
        }
    }
	
	private HarmonyWindowStateListener hwsl;
	private HarmonyWindowMoved hwm;
	private HarmonyWindowCloseListener hwcl;
	private HarmonyKeyboardListener hkl;
	private HarmonyMouseListener hml;
	private HarmonyMouseWheelListener hmwl; 

	private Point oldPos = null;
    private void server_BecomeSecondary()
    {
    	this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        isActivePC = false;
        this.dispose();
        this.setUndecorated(true);
        this.setOpacity(0.1f);
        oldPos = this.getLocation();
        this.setLocation(xMin-10, yMin-10);
        this.setSize(envWidth*2, envHeight*2);
        this.setAlwaysOnTop(true);
        this.setVisible(true);
        if(hwsl == null) {
        	hwsl = new HarmonyWindowStateListener();
        }
        if(hwm == null) {
        	hwm = new HarmonyWindowMoved();
        }
        if(hwcl == null) {
        	hwcl = new HarmonyWindowCloseListener();
        }
        if(hkl==null) {
        	hkl = new HarmonyKeyboardListener();
        }
        if(hml==null) {
        	hml = new HarmonyMouseListener();
        }
        if(hmwl == null) {
        	hmwl = new HarmonyMouseWheelListener();
        }

		this.setFocusable(true);
        this.requestFocusInWindow();
        
        if(r==null) {
        	try {
				r = new Robot();
			} catch (AWTException e) {
				JOptionPane.showMessageDialog(null,"Unable to constrain mouse: "+e.toString());
			}
        }
        
        this.addWindowStateListener(hwsl);
        this.addComponentListener(hwm);
        this.addWindowListener(hwcl);
        this.addKeyListener(hkl);
        this.addMouseListener(hml);
        this.addMouseWheelListener(hmwl);

        for (int i = 0; i < server_buttons.length; i++)
        {
        	server_buttons[i].setEnabled(false);
        }
    }
    
    JFrame thisFrame = this;
    
    private int xPrev = 0;
    private int yPrev = 0;
    Robot r = null;
    
    UDPThread recvThread;
    HeartBeatThread hbThread;
    MouseThread mThread;
    class HarmonyWindowCloseListener implements WindowListener {

		@Override
		public void windowOpened(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowClosing(WindowEvent e) {
			if(isServer) {
				if(isAlt<=0 && isF4 == false) {
					if(recvThread == null) {
						System.exit(0);
					}
					else {
						closeProgram = true;
					}
				}
			}
			else {
				//send message to server to remove client
				if(recvThread == null) {
					System.exit(0);
				}
				else {
					closeProgram = true;
				}
			}
		}

		@Override
		public void windowClosed(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowIconified(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowActivated(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
			// TODO Auto-generated method stub
			
		}
    	
    }
    
    class HarmonyWindowStateListener implements WindowStateListener {
		@Override
		public void windowStateChanged(WindowEvent e) {
			if(e.getNewState() == WindowEvent.WINDOW_ICONIFIED) {
				thisFrame.setState(Frame.NORMAL);
			}
		}
    	
    }
    
    class HarmonyWindowMoved implements ComponentListener {

		@Override
		public void componentResized(ComponentEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void componentMoved(ComponentEvent e) {
			if(thisFrame.getLocation()!=new Point(-10,-10)) {
				thisFrame.setLocation(-10, -10);
			}
		}

		@Override
		public void componentShown(ComponentEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void componentHidden(ComponentEvent e) {
			// TODO Auto-generated method stub
			
		}
		
    }
	
	
	private int xMoved = 0;
	private int yMoved = 0;
	private boolean switchPC(int i, int relPos)
    {
        if (isServer && currentPC!=null)
        {
            xMoved = 0;
            yMoved = 0;
            String currentIP = currentPC.pc_ip;
            boolean didSwitch = false;
            if(i==-1) {
            	didSwitch = true;
                currentPC = pcList.get(0);
            }
            else if (i == 1 && currentPC.up!=null && currentPC.up.isOnline)
            {
                didSwitch = true;
                currentPC = currentPC.up;
            }
            else if (i == 2 && currentPC.right != null && currentPC.right.isOnline)
            {
                didSwitch = true;
                currentPC = currentPC.right;
            }
            else if (i == 3 && currentPC.down != null && currentPC.down.isOnline)
            {
                didSwitch = true;
                currentPC = currentPC.down;
            }
            else if (i == 4 && currentPC.left != null && currentPC.left.isOnline)
            {
                didSwitch = true;
                currentPC = currentPC.left;
            }
            if (didSwitch)
            {
                if (!currentIP.equals("0"))
                {
                    sendMessage("t|", currentIP);
                }
                if (currentPC.pc_ip.equals("0"))
                {
                    server_BecomeMain();
                   
                    if (i==1)
                    {
                    	r.mouseMove((int)lerp(xMin,xMax,(((float)relPos)/100)), yMax-50);
                    }
                    else if (i==3)
                    {
                    	r.mouseMove((int)lerp(xMin,xMax,(((float)relPos)/100)), yMin+50);
                    }
                    else if(i==2)
                    {
                    	r.mouseMove(xMin+50, (int)lerp(yMin,yMax,((float)relPos)/100));
                    }
                    else if (i==4)
                    {
                    	r.mouseMove(xMax-50, (int)lerp(yMin,yMax,((float)relPos)/100));
                    }
                    
                }
                else
                {
                    if (isActivePC == true)
                    {
                    	server_BecomeSecondary();
                    }
                    sendMessage("s|" + i +"|"+ relPos, currentPC.pc_ip);

                }
            }
            return didSwitch;
        }
        return false;
    }

	private int isAlt = 0;
	private boolean isF4 = false;
	class HarmonyKeyboardListener implements KeyListener{

		@Override
		public void keyTyped(KeyEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if(isActivePC || !isServer) {
				
				return;
			}
			if(isServer) {
				if(e.getKeyCode()==KeyEvent.VK_ALT) {
					isAlt++;
				}
				else if(e.getKeyCode()==KeyEvent.VK_F4) {
					isF4 = true;
				}
			}
			sendMessage("kd|" + e.getKeyCode(),currentPC.pc_ip);
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if(isActivePC || !isServer) {
				return;
			}
			if(isServer) {
				if(e.getKeyCode()==KeyEvent.VK_ALT) {
					isAlt--;
				}
				else if(e.getKeyCode()==KeyEvent.VK_F4) {
					isF4 = false;
				}
			}
			sendMessage("ku|" + e.getKeyCode(),currentPC.pc_ip);
		}

		
	}
	
	class HarmonyMouseWheelListener implements MouseWheelListener{

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if(isActivePC || !isServer) {
				return;
			}
			sendMessage("m|" + e.getWheelRotation(), currentPC.pc_ip);
		}
		
	}
	
	private boolean m1 = false;
	private boolean m2 = false;
	private boolean m3 = false;
	class HarmonyMouseListener implements MouseListener {
		
		@Override
		public void mouseClicked(MouseEvent e) {
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if(isActivePC || !isServer) {
				return;
			}
			if(SwingUtilities.isLeftMouseButton(e)) {
				m1 = true;
				sendMessage("m|1d",currentPC.pc_ip);
			}
			if(SwingUtilities.isRightMouseButton(e)) {
				m2 = true;
				sendMessage("m|3d",currentPC.pc_ip);
			}
			if(SwingUtilities.isMiddleMouseButton(e)) {
				m3 = true;
				sendMessage("m|2d",currentPC.pc_ip);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if(isActivePC || !isServer) {
				return;
			}
			if(SwingUtilities.isLeftMouseButton(e) && m1) {
				m1 = false;
				sendMessage("m|1u",currentPC.pc_ip);
			}
			if(SwingUtilities.isRightMouseButton(e) && m2) {
				m2 = false;
				sendMessage("m|3u",currentPC.pc_ip);
			}
			if(SwingUtilities.isMiddleMouseButton(e) && m3) {
				m3 = false;
				sendMessage("m|2u",currentPC.pc_ip);
			}
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
	
	
	private boolean firstTimeADD = true;
	private void addNewPC(String s, String ip)
    {
        if (firstTimeADD)
        {
            for (int i = 0; i < server_buttons.length; i++)
            {
            	server_buttons[i].setEnabled(true);
            	server_buttons[i].addActionListener(new ActionListener() {
        		    @Override
        		    public void actionPerformed(ActionEvent e) {
        		    	Button_ADDPC((JButton)e.getSource());
        		    }
        		});
            }
        }
        else
        {
            enableSomeButtons();
        }
        if(s.length()>15) {
        	s = s.substring(0,12)+"...";
        }
        listModel.addElement(s);

        ipList.add(ip);
        firstTimeADD = false;
    }
	
	private ArrayList<PC> pcList = new ArrayList<PC>();
	private PC currentPC;
	private boolean firstClick = true;
	private void Button_ADDPC(JButton source)
    {
        if (listModel.getSize() > 2)
        {
        	String s = (String)listModel.get(2);
        	if(s.length()>7) {
        		s=s.substring(0,7);
        	}
        	source.setText(s);
        	source.setEnabled(false);

            if (ipList.get(0) != "0")
            {
                sendMessage("ok", ipList.get(0));
            }
            pcList.add(new PC((String)listModel.get(2), ipList.get(0)));
            if (currentPC == null)
            {
                currentPC = pcList.get(pcList.size()-1);
                currentPC.setHeartBeat();
            }
            ipList.remove(0);
            listModel.remove(2);
            for (int i = 0; i < server_buttons.length; i++)
            {
            	server_buttons[i].setEnabled(false);
                if (server_buttons[i] == source)
                {
                    pcList.get(pcList.size() - 1).buttonid = i;
                }
            }
            connectionItter();
            if (listModel.getSize() > 2)
            {
                enableSomeButtons();
            }
            else if(firstClick)
            {
                firstClick = false;
            }
        }
    }
	
	private void enableSomeButtons()
    {
        for (int i = 0; i < server_buttons.length; i++)
        {
            if (server_buttons[i].getText() != "[EMPTY]")
            {
                if ((i % 5 != 0) && server_buttons[i-1].getText().equals("[EMPTY]"))
                {
                	server_buttons[i-1].setEnabled(true);
                }
                if ((i % 5 != 4) && server_buttons[i+1].getText().equals("[EMPTY]"))
                {
                	server_buttons[i+1].setEnabled(true);
                }
                if ((i >= 5) && server_buttons[i-5].getText().equals("[EMPTY]"))
                {
                	server_buttons[i-5].setEnabled(true);
                }
                if ((i <= 19) && server_buttons[i+5].getText().equals("[EMPTY]"))
                {
                	server_buttons[i+5].setEnabled(true);
                }
            }
        }
    }
	
	private void connectionItter()
    {
        for(int i = 0; i < pcList.size(); i++)
        {
            for(int j = 0; j < pcList.size(); j++)
            {
                if (j != i)
                {
                    if((pcList.get(i).buttonid % 5 != 0) && pcList.get(i).buttonid == (pcList.get(j).buttonid - 1))
                    {
                        pcList.get(j).left = pcList.get(i);
                        pcList.get(i).right = pcList.get(j);
                    }
                    else if ((pcList.get(i).buttonid % 5 != 4) && pcList.get(i).buttonid == (pcList.get(j).buttonid + 1))
                    {
                        pcList.get(i).left = pcList.get(j);
                        pcList.get(j).right = pcList.get(i);
                    }
                    else if ((pcList.get(i).buttonid >= 5) && pcList.get(i).buttonid == (pcList.get(j).buttonid - 5))
                    {
                        pcList.get(i).down = pcList.get(j);
                        pcList.get(j).up = pcList.get(i);
                    }
                    else if ((pcList.get(i).buttonid <= 19) && pcList.get(i).buttonid == (pcList.get(j).buttonid + 5))
                    {
                        pcList.get(i).up = pcList.get(j);
                        pcList.get(j).down = pcList.get(i);
                    }
                }
            }
        }
    }

}
