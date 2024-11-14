package org.java.practice;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TicTacToePeer extends JFrame {


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            int portInfo;
            if (args.length == 0) {
                portInfo = Integer.parseInt(JOptionPane.showInputDialog("Enter staring port: "));
            } else {
                portInfo = Integer.parseInt(args[0]);
            }
            new TicTacToePeer(portInfo).setVisible(true);
        });
    }

    final PrintStream log;
    final int SIZE = 9;
    final private JButton[] buttons;
    private char myMark;
    private char opponentMark;
    private boolean myTurn = false;
    private String currentTitle;
    private final int serverPort;
    private ServerSocket selfServerSocket;
    private String opponentHost;
    private int opponentPort;

    public TicTacToePeer(int port) throws HeadlessException {
        buttons = new JButton[SIZE];
        log = System.out;
        this.serverPort = port;
        try {
            setupServerSocket();
            setupGUI();
            setupMenu();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "ERROR: " + ex.getMessage());
        }
        addWindowListener(new WindowAdapter() {
            /**
             * Invoked when a window has been closed.
             *
             * @param e : eventInfo
             */
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    log.println("Server socket shutdown .....");
                    selfServerSocket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                super.windowClosed(e);
            }
        });
    }


    private void setupMenu() {
        // Create the menu bar
        JMenuBar menuBar = new JMenuBar();
        // Create a menu
        JMenu menu = new JMenu("Actions");

        // Create a menu item
        JMenuItem inputDialogItem = new JMenuItem("Connect ..");
        inputDialogItem.addActionListener(e -> {
            // Show the input dialog
            new Thread(this::connectToPeer).start();
        });
        // Add the menu item to the menu
        menu.add(inputDialogItem);
        // Add the menu to the menu bar
        menuBar.add(menu);
        // Set the menu bar for the frame
        this.setJMenuBar(menuBar);
    }

    private void setupServerSocket() throws IOException {
        selfServerSocket = new ServerSocket(this.serverPort);
        log.println("DEBUG Start server in port " + this.serverPort);
        new Thread(this::receiveCommand).start();
    }

    final String CONNECT_ASK = "CONNECT:ASK";
    final String CONNECT_OK = "CONNECT:OK";
    final String CONNECT_MOVE_PREFIX = "MOVE:";

    private void connectToPeer() {
        String[] peerInfo = JOptionPane.showInputDialog("Enter peer host:port").split(":");
        this.opponentHost = peerInfo[0];
        this.opponentPort = Integer.parseInt(peerInfo[1]);
        try (Socket peerSocket = new Socket(this.opponentHost, this.opponentPort);
             DataOutputStream peerSocketOutput = new DataOutputStream(peerSocket.getOutputStream());
             DataInputStream peerSocketInput = new DataInputStream(peerSocket.getInputStream())
        ) {
            String askCommand = CONNECT_ASK + ":" + peerSocket.getLocalAddress().getHostAddress() + ":" + this.serverPort;
            peerSocketOutput.writeUTF(askCommand);
            peerSocketOutput.flush();
            String response = peerSocketInput.readUTF();
            if (response.equals(CONNECT_OK)) {
                updateMyMark('X');
                this.myTurn = true;
                updateFrameTitleSuffix("Your Turn");
            }
        } catch (IOException ex) {
            log.println("ERROR connectToPeer " + ex.getMessage());
        }
    }

    private void updateFrameTitleSuffix(String message) {
        setTitle(String.format("%s ! %s", currentTitle, message));
    }

    private void updateMyMark(char mark) {
        this.myMark = mark;
        this.opponentMark = this.myMark == 'X' ? 'O' : 'X';
        this.currentTitle = String.format("Tic Tac Toe - Your Mark is %c",  this.myMark);
        setTitle(this.currentTitle);
    }

    private void toggleTurn() {
        this.myTurn = !this.myTurn;
        updateFrameTitleSuffix( this.myTurn ? "Your Turn": "Wait for Your Turn");
    }
    private void receiveCommand() {
        try {
            log.println("DEBUG wait receiveCommand ...");
            while (true) {
                Socket client = selfServerSocket.accept();
                try ( DataInputStream socketInput = new DataInputStream(client.getInputStream());
                      DataOutputStream socketOutput = new DataOutputStream(client.getOutputStream()))
                {
                    String command = socketInput.readUTF();
                    log.println("DEBUG receiveCommand: " + command);
                    if (command.contains(CONNECT_ASK)) {
                        String[] peerInfo = command.split(":");
                        resetGame();
                        log.println("INFO : I'm a SERVER: opponentHost: " + peerInfo[2] + " opponentPort : " + peerInfo[3]);
                        this.opponentHost = peerInfo[2];
                        this.opponentPort = Integer.parseInt(peerInfo[3]);
                        updateMyMark('O');
                        this.myTurn = false;
                        updateFrameTitleSuffix("Wait for Your Turn");
                        socketOutput.writeUTF(CONNECT_OK);
                        log.println("INFO : My Mark  " + this.myMark + " WAIT ");
                    } else {
                        int position = Integer.parseInt(command.split(":")[1]);
                        log.println("INFO : Receive Moves " + position);
                        SwingUtilities.invokeLater(() -> {
                            buttons[position].setText(String.valueOf(opponentMark));
                            buttons[position].setEnabled(false);
                            checkForWinner("Try it again!");
                            toggleTurn();
                        });
                    }
                }

            }
        } catch (IOException e) {
            log.println("ERROR: " + e.getMessage());
        }
    }

    private void sendMoves(int pos) throws IOException {
        String command = CONNECT_MOVE_PREFIX + pos;
        try (Socket peerSocket = new Socket(this.opponentHost, this.opponentPort);
             DataOutputStream peerSocketOutput = new DataOutputStream(peerSocket.getOutputStream())) {
            peerSocketOutput.writeUTF(command);
            peerSocketOutput.flush();
        }
    }

    private void setupGUI() {
        setTitle(String.format("Tic Tac Toe - Player Open port %d", selfServerSocket.getLocalPort()));
        setSize(300, 300);
        setLayout(new GridLayout(3, 3));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        for (int i = 0; i < SIZE; i++) {
            buttons[i] = new JButton("");
            buttons[i].setFont(new Font("Arial", Font.PLAIN, 60));
            buttons[i].setFocusPainted(false);
            buttons[i].addActionListener(new ButtonClickListener(i));
            add(buttons[i]);
        }
        setLocationRelativeTo(null);
    }

    private void resetGame() {
        for (int i = 0; i < SIZE; i++) {
            buttons[i].setText("");
            buttons[i].setEnabled(true);
        }
    }

    private void checkForWinner(String message) {
        int[][] winPositions = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
                {0, 4, 8}, {2, 4, 6}            // diagonals
        };
        for (int[] pos : winPositions) {
            JButton b1 = buttons[pos[0]];
            JButton b2 = buttons[pos[1]];
            JButton b3 = buttons[pos[2]];
            if (b1.getText().equals(b2.getText()) && b2.getText().equals(b3.getText())
                    && !b1.getText().isEmpty() && !b3.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Player " + b1.getText() + " wins! " + message);
                resetGame();
                return;
            }
        }
        boolean draw = true;
        for (JButton button : buttons) {
            if (button.getText().isEmpty()) {
                draw = false;
                break;
            }
        }

        if (draw) {
            JOptionPane.showMessageDialog(this, "It's a draw!");
            resetGame();
        }
    }

    private class ButtonClickListener implements ActionListener {
        private final int index;

        public ButtonClickListener(int index) {
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JButton me = buttons[index];
            if (myTurn && me.getText().isEmpty()) {
                me.setText(String.valueOf(myMark));
                me.setEnabled(false);
                try {
                    sendMoves(index);
                    checkForWinner("Congratulation!");
                    toggleTurn();
                } catch (IOException ex) {
                    log.println("ERROR: actionPerformed " + ex.getMessage());
                }
            }
        }
    }

}
