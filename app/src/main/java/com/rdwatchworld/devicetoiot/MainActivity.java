package com.rdwatchworld.devicetoiot;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static DeviceClient client;
    private final String connString = "HostName=iotworksuccess.azure-devices.net;DeviceId=android_device01;SharedAccessKey=Y/82vmSCJc7PrV73gUlfHyv5TduMBzLwj7MnVaIfjBI=";
    private final String deviceId = "android_device01";
    private static IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    double minTemperature = 20;
    double minHumidity = 60;
    Random rand = new Random();
    ExecutorService executor;
    public static EditText mInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            client = new DeviceClient(connString, protocol);
            client.open();
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        mInfo = (EditText)findViewById(R.id.Info_Text);
        Button mSend = (Button)findViewById(R.id.Send_Button);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageSender sender = new MessageSender();
                executor = Executors.newFixedThreadPool(1);
                executor.execute(sender);
            }
        });

    }
    private static class MessageSender implements Runnable {
        public void run() {
            try {
                // Initialize the simulated telemetry.
                double minTemperature = 20;
                double minHumidity = 60;
                Random rand = new Random();

                while (true) {
                    // Simulate telemetry.
                    double currentTemperature = minTemperature + rand.nextDouble() * 15;
                    double currentHumidity = minHumidity + rand.nextDouble() * 20;
                    TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
                    telemetryDataPoint.temperature = currentTemperature;
                    telemetryDataPoint.humidity = currentHumidity;

                    // Add the telemetry to the message body as JSON.
                    String msgStr = telemetryDataPoint.serialize();
                    Message msg = new Message(msgStr);

                    // Add a custom application property to the message.
                    // An IoT hub can filter on these properties without access to the message body.
                    msg.setProperty("temperatureAlert", (currentTemperature > 30) ? "true" : "false");

                    System.out.println("Sending message: " + msgStr);

                    Object lockobj = new Object();

                    // Send the message.
                    EventCallback callback = new EventCallback();
                    client.sendEventAsync(msg, callback, 1);
/*
                    synchronized (lockobj) {
                        lockobj.wait();
                    }
*/
                    mInfo.setText("Temperature = "+ currentTemperature + " Humidity = " + currentHumidity + "\n");
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                System.out.println("Finished.");
            }
        }
    }
    private static class TelemetryDataPoint {
        public double temperature;
        public double humidity;

        // Serialize object to JSON format.
        public String serialize() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }
    private static class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode status, Object context) {
            System.out.println("IoT Hub responded to message with status: " + status.name());

            if (context != null) {
                synchronized (context) {
                    context.notify();
                }
            }
        }
    }
    public void onDestroy() {
        super.onDestroy();
        try {
            executor.shutdownNow();
            client.closeNow();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}