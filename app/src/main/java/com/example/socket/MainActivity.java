package com.example.socket;

import android.annotation.SuppressLint;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ServerSocket serverSocket;
    private Thread Thread1 = null;
    private TextView tvIP, tvPort, tvMessages;
    private EditText etMessage;
    private Button btnSend;
    public static String SERVER_IP = "";
    public static final int SERVER_PORT = 8080;
    private String message;
    private PrintWriter output;
    private BufferedReader input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvIP = findViewById(R.id.tvIP);
        tvPort = findViewById(R.id.tvPort);
        tvMessages = findViewById(R.id.tvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        try {
            SERVER_IP = getLocalIpAddress();
            Log.d(TAG, "IP Address: " + SERVER_IP);  // Log IP Address
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        Log.d(TAG, "Port: " + SERVER_PORT);  // Log Port

        Thread1 = new Thread(new Thread1());
        Thread1.start();

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    new Thread(new Thread3(message)).start();
                }
            }
        });
    }

    private String getLocalIpAddress() throws UnknownHostException {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
    }

    class Thread1 implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {
                // Check for network permissions or handle accordingly
                serverSocket = new ServerSocket(SERVER_PORT, 50, InetAddress.getByName("0.0.0.0"));
                runOnUiThread(() -> {
                    tvMessages.setText("Not connected");
                    tvIP.setText("IP: " + SERVER_IP);
                    tvPort.setText("Port: " + SERVER_PORT);
                });

                socket = serverSocket.accept();
                output = new PrintWriter(socket.getOutputStream(), true); // auto-flush enabled
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                runOnUiThread(() -> tvMessages.setText("Connected"));

                new Thread(new Thread2()).start();
            } catch (IOException e) {
                Log.e(TAG, "Server socket error: ", e);
                // Handle error appropriately
            }
        }
    }

    private class Thread2 implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    final String message = input.readLine();
                    if (message != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvMessages.append("client: " + message + "\n");
                            }
                        });
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Thread3 implements Runnable {
        private final String message;

        Thread3(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            output.println(message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvMessages.append("server: " + message + "\n");
                    etMessage.setText("");
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
