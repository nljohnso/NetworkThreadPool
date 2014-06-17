package cs455.scaling.task;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ServerTask implements Task {

	private static boolean debug = true;
	private SelectionKey key = null;
	private byte[] data = null;

	public ServerTask(byte[] data, SelectionKey key) {
		this.data = data;
		this.key = key;
	}

	/**
	 * Calculates the hash of the 8kb packet that has been received and writes
	 * it to the client.
	 */
	@Override
	public void run() {
		if (debug)
			System.out.println("Executing server task.");

		try {
			write(calculateHash(data));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the calculated hash to the client.
	 * 
	 * @param hash
	 * @throws IOException
	 */
	private void write(String hash) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buf = ByteBuffer.allocate(hash.length() + 4);
		int length = hash.getBytes().length;
		buf.putInt(length);
		buf.put(hash.getBytes());
		buf.flip();
		channel.write(buf);
		key.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * Calculates the hash using the SHA-1 algorithm
	 * 
	 * @return
	 */
	private String calculateHash(byte[] data) {

		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA1");
			byte[] hash = digest.digest(data);
			BigInteger hashInt = new BigInteger(1, hash);
			return hashInt.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return "Error calculateing hash.";
		}
	}
}
