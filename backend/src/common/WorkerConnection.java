package common;

import java.io.*;
import java.net.*;

public class WorkerConnection implements Serializable, AutoCloseable {
    private final WorkerInfo workerInfo;
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    
    public WorkerConnection(WorkerInfo workerInfo) throws IOException {
        this.workerInfo = workerInfo;
        try {
            this.socket = new Socket();
            // Set connection timeout and keep-alive
            socket.setSoTimeout(30000); // 30 seconds timeout
            socket.setKeepAlive(true);
            socket.connect(new InetSocketAddress(workerInfo.getHost(), workerInfo.getPort()), 5000);
            
            this.out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new IOException("Failed to establish worker connection: " + e.getMessage(), e);
        }
    }

    public WorkerInfo getWorkerInfo() {
        return workerInfo;
    }

    public ObjectOutputStream getOut() {
        return out;
    }

    public ObjectInputStream getIn() {
        return in;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void close() throws IOException {
        try {
            if (in != null) in.close();
        } finally {
            try {
                if (out != null) out.close();
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            }
        }
    }
}