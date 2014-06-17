package cs455.scaling.task;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class AcceptTask implements Task {

		private SelectionKey key = null;
		private Selector socketSelector = null;

		public AcceptTask(SelectionKey key, Selector socketSelector) {
			this.key = key;
			this.socketSelector = socketSelector;
		}

		@Override
		public void run() {
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
			SocketChannel socketChannel = null;
			try {
				socketChannel = serverSocketChannel.accept();
				socketChannel.configureBlocking(false);
				socketChannel.register(socketSelector, SelectionKey.OP_READ);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}

