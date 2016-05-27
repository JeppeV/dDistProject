import main.ConnectionManager;
import main.Utility;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.IOException;

public class DistributedTextEditor extends JFrame {

    private static final int DEFAULT_PORT_NUMBER = 40103;

    private JTextArea textArea = new JTextArea(20, 120);
    private JTextField IPAddressField = new JTextField("Insert IP address to connect to here");
    private JTextField remotePortNumberField = new JTextField("" + DEFAULT_PORT_NUMBER);
    private JTextField localPortNumberField = new JTextField("" + DEFAULT_PORT_NUMBER);

    private ConnectionManager connectionManager;

    private JFileChooser dialog =
            new JFileChooser(System.getProperty("user.dir"));

    private String currentFile = "Untitled";
    private boolean changed = false;

    public DistributedTextEditor() {

        addTextFieldPlaceholderListeners();

        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        Container content = getContentPane();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JScrollPane scroll1 =
                new JScrollPane(textArea,
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                        JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        content.add(scroll1, BorderLayout.CENTER);

        content.add(IPAddressField, BorderLayout.CENTER);
        content.add(remotePortNumberField, BorderLayout.CENTER);
        content.add(localPortNumberField, BorderLayout.CENTER);


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
     * This method is used when the 'Listen' event is fired. It sets up this peer as root of the connection tree.
     * @return the ConnectionManager instance, used for disconnecting
     */
    private ConnectionManager startAsRoot() {
        String localAddress = Utility.getLocalHostAddress();
        int localPort = getLocalPortNumber();
        ConnectionManager connectionManager = new ConnectionManager(localPort, textArea);
        Utility.startRunnables(connectionManager);
        setTitle("I'm listening on: " + localAddress + ":" + localPort);
        return connectionManager;
    }

    /**
     * This method is used when the 'Connect' event is fired. It sets up this peer as leaf of in the connection tree.
     * @param IPAddress the IPAddress of the parent peer we wish to connect to
     * @param portNumber the portNumber of the parent peer we wish to connect to
     * @return the ConnectionManager instance, used for disconnecting
     */
    private ConnectionManager startAsPeer(String IPAddress, int portNumber) {
        String localAddress = Utility.getLocalHostAddress();
        int localPort = getLocalPortNumber();
        ConnectionManager connectionManager = new ConnectionManager(localPort, textArea, IPAddress, portNumber);
        Utility.startRunnables(connectionManager);
        setTitle("I'm a peer and I'm listening on: " + localAddress + ":" + localPort);
        return connectionManager;
    }


    private String getIPAddress() {
        return IPAddressField.getText();
    }

    private int getLocalPortNumber() {
        return Integer.parseInt(localPortNumberField.getText());
    }

    private int getRemotePortNumber() {
        return Integer.parseInt(remotePortNumberField.getText());
    }

    private void clearTextArea() {
        textArea.setText("");
    }

    /**
     * This action is performed when the 'Listen' menu item is clicked.
     * When that happens, this peer is added as root in the connection tree.
     */
    Action Listen = new AbstractAction("Listen") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            clearTextArea();
            connectionManager = startAsRoot();
            setMenuItemsConfigurationToConnected();
            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);
        }
    };

    /**
     * This action is performed when the 'Connect' menu item is clicked.
     * When that happens, this peer is added as a leaf in the connection tree.
     */
    Action Connect = new AbstractAction("Connect") {
        public void actionPerformed(ActionEvent e) {
            saveOld();
            clearTextArea();
            changed = false;
            Save.setEnabled(false);
            SaveAs.setEnabled(false);

            setTitle("Attempting to connect to: " + getIPAddress() + ":" + getRemotePortNumber() + "...");
            connectionManager = startAsPeer(getIPAddress(), getRemotePortNumber());
            setMenuItemsConfigurationToConnected();

        }
    };


    /**
     * This action is performed when the 'Disconnect' menu item is clicked.
     * When that happens, this peer is removed from the connection tree.
     */
    Action Disconnect = new AbstractAction("Disconnect") {
        public void actionPerformed(ActionEvent e) {
            setTitle("Disconnected");
            try {
                connectionManager.disconnect();
                connectionManager = null;
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

    private void addTextFieldPlaceholderListeners() {
        IPAddressField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                IPAddressField.setText("");
                IPAddressField.removeMouseListener(this);
            }
        });

        localPortNumberField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                localPortNumberField.setText("");
                localPortNumberField.removeMouseListener(this);

            }
        });

        remotePortNumberField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                remotePortNumberField.setText("");
                remotePortNumberField.removeMouseListener(this);
            }
        });
    }

    public static void main(String[] arg) {
        new DistributedTextEditor();
    }

}
