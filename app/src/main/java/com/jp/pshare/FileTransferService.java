package com.jp.pshare;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mohsenoid.protobuftest.AddressBookProtos;
import com.mohsenoid.protobuftest.MsgProto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.text.DecimalFormat;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d(WiFiDirectActivity.TAG, "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream();
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d(WiFiDirectActivity.TAG, e.toString());
                }
                sendMsg(stream);
                //DeviceDetailFragment.copyFile(is, stream);
                //copyFile(is, stream);
                Log.d(WiFiDirectActivity.TAG, "Client: Data written");
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }

        }
    }

    private void sendMsg(OutputStream outputStream) {
        AddressBookProtos.Person person = AddressBookProtos.Person.newBuilder().setEmail("test@test.com")
                .setId(1).setName("jin").build();
        AddressBookProtos.Person person2 = AddressBookProtos.Person.newBuilder().setEmail("fdsfsfsfs@test.com")
                .setId(1).setName("fdsfsfsfsfsfs").build();
        AddressBookProtos.AddressBook addressBook = AddressBookProtos.AddressBook.newBuilder().addPeople(person2).build();
        MsgProto.Header header = MsgProto.Header.newBuilder().setStag(15).setVersion(1)
                .setType(1).setSeqno(1).setChecksum(8888).setLength(person.toByteArray().length).build();
        MsgProto.Request request = MsgProto.Request.newBuilder().setHeader(header).setBody(person.toByteString()).build();
        Log.e(WiFiDirectActivity.TAG, "sendMsg" + request.toString());
        try {
            request.writeTo(outputStream);
            outputStream.flush();
            header = header.toBuilder().setLength(addressBook.toByteArray().length).build();
            request = request.toBuilder().setHeader(header).setBody(addressBook.toByteString()).build();
            Log.e(WiFiDirectActivity.TAG, "sendMsg2" + request.toString());
            request.writeTo(outputStream);
            outputStream.flush();
            Log.e(WiFiDirectActivity.TAG, "sendMsg done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void copyFile(final InputStream sourcefile, final OutputStream targetFile) {
        try {
            long statTime = System.currentTimeMillis();
//            FileInputStream fileInputStream = new FileInputStream(sourcefile);
            int buffSize = 10 * 1024;//10
            BufferedInputStream inbuff = new BufferedInputStream(sourcefile, buffSize);
//            FileOutputStream fileOutputStream = new FileOutputStream(targetFile);// 新建文件输出流并对它进行缓冲
            BufferedOutputStream outbuff = new BufferedOutputStream(targetFile, buffSize);
            //int fileVolume = (int) (dirSize / (1024 * 1024));
            FileChannel fileChannelOutput = null;//fileOutputStream.getChannel();
            ReadableByteChannel readableByteChannel = Channels.newChannel(inbuff);
            FileChannel fileChannelInput = null;//fileInputStream.getChannel();
            WritableByteChannel writableByteChannel = Channels.newChannel(outbuff);
            //fileChannelInput.transferTo(0, fileChannelInput.size(), fileChannelOutput);
            long transferSize = 0;
            DecimalFormat decimalFormat = new DecimalFormat(".00");//构造方法的字符格式这里如果小数不足2位,会以0补足.
            /*ByteBuffer buffer = ByteBuffer.allocate(buffSize);
            while (readableByteChannel.read(buffer) != -1) {
                buffer.flip();
                transferSize += writableByteChannel.write(buffer);
                buffer.clear();
            }*/
            byte[] bt = new byte[buffSize];
            int c;
            while ((c = inbuff.read(bt)) > 0) {
                transferSize += c;
                outbuff.write(bt, 0, c);
            }
            outbuff.flush();
            inbuff.close();
            outbuff.close();
            targetFile.flush();
            targetFile.close();
            sourcefile.close();
            writableByteChannel.close();
            readableByteChannel.close();
            /*fileOutputStream.close();
            fileInputStream.close();
            fileChannelOutput.close();
            fileChannelInput.close();*/
            float time = (System.currentTimeMillis() - statTime) / 1000f;
            float speed = transferSize / (time * 1024 * 1024f);
            Log.e(WiFiDirectActivity.TAG, "speed:" + decimalFormat.format(speed) + "MB/s, time:" + decimalFormat.format(time) + "s");
        } catch (FileNotFoundException e) {
            Log.e(WiFiDirectActivity.TAG, "CopyPasteUtil copyFile error:" + e.getMessage());
        } catch (IOException e) {
            Log.e(WiFiDirectActivity.TAG, "CopyPasteUtil copyFile error:" + e.getMessage());
        }
    }
}
