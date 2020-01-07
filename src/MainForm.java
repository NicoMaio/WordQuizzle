import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MainForm {
    private Operation op;
    private JPanel MainPanel;
    private JButton SignIn;
    private JButton signUpButton;
    private JTabbedPane tabbedPane1;
    private JTextField usernameTextField;
    private JPasswordField passwordPasswordField;
    private JPasswordField passwordPasswordField1;
    private JTextArea registrazioneTextArea;
    private JTextField usernameTextField1;
    private JPasswordField passwordField1;
    private JTextArea loginTextArea;
    private JPanel SignUpPanel;
    private JPanel SignInPanel;

    public MainForm(Operation op) throws Exception {
        this.op = op;
        try {
            signUpButton.addActionListener(new ActionListener() {
                                               @Override
                                               public void actionPerformed(ActionEvent actionEvent) {
                                                   registrazione(usernameTextField.getText(), passwordPasswordField.getPassword(), passwordPasswordField1.getPassword());

                                               }
                                           }
            );

            SignIn.addActionListener(event -> {
                try {
                    String result = op.login(usernameTextField1.getText(), passwordField1.getPassword());

                    switch (result) {
                        case "-2":
                            loginTextArea.removeAll();
                            loginTextArea.append("Username errato");
                            break;
                        case "-1":
                            loginTextArea.removeAll();
                            loginTextArea.append("Password errata");

                            break;
                        case "0":
                            loginTextArea.removeAll();
                            loginTextArea.append("Login già effettuato");
                            break;
                        case "1":
                            loginTextArea.removeAll();
                            loginTextArea.append("Login effettuato con successo");
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e){

        }
    }

    private static int DEFAULT_PORT = 13200;
    private static int PORT_FOR_RSERVICE = 9999;
    private static String host;

    public static void main(String[] args)throws Exception{
        if(args.length == 0){
            System.err.println("Usage: java MainClassClient host port");
            return;
        }

        host = args[0];

        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (RuntimeException e) {
            port = DEFAULT_PORT;
        }

        Registry reg = LocateRegistry.getRegistry(host,PORT_FOR_RSERVICE);

        RemoteRegistration registration = (RemoteRegistration) reg.lookup(RemoteRegistration.SERVICE_NAME);

       /* int x =registration.registra("Michele","Superman");

        registration.registra("Francesco","Illegale");

        if(x == -1)System.out.println("Utente Michele già registrato");
        */

        Registry registry = LocateRegistry.getRegistry(host,5000);
        ServerInterface server = (ServerInterface) registry.lookup(ServerInterface.SERVICE_NAME);

        NotifyEventInterface callbackObj = new NotifyEventImpl();
        NotifyEventInterface stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj,0);
        //server.registerForCallback("Michele",stub);

        SocketAddress address = new InetSocketAddress(host,port);
        SocketChannel client = SocketChannel.open(address);
        client.configureBlocking(true);

        Operation op = new Operation(registration,server,client);
        JFrame frame = new JFrame("Word Quizzle");
        frame.setContentPane(new MainForm(op).MainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
    private void createUIComponents() {
        // TODO: place custom component creation code here

    }

    private void registrazione(String e1,char[] e2,char[] e3){
        try {
            if(e2 == e3) {
                int x = op.registra(e1, e2);
                registrazioneTextArea.removeAll();
                if(x == -1) {
                    registrazioneTextArea.append("Username non disponibile");
                } else {
                    registrazioneTextArea.append("Registrazione avvenuta con successo");
                }
            } else {
                registrazioneTextArea.removeAll();
                registrazioneTextArea.append("Le due password inserite sono diverse");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
