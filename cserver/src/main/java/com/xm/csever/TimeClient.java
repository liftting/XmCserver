package com.xm.csever;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by wm on 15/7/1.
 */
public class TimeClient {

    public static void main(String[] args) {
        new Thread(new MultiMainTimeClient("127.0.0.1", 8080)).start();
    }

    private static class MultiMainTimeClient implements Runnable {

        private String host;
        private int port;
        private Selector mSelector;
        private SocketChannel mSocketChannel;
        private boolean isStop;

        public MultiMainTimeClient(String host, int port) {

            this.host = host == null ? "127.0.0.1" : host;

            this.port = port;

            try {
                mSelector = Selector.open();
                mSocketChannel = SocketChannel.open();
                mSocketChannel.configureBlocking(false);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {

            try {
                doConnect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!isStop) {
                try {
                    mSelector.select(1000);
                    Set<SelectionKey> selectionKeys = mSelector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    SelectionKey key;
                    while (iterator.hasNext()) {
                        key = iterator.next();
                        iterator.remove();
                        try {
                            handleInput(key);
                        } catch (IOException e) {
                            if (key != null) {
                                key.cancel();
                                if (key.channel() != null) {
                                    key.channel().close();
                                }
                            }
                        }

                    }

                } catch (IOException e) {

                }
            }


            if (mSelector != null) {
                try {
                    mSelector.close();
                } catch (IOException e) {

                }
            }


        }

        private void handleInput(SelectionKey key) throws IOException {
            if (key.isValid()) {

                SocketChannel socketChannel = (SocketChannel) key.channel();

                if (key.isConnectable()) {
                    if (socketChannel.finishConnect()) {
                        socketChannel.register(mSelector, SelectionKey.OP_READ);
                        doWrite();
                    } else {
                        // exit 链接失败
                        System.exit(1);
                    }
                } else if (key.isReadable()) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    //读到bufer
                    int readBytes = socketChannel.read(byteBuffer);
                    if (readBytes > 0) {
                        // 这里，没有add时，数据没有显示
                        byteBuffer.flip();
                        // this place
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);

                        String data = new String(bytes, "utf-8");
                        String body = "from Server info : length is " + bytes.length + " and info body is :" + data;
                        System.out.println(body);
                        this.isStop = true;
                    } else if (readBytes < 0) {
                        key.cancel();
                        socketChannel.close();
                    }
                }
            }
        }

        private void doConnect() throws IOException {
            if (mSocketChannel.connect(new InetSocketAddress(host, port))) {
                // success connect
                mSocketChannel.register(mSelector, SelectionKey.OP_READ);
                doWrite();
            } else {
                // 再连接
                mSocketChannel.register(mSelector, SelectionKey.OP_CONNECT);
            }
        }

        private void doWrite() throws IOException {
            String clientInfo = "query time from client";
            byte[] bytes = clientInfo.getBytes();
            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
            byteBuffer.put(bytes);
            byteBuffer.flip();
            mSocketChannel.write(byteBuffer);
            if (!byteBuffer.hasRemaining()) {
                System.out.println("send info successed");
            }
        }

    }

}
