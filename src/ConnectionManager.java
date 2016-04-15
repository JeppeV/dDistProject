import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Jeppe Vinberg on 15-04-2016.
 */
public class ConnectionManager implements Runnable {

    private ServerSocket serverSocket;

    public ConnectionManager(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        while(true){
            Socket socket = waitForConnectionFromClient(serverSocket);
            if(socket != null){
                //initThreads(socket);
            }else{
            }
        }
    }

    private Socket waitForConnectionFromClient(ServerSocket serverSocket) {
        Socket res = null;
        try {
            res = serverSocket.accept();
        } catch (IOException e) {
            // We return null on IOExceptions
        }
        return res;
    }
}
