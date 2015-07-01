package com.xm.csever;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by wm on 15/7/1.
 */
public class TimeServer {


    public static void main(String[] args) {
        int port = 8080;

        new Thread(new MultiMainTimeServer(port)).start();
    }

    private static class MultiMainTimeServer implements Runnable {

        private Selector mSelector;
        private ServerSocketChannel mServerChannel;
        private volatile boolean isStop;

        public MultiMainTimeServer(int port) {

            try {
                mSelector = Selector.open();
                mServerChannel = ServerSocketChannel.open();
                mServerChannel.configureBlocking(false);
                mServerChannel.socket().bind(new InetSocketAddress(port), 1024); // listen
                mServerChannel.register(mSelector, SelectionKey.OP_ACCEPT); // listen the accept


            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void stop() {
            isStop = true;
        }

        @Override
        public void run() {
            while (!isStop) {
                // 不断监听，接受请求
                try {
                    mSelector.select(1000); // 开始查询，设置超时
                    Set<SelectionKey> selectionKeySet = mSelector.selectedKeys(); // 得到已经就绪的集合
                    Iterator<SelectionKey> iterator = selectionKeySet.iterator();

                    SelectionKey key;
                    while (iterator.hasNext()) {
                        key = iterator.next();

                        iterator.remove();
                        try {
                            // 不管注册的什么类型key都会进行注册
                            handleInput(key);
                        } catch (IOException e) {
                            // end
                            if (key != null) {
                                key.cancel();
                                if (key.channel() != null) {
                                    key.channel().close();
                                }
                            }
                        }
                    }


                } catch (Throwable e) {
                    e.printStackTrace();
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
                if (key.isAcceptable()) { // is we need
                    //得到channel
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

                    SocketChannel sc = serverSocketChannel.accept(); // 开始去拿流的sockchannel
                    sc.configureBlocking(false);
                    sc.register(mSelector, SelectionKey.OP_READ); // 有数据可读时 , 注册上
                }

                if (key.isReadable()) { // is  op_read
                    SocketChannel sc = (SocketChannel) key.channel();
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    int readCounts = sc.read(readBuffer);
                    if (readCounts > 0) {
                        readBuffer.flip();

                        byte[] bytes = new byte[readBuffer.remaining()];

                        readBuffer.get(bytes); // 放到实际的byte中

                        String clientToServerInfo = new String(bytes, "UTF-8");

                        System.out.println("This is message from client:" + clientToServerInfo);

                        // socket response

                        String currentTime = new Date(System.currentTimeMillis()).toString();
                        System.out.println("This is Server send to client message:" + currentTime);
                        doWrite(sc, currentTime);

                    } else if (readCounts < 0) {
                        // end
                        key.cancel();
                        sc.close();
                    } else {
                        // 0 字节，
                    }

                }
            }
        }

        private void doWrite(SocketChannel socketChannel, String response) throws IOException {

            if (response != null && response.trim().length() > 0) {
                byte[] bytes = response.getBytes();
                ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                buffer.put(bytes);
                socketChannel.write(buffer);
                buffer.flip();
                socketChannel.write(buffer);
            }
        }

    }


}
