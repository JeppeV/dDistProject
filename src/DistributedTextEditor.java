import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;

public class DistributedTextEditor extends JFrame {

    private static final int DEFAULT_PORT_NUMBER = 40103;
    private JTextArea textArea = new JTextArea(20, 120);
    private JTextField ipaddress = new JTextField("Insert IP address to connect to here");
    private JTextField remotePortNumber = new JTextField("" + DEFAULT_PORT_NUMBER);
    private JTextField localPortNumber = new JTextField("" + DEFAULT_PORT_NUMBER);

    private DisconnectHandler disconnectHandler;
    private LinkedBlockingQueue<MyTextEvent> incomingEvents;
    private LamportClock lamportClock;

    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;
    private DocumentEventCapturer dec;

    public DistributedTextEditor() {

        //init();
        clearTextFieldsWhenClicked();

        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(textArea,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1, BorderLayout.CENTER);

        content.add(ipaddress, BorderLayout.CENTER);
        content.add(remotePortNumber, BorderLayout.CENTER);
        content.add(localPortNumber, BorderLayout.CENTER);


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
        textArea.addKeyListener(k1);
        setTitle("Disconnected");
        setVisible(true);

    }

    /**
     * Initialize stuff, that is not GUI related.
     * A buffer that keeps track of local events
     * A DocumentEventCapturer to capture local events when entered on the keyboard.
     * A queue whereon to put new incoming events
     * An EventReplayer to replay remote events locally.

    private void init() {
        lamportClock = new LamportClock();
        dec = new DocumentEventCapturer(lamportClock);
        ((AbstractDocument) textArea.getDocument()).setDocumentFilter(dec);
        incomingEvents = new LinkedBlockingQueue<>();
        EventReplayer eventReplayer = new EventReplayer(incomingEvents, textArea, dec);
        Utility.startRunnable(eventReplayer);
    }
     */

    /**
     * When the menu item "Listen" is clicked, we begin listening for incoming connections on the
     * port indicated by DEFAULT_PORT_NUMBER. A thread running a ConnectionManager is started
     * which handles all actions relevant to acting as a server.
     * Afterwards, this peer connects to its own server process and
     * in that way becomes a client as well.
     */


    private DisconnectHandler startAsRoot() {
        String localAddress = Utility.getLocalHostAddress();
        int localPort = getLocalPortNumber();
        ConnectionManager connectionManager = new ConnectionManager(localPort, textArea);
        Utility.startRunnable(connectionManager);
        setTitle("I'm listening on: " + localAddress + ":" + localPort);
        System.out.println("I am Root");
        return connectionManager;
    }

    private DisconnectHandler startAsPeer(String IPAddress, int portNumber) {
        String localAddress = Utility.getLocalHostAddress();
        int localPort = getLocalPortNumber();
        ConnectionManager connectionManager = new ConnectionManager(localPort, textArea, IPAddress, portNumber);
        Utility.startRunnable(connectionManager);
        setTitle("I'm a peer and I'm listening on: " + localAddress + ":" + localPort);
        System.out.println("I am a Peer");
        return connectionManager;
    }




    private String getIPAddress() {
        return ipaddress.getText();
    }

    private int getLocalPortNumber() {
        return Integer.parseInt(localPortNumber.getText());
    }

    private int getRemotePortNumber() {
        return Integer.parseInt(remotePortNumber.getText());
    }

    private void clearTextArea() {
        textArea.setText("");
    }

    Action Listen = new AbstractAction("Listen") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            clearTextArea();
            disconnectHandler = startAsRoot();
            if (disconnectHandler != null) {
                setMenuItemsConfigurationToConnected();
            }
            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);
        }
    };

    /**
     * When the menu item "Connect" is clicked, the peer acts as a client and attempts to connect the the host
     * indicated by the ipaddress and localPortNumber fields
     */
    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            clearTextArea();
            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);

            // Connecting to the server
            setTitle("Attempting to connect to: " + getIPAddress() + ":" + getRemotePortNumber() + "...");
            disconnectHandler = startAsPeer(getIPAddress(), getRemotePortNumber());
            if (disconnectHandler != null) {
                setMenuItemsConfigurationToConnected();
            }
        }
    };


    /**
     * When pressing disconnect, the disconnect handler for this peer is used in order to
     * disconnect from the network.
     */
    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            setTitle("Disconnected");
            try {
                disconnectHandler.disconnect();
                disconnectHandler = null;
            } catch (InterruptedException ie) {
                //TODO
            }
            setMenuItemsConfigurationToDisconnected();
        }
    };



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

    private void setMenuItemsConfigurationToConnected() {
        Listen.setEnabled(false);
        Connect.setEnabled(false);
        Disconnect.setEnabled(true);
    }

    private void setMenuItemsConfigurationToDisconnected() {
        Listen.setEnabled(true);
        Connect.setEnabled(true);
        Disconnect.setEnabled(false);
    }

    private KeyListener k1 = new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
            changed = true;
            Save.setEnabled(true);
            SaveAs.setEnabled(true);
        }
    };

    ActionMap m = textArea.getActionMap();

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
            textArea.write(w);
            w.close();
            currentFile = fileName;
            changed = false;
            Save.setEnabled(false);
        } catch (IOException e) {
        }
    }

    private void clearTextFieldsWhenClicked() {
        ipaddress.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ipaddress.setText("");
                ipaddress.removeMouseListener(this);
            }
        });

        localPortNumber.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                localPortNumber.setText("");
                localPortNumber.removeMouseListener(this);

            }
        });

        remotePortNumber.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                remotePortNumber.setText("");
                remotePortNumber.removeMouseListener(this);
            }
        });
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

}
