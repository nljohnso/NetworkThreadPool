package cs455.scaling.server;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import cs455.scaling.task.AcceptTask;
import cs455.scaling.task.ServerTask;

public class Server {
	private static final int BUFFER_SIZE = 8192;
	private static Server instance = null;
	private String hostAddress;
	private int port;
	private int poolSize;
	private static boolean debug = true;

	private Server() throws IOException {
		this.hostAddress = Inet4Address.getLocalHost().getHostAddress();
	}

	public static Server getInstance() throws IOException {
		if (instance == null)
			instance = new Server();

		return instance;
	}

	/**
	 * Opens a SocketChannel between itself and the Client and also delegates
	 * tasks to threads within the thread pool as they come in.
	 * 
	 * 
	 */
	public void start() throws IOException {
		ThreadPoolManager pool = new ThreadPoolManager(getPoolSize());
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.socket().bind(
				new InetSocketAddress(getHostAddress(), getPort()));
		channel.configureBlocking(false);
		Selector socketSelector = Selector.open();
		channel.register(socketSelector, SelectionKey.OP_ACCEPT);

		while (true) {
			try {
				socketSelector.selectNow();
				Iterator<SelectionKey> selectedKeys = socketSelector
						.selectedKeys().iterator();

				while (selectedKeys.hasNext()) {

					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					if (key.isAcceptable()) {
						pool.execute(new AcceptTask(key, socketSelector));

						if (debug)
							System.out.println("Server: Accepted request.");
					} else if (key.isReadable()) {
						byte[] data = read(key);
						pool.execute(new ServerTask(data, key));
						if (debug)
							System.out
									.println("Server: Queued ServerTask request.");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads an 8kb packet send by the client through the channel.
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	private byte[] read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		readBuffer.clear();

		int length;
		try {
			length = socketChannel.read(readBuffer);
		} catch (IOException e) {
			if (debug)
				System.out.println("Reading problem, closing connection.");
			key.cancel();
			socketChannel.close();
			return null;
		}
		if (length == -1) {
			if (debug)
				System.out.println("Nothing was read from server.");
			socketChannel.close();
			key.cancel();
			return null;
		}
		readBuffer.flip();
		byte[] buff = new byte[BUFFER_SIZE];
		readBuffer.get(buff, 0, length);

		if (debug)
			System.out.println("Read data from buffer.");

		return buff;
	}

	/**
	 * 
	 * @return hostAddress
	 */
	public String getHostAddress() {
		return hostAddress;
	}

	/**
	 * 
	 * @return port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set port number
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 
	 * @return pool size
	 */
	public int getPoolSize() {
		return poolSize;
	}

	/**
	 * Set pool size
	 * 
	 * @param poolSize
	 */
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	/**
	 * Starts server and sets port and poolsize based on arguments.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Server server = Server.getInstance();
			server.setPort(Integer.parseInt(args[0]));
			server.setPoolSize(Integer.parseInt(args[1]));
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
