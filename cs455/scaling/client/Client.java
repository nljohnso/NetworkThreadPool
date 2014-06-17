package cs455.scaling.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class Client {

	private static final int BUFFER_SIZE = 8192;
	private static boolean debug = false;
	private String serverHost;
	private int serverPort;
	public int clientPort;
	private int messageRate;
	private long pastTime = Long.MAX_VALUE;

	private LinkedList<String> hashCodes = null;
	public DataOutputStream dout;
	private Selector selector;
	private SocketChannel channel = null;

	public Client(String serverHost, int serverPort, int messageRate)
			throws IOException, InterruptedException {
		setServerHost(serverHost);
		setServerPort(serverPort);
		setMessageRate(messageRate);
		hashCodes = new LinkedList<String>();
		this.selector = this.initSelector();
	}

	/**
	 * Initializes the selector.
	 * 
	 * @return
	 * @throws IOException
	 */
	private Selector initSelector() throws IOException {
		return SelectorProvider.provider().openSelector();
	}

	/**
	 * Connects to a SocketChannel between itself and the Server and reads and
	 * writes data coming from and going to the server.
	 */
	public void run() {
		SocketChannel channel;
		try {
			selector = Selector.open();
			channel = SocketChannel.open();
			channel.configureBlocking(false);

			channel.connect(new InetSocketAddress(getServerHost(),
					getServerPort()));
			channel.register(selector, SelectionKey.OP_CONNECT);

			while (true) {

				selector.selectNow();

				Iterator<SelectionKey> keys = selector.selectedKeys()
						.iterator();

				if (!keys.hasNext()) {
					channel.register(selector, SelectionKey.OP_WRITE);
				}

				while (keys.hasNext()) {
					SelectionKey key = keys.next();

					keys.remove();

					if (!key.isValid())
						continue;

					if (key.isConnectable()) {
						connect(key);
						if (debug)
							System.out
									.println("Client: Connected to the server.");
					}

					if (key.isWritable()) {
						if (timeToWrite()) {
							write(key);

							SimpleDateFormat dateFormat = new SimpleDateFormat(
									"yyyy/MM/dd HH:mm:ss.SSS");
							Calendar cal = Calendar.getInstance();
							System.out.println("Sent packet to server at: "
									+ dateFormat.format(cal.getTime()));
						}
						channel.register(selector, SelectionKey.OP_READ);
					}

					if (key.isReadable()) {
						if (debug)
							System.out.println("Client: Channel is readable.");

						read(key);

						if (debug)
							System.out.println("Client: Read from server.");

						channel.register(selector, SelectionKey.OP_WRITE);
					}
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			close();
		}
	}

	/**
	 * Closes the selector
	 */
	private void close() {
		try {
			selector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the incoming hash codes coming from the server.
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	private byte[] read(SelectionKey key) throws IOException {
		channel = (SocketChannel) key.channel();

		int lengthToRead = 0;

		ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
		lengthBuffer.rewind();
		lengthBuffer.clear();

		channel.read(lengthBuffer);
		lengthToRead = lengthBuffer.getInt(0);

		ByteBuffer readBuffer = ByteBuffer.allocate(lengthToRead);
		readBuffer.rewind();
		readBuffer.clear();

		try {
			channel.read(readBuffer);
		} catch (IOException e) {
			if (debug)
				System.out.println("Reading problem, closing connection.");
			key.cancel();
			channel.close();
			return null;
		}
		if (lengthToRead == -1) {
			if (debug)
				System.out.println("Nothing was read from server.");
			channel.close();
			key.cancel();
			return null;
		}

		readBuffer.flip();
		byte[] buff = new byte[lengthToRead];
		readBuffer.get(buff, 0, lengthToRead);

		String hash = new String(buff);

		System.out.println("Removing " + hash + " from hash list.");
		System.out.println("****************************");
		removeFromHashCodes(new String(buff));

		return buff;
	}

	/**
	 * Generates 8kb packets, stores their hash code, and sends the packet to
	 * the server.
	 * 
	 * @param key
	 * @return
	 * @throws IOException
	 */
	private byte[] write(SelectionKey key) throws IOException {
		byte[] randByteArray = generateByteArray();
		channel = (SocketChannel) key.channel();
		channel.write(ByteBuffer.wrap(randByteArray));

		try {
			String hash = SHA1FromBytes(randByteArray);
			System.out.println("Adding   " + hash + " to hash list.");
			hashCodes.add(hash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return randByteArray;
	}

	/**
	 * Finish connecting to the channel.
	 * 
	 * @param key
	 * @throws IOException
	 */
	private void connect(SelectionKey key) throws IOException {
		channel = (SocketChannel) key.channel();
		if (channel.isConnectionPending()) {
			channel.finishConnect();
		}
		channel.configureBlocking(false);

		channel.register(selector, SelectionKey.OP_WRITE);

	}

	/**
	 * Checks if enough time has elapsed in order to send another packet.
	 * 
	 * @return
	 */
	private boolean timeToWrite() {

		Calendar currentTime = Calendar.getInstance();
		long now = currentTime.getTime().getTime();

		if (pastTime == Long.MAX_VALUE) {
			pastTime = now;
			return true;
		} else if ((now - pastTime) < 1000 / getMessageRate()) {
			return false;
		}

		pastTime = now;
		return true;
	}

	/**
	 * Removes the provided hashcode from the list of expected hashcodes (i.e.
	 * previously sent messages).
	 * 
	 * @param hashCode
	 *            - Hashcode to find and remove.
	 */
	public void removeFromHashCodes(String hashCode) {
		if (hashCodes.contains(hashCode)) {
			hashCodes.remove(hashCode);
		} else {
			System.out.println("LinkedList does not contain hash: " + hashCode);
			System.exit(0);
		}
	}

	/**
	 * Creates random byte array message.
	 * 
	 * @return Randomly created byte array.
	 */
	private byte[] generateByteArray() {
		Random rand = new Random();
		byte[] bytes = new byte[BUFFER_SIZE];
		rand.nextBytes(bytes);

		return bytes;
	}

	/**
	 * Calculate the hashcode from a byte array.
	 * 
	 * @param data
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA1");
		byte[] hash = digest.digest(data);
		BigInteger hashInt = new BigInteger(1, hash);

		return hashInt.toString(16);
	}

	/**
	 * 
	 * @return serverhost
	 */
	public String getServerHost() {
		return serverHost;
	}

	/**
	 * Set the serverhost
	 * 
	 * @param serverHost
	 */
	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	/**
	 * 
	 * @return serverport
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * Set the serverport
	 * 
	 * @param serverPort
	 */
	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * 
	 * @return messagerate
	 */
	public int getMessageRate() {
		return messageRate;

	}

	/**
	 * Set the message rate
	 * 
	 * @param messageRate
	 */
	public void setMessageRate(int messageRate) {
		this.messageRate = messageRate;
	}

	/**
	 * Creates a client and sets the serverhost, serverport, and message rate.
	 * 
	 * @param args
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static void main(String[] args) throws InterruptedException,
			IOException, NoSuchAlgorithmException {
		try {
			Client client = new Client(args[0], Integer.parseInt(args[1]),
					Integer.parseInt(args[2]));
			client.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
