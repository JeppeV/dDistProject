import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class DistributedTextEditor extends JFrame {

    private static final int PORT_NUMBER = 40103;
    private JTextArea area1 = new JTextArea(20, 120);
    private JTextField ipaddress = new JTextField("Insert IP address here");
    private JTextField portNumber = new JTextField("Insert port number here");

    private DisconnectHandler disconnectHandler;
    private ServerSocket serverSocket;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;

    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;
    private DocumentEventCapturer dec;

    public DistributedTextEditor() {
        ConcurrentHashMap<MyTextEvent,MyTextEvent> localBuffer = new ConcurrentHashMap<>();
        dec = new DocumentEventCapturer( getLocalHostAddress(), localBuffer);
        area1.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ((AbstractDocument) area1.getDocument()).setDocumentFilter(dec);

        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(area1,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1, BorderLayout.CENTER);

        content.add(ipaddress, BorderLayout.CENTER);
        content.add(portNumber, BorderLayout.CENTER);

        area1.hashCode();

        JMenuBar JMB = new JMenuBar();
        setJMenuBar(JMB);
        JMenu file = new JMenu("File");
        JMenu edit = new JMenu("Edit");
        JMB.add(file);
        JMB.add(edit);

        file.add(Listen);
        file.add(Connect);
        Disconnect.setEnabled(false);
        file.add(Disconnect);
        file.addSeparator();
        file.add(Save);
        file.add(SaveAs);
        file.add(Quit);

        edit.add(Copy);
        edit.add(Paste);
        edit.getItem(0).setText("Copy");
        edit.getItem(1).setText("Paste");

        Save.setEnabled(false);
        SaveAs.setEnabled(false);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        area1.addKeyListener(k1);
        setTitle("Disconnected");
        setVisible(true);

        /*
            incomingEvents is a queue where incoming text events are stored.
            A reference to the queue is delegated to instances of TextEventReceiver
            Each TextEventReceiver then puts incoming text events onto the queue,
            and the EventReplayer takes elements from this list and replays them in the second text area.
         */
        incomingEvents = new LinkedBlockingQueue<>();

        /*
            The EventReplayer runnable keeps running while the peer lives.
            It will only terminate once the program exits.
         */
        EventReplayer eventReplayer = new EventReplayer(incomingEvents, area1, dec, localBuffer);
        Thread ert = new Thread(eventReplayer);
        ert.start();


    }

    /**
     * This method is called if this peer is supposed to act as a server.
     */
    private DisconnectHandler initServerThreads() {
        //clear the document event capturer queue
        dec.clear();
        ServerConnectionManager connectionManager = new ServerConnectionManager(serverSocket, dec, area1);
        Thread connectionManagerThread = new Thread(connectionManager);
        connectionManagerThread.start();
        return connectionManager;
    }

    /**
     * This method is called if this peer is supposed to act as a client.
     * Initiates threads to handle sending and receiving text events to and from the server.
     *
     * @param socket the socket representing the connection to the server
     */
    private DisconnectHandler initClientThreads(Socket socket) {
        //clear the document event capturer queue
        dec.clear();
        TextEventSender sender = new TextEventSender(socket, dec.getEventHistory());
        TextEventReceiver receiver = new TextEventReceiver(socket, incomingEvents, sender);
        Thread senderThread = new Thread(sender);
        Thread receiverThread = new Thread(receiver);
        senderThread.start();
        receiverThread.start();
        return sender;
    }

    private KeyListener k1 = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            changed = true;
            Save.setEnabled(true);
            SaveAs.setEnabled(true);
        }
    };


    /**
     * When the menu item "Listen" is clicked, we begin listening for incoming connections on the
     * port indicated by PORT_NUMBER. To setup for acting like a server
     */
    Action Listen = new AbstractAction("Listen") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            clearTextArea();

            String address = getLocalHostAddress();
            serverSocket = registerOnPort(PORT_NUMBER);
            if (serverSocket != null) {
                setTitle("I'm listening on: " + address + ":" + PORT_NUMBER);
                disconnectHandler = initServerThreads();
                System.out.println("I'm server");
                Socket socket = connectToServer(address, "" + PORT_NUMBER);
                if (socket != null) {
                    initClientThreads(socket);
                    System.out.println("I'm client");
                }
                //disable irrelevant actions in order to avoid unexpected behaviour
                Listen.setEnabled(false);
                Connect.setEnabled(false);
                Disconnect.setEnabled(true);

                changed = false;
                Save.setEnabled(false);
                SaveAs.setEnabled(false);
            } else {
                setTitle("Failed to begin listening");
            }

        }
    };


    private ServerSocket registerOnPort(int portNumber) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.err.println("Cannot open server socket on port number" + portNumber);
            System.err.println(e);
            System.exit(-1);
        }
        return serverSocket;
    }

    private String getLocalHostAddress() {
        String address = null;
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            address = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Cannot resolve Internet address of the local host");
            System.err.println(e);
            System.exit(-1);
        }
        return address;
    }

    private String getIPAddress() {
        return ipaddress.getText();
    }

    private String getPortNumber() {
        return "40103";
        //return portNumber.getText();
    }

    private void clearTextArea() {
        dec.disable();
        area1.setText("");
        dec.enable();
    }


    /**
     * When the menu item "Connect" is clicked, the peer acts as a client and attempts to connect the the host
     * indicated by the ipaddress and portNumber fields
     */
    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            clearTextArea();
            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);

            // Connecting to the server
            setTitle("Attempting to connect to: " + getIPAddress() + ":" + getPortNumber() + "...");
            Socket socket = connectToServer(getIPAddress(), getPortNumber());
            if (socket != null) {
                disconnectHandler = initClientThreads(socket);
                System.out.println("I'm client");
                setTitle("Connection good!");
                Listen.setEnabled(false);
                Connect.setEnabled(false);
                Disconnect.setEnabled(true);
            } else {
                setTitle("Connection failed");
            }
        }
    };

    private Socket connectToServer(String serverAddress, String portNumber) {
        Socket socket = null;
        try {
            socket = new Socket(serverAddress, Integer.parseInt(portNumber));
        } catch (IOException e) {
            e.printStackTrace();
            // TODO
        }
        return socket;
    }

    /**
     * When the menu item "Disconnect" is clicked, depending on whether this peer is a server or a client,
     * this method initiates the shutdown procedure, using a ShutDownTextEvent.
     * The result is that all relevant threads for this peer are shut down.
     */
    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            setTitle("Disconnected");
            try {
                disconnectHandler.disconnect();
                disconnectHandler = null;
                deregisterOnPort();
            } catch (InterruptedException ie) {
                //TODO
            }

            Listen.setEnabled(true);
            Connect.setEnabled(true);
            Disconnect.setEnabled(false);
        }
    };

    private void deregisterOnPort() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }

    Action Save = new AbstractAction("Save") {
        public void actionPerformed(ActionEvent e) {
            if (!currentFile.equals("Untitled"))
                saveFile(currentFile);
            else
                saveFileAs();
        }
    };

    Action SaveAs = new AbstractAction("Save as...") {
        public void actionPerformed(ActionEvent e) {
            saveFileAs();
        }
    };

    Action Quit = new AbstractAction("Quit") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            System.exit(0);
        }
    };

    ActionMap m = area1.getActionMap();

    Action Copy = m.get(DefaultEditorKit.copyAction);
    Action Paste = m.get(DefaultEditorKit.pasteAction);

    private void saveFileAs() {
        if (dialog.showSaveDialog(null) == JFileChooser.APPROVE_OPTION)
            saveFile(dialog.getSelectedFile().getAbsolutePath());
    }

    private void saveOld() {
        if (changed) {
            if (JOptionPane.showConfirmDialog(this, "Would you like to save " + currentFile + " ?", "Save", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                saveFile(currentFile);
        }
    }

    private void saveFile(String fileName) {
        try {
            FileWriter w = new FileWriter(fileName);
            area1.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            Save.setEnabled(false);
        } catch (IOException e) {
        }
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

}
