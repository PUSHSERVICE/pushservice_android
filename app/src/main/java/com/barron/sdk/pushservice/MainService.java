/*
WWW.BARRON.COM.SE
Bütün Hakları Saklıdır.
2020 (C) İstanbul'da geliştirilmiştir.
mail@barron.com.se
------------------------
All Rights Reserved
2020 (C) Made in İstanbul
------------------------
For SDK SETUP Instructions Please visit the site.
*/

package com.barron.sdk.pushservice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;

import static com.barron.sdk.pushservice.Config.CHANNEL_ID;
import static com.barron.sdk.pushservice.Config._USERID;
import static com.barron.sdk.pushservice.Config.currentUser;
import static com.barron.sdk.pushservice.Config.servername;

public class MainService extends Service implements  IPushController {
    String TAG = "XMPPappdebug";
    InetAddress addr;
    XMPPTCPConnection connection;

    @Override
    public String getToken() {
        if (connection.isConnected()) {
            return Config.TOKEN;
        } else {
            return "no token received.";
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public String onTokenReceive() {
        if (connection.isConnected()) {
            Config.TOKEN = connection.getStreamId();
            return Config.TOKEN;
        } else {
            return "no token received.";
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        _USERID = getPackageName();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        StrictMode.setThreadPolicy(policy);


        tryToConnect();
        return START_STICKY;

    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(/*savedInstanceState*/);
//        //setContentView(R.layout.activity_main);
//
//
//        //connection.addStanzaSendingListener(stanzaListener, stanzaFilter);
//
////        Button sendmsg = findViewById(R.id.button);
////        Button button2 = findViewById(R.id.button2);
////        Button deloofline = findViewById(R.id.button3);
////        button2.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                try {
////                    checkOfflineMsg(notificationManager);
////
////                } catch (Exception a) {
////                }
////            }
////        });
////        sendmsg.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                try {
////
////                    Message msgnew = new Message();
////                    msgnew.setBody("a message from " + _USERID);
////                    msgnew.setSubject("body");
////                    msgnew.setType(Message.Type.normal);
////                    msgnew.setTo(_USERID.equals("test1") ? "test" : "test1" + "@" + servername);
////                    connection.sendStanza(msgnew);
////                } catch (Exception a) {
////                }
////
////            }
////        });
////        deloofline.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                try {
////
////                    OfflineMessageManager OMM = new OfflineMessageManager(connection);
////                    OMM.deleteMessages();
////                } catch (Exception a) {
////                }
////
////            }
////        });
//    }

//    @Override
//    public void pingFailed() {
//        Log.d(TAG, "pingFailed");
//
//    }

    public void tryToConnect() {


        try {
            addr = InetAddress.getByName(servername);

        } catch (Exception e) {
        }

        createNotificationChannel();


        XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setConnectTimeout(30000);
        // config.setUsernameAndPassword("test1", "000000");

        try {

            config.setResource("somehash");
        } catch (Exception a) {

        }

        config.setHost(servername);
        config.setPort(Config.PORT);
        config.setHostAddress(addr);
        //config.setSendPresence(true);
        try {
            config.setXmppDomain(servername);
        } catch (Exception r) {

        }

        config.allowEmptyOrNullUsernames();
        config.performSaslAnonymousAuthentication();
        config.enableDefaultDebugger();
        XMPPTCPConnection.setUseStreamManagementResumptiodDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);

        connection = new XMPPTCPConnection(config.build());
        connection.setUseStreamManagement(true);


        ConnectionListener connectionListener = new ConnectionListener() {
            @Override
            public void connected(XMPPConnection connection) {
                Log.d(TAG, "connected, streamid=" + connection.getStreamId());
                // TextView clientid = findViewById(R.id.clientid);
                //clientid.setText("now connected " + connection.getStreamId());

            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                Log.d(TAG, "authenticated");
                doPostData();
                onTokenReceive();
                try {


                    StanzaListener stanzaListener = new StanzaListener() {
                        @Override
                        public void processStanza(Stanza packet) throws SmackException.NotConnectedException, InterruptedException, SmackException.NotLoggedInException {
                            Log.e(TAG, "incoming process" + packet.getFrom());
                        }
                    };


                    StanzaFilter stanzaFilter = new StanzaFilter() {

                        @Override
                        public boolean accept(Stanza stanza) {
                            //  Log.e(TAG, "incoming " + ((Message)stanza).getBody() );

                            if (stanza != null && stanza.getTo() != null && stanza.getTo().equals(currentUser)) {
//                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
//                                        .setSmallIcon(R.mipmap.ic_launcher)
//                                        .setContentTitle("New Message")
//                                        .setContentText(((Message) stanza).getBody())
//                                        .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
//                                        .setPriority(NotificationCompat.PRIORITY_MAX);
//                                notificationManager.notify(32, builder.build());

                                Log.e(TAG, "incoming " + ((Message) stanza).getBody());

                            }

//                            if (stanza != null && stanza.getFrom() != null && stanza.getFrom().equals(servername)) {
//                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
//                                        .setSmallIcon(R.mipmap.ic_launcher)
//                                        .setContentTitle("New Message")
//                                        .setContentText(((Message) stanza).getBody())
//                                        .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
//
//                                        .setPriority(NotificationCompat.PRIORITY_MAX);
//                                notificationManager.notify(32, builder.build());
//
//                                Log.e(TAG, "incoming wholemsg " + ((Message) stanza).getBody());
//
//                            }
                            return false;
                        }
                    };
                    connection.addStanzaListener(stanzaListener, stanzaFilter);
//
//                    ChatManager CM = ChatManager.getInstanceFor(connection);
//                    CM.addIncomingListener(new IncomingChatMessageListener() {
//                        @Override
//                        public void newIncomingMessage(EntityBareJid from, Message m, Chat chat) {
//
//
//                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
//                                    .setSmallIcon(R.mipmap.ic_launcher)
//                                    .setContentTitle("New Message")
//                                    .setContentText(((Message) m).getBody())
//                                    .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
//                                    .setPriority(NotificationCompat.PRIORITY_MAX);
//                            notificationManager.notify(32, builder.build());
//
//                        }
//                    });
//                    Presence presence = new Presence(Presence.Type.available);
////
////
//                    //
//                    connection.sendStanza(presence);//after getting offline messages
//                    //checkOfflineMsg(notificationManager);

                } catch (Exception a) {
                    Log.e("Offlinemsg2", " " + a.getMessage());

                }

            }

            @Override
            public void connectionClosed() {
                Log.d(TAG, "connectionClosed");

            }

            @Override
            public void connectionClosedOnError(Exception e) {
                Log.d(TAG, "connectionClosedOnError" + e.getMessage());

            }

        };

        connection.addConnectionListener(connectionListener);
//        PingManager pingManager = PingManager.getInstanceFor(connection);
//        pingManager.registerPingFailedListener(this);
        try {
            connection.connect();
//
            //connection.login(_USERID, "000000");
//            Localpart lp = Localpart.from("test123");
//            AccountManager accountManager = AccountManager.getInstance(connection);
//            accountManager.sensitiveOperationOverInsecureConnection(true);
//            accountManager.createAccount(lp, "000000");


            connection.login();
            Log.i("XMPPClient", "Logged in as " + connection.getUser());
            currentUser = connection.getUser().asEntityBareJidString();
            // Set the status to available

            // XMPPTCPConnection.setConnection(connection);


        } catch (Exception ex) {

            Log.e("XMPPClient", ex.toString());
        }

    }

    public void doPostData() {
        try {

            URL url = new URL("http://pushservice.barron.com.se:801/register");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {


                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                out.write(new byte[]{1, 2, 3});
                //writeStream(out);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                in.read();
                System.out.println("result of stream " + in.read());
                //readStream(in);
            } catch (Exception a) {

            } finally {
                urlConnection.disconnect();
            }

        } catch (Exception d) {
        }

    }


    public void checkOfflineMsg(NotificationManagerCompat notificationManager) {

        try {

            OfflineMessageManager OMM = new OfflineMessageManager(connection);

            List<Message> OfflineMessageList = OMM.getMessages();
            Log.e("Offlinemsg2", " " + OfflineMessageList.size());

            int notified = 0;
            for (Message m : OfflineMessageList) {
                notified++;
                Log.e("Offlinemsg2", " " + m.getStanzaId());

                try {
                    DeliveryReceiptRequest request = DeliveryReceiptRequest.from(m);
                    if (request != null) {
                        Message receipt = DeliveryReceiptManager.receiptMessageFor(m);
                        connection.sendStanza(receipt);
                    }


                } catch (Exception e) {

                    Log.e("Offlinemsg2", " " + e.getMessage());

                }

//
//                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
//                        .setSmallIcon(R.mipmap.ic_launcher)
//                        .setContentTitle("New Message")
//                        .setContentText(((Message) m).getBody())
//                        .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
//                        .setPriority(NotificationCompat.PRIORITY_MAX);
//                notificationManager.notify(32, builder.build());


            }
            if (notified == OfflineMessageList.size() + 1) {

                OMM.deleteMessages();
            }

        } catch (Exception a) {
        }


    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "kanal";
            String description = "testkanalı";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            connection.disconnect();
        } catch (Exception asd) {
        }

    }

}
