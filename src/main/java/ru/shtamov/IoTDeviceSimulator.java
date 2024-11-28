package ru.shtamov;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import org.eclipse.paho.client.mqttv3.*;

public class IoTDeviceSimulator extends JFrame {
    private JSlider soilMoistureSlider;
    private JLabel moistureLabel;
    private JButton pumpButton;
    private JLabel pumpStatusLabel;
    private boolean pumpOn = false;
    private Timer timer;
    private int moistureLevel = 50;

    private final String BROKER_URL = "tcp://test.mosquitto.org:1883";
    private final String SENSOR_TOPIC = "sensor/soilMoisture";
    private final String ACTUATOR_TOPIC = "actuator/pump";
    private MqttClient client;
    private boolean manualMode = false;

    public IoTDeviceSimulator() {
        setTitle("IoT Device Simulator");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 1));

        moistureLabel = new JLabel("Soil Moisture: " + moistureLevel);
        add(moistureLabel);

        soilMoistureSlider = new JSlider(0, 100, moistureLevel);
        soilMoistureSlider.setEnabled(false);
        add(soilMoistureSlider);

        pumpStatusLabel = new JLabel("Pump is OFF");
        add(pumpStatusLabel);

        pumpButton = new JButton("Toggle Pump");
        pumpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pumpOn = !pumpOn;
                pumpStatusLabel.setText(pumpOn ? "Pump is ON" : "Pump is OFF");
            }
        });
        add(pumpButton);

        // Таймер для обновления показаний каждые 10 секунд
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateMoistureLevel();
            }
        }, 0, 10000);

        try {
            // Настройка MQTT-клиента
            client = new MqttClient(BROKER_URL, MqttClient.generateClientId());
            client.connect();

            // Подписка на топик для управления насосом
            client.subscribe(ACTUATOR_TOPIC, (topic, message) -> {
                String command = new String(message.getPayload());
                if ("ON".equals(command)) {
                    pumpOn = true;
                    pumpStatusLabel.setText("Pump is ON (Manual)");
                } else if ("OFF".equals(command)) {
                    pumpOn = false;
                    pumpStatusLabel.setText("Pump is OFF (Manual)");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        // Обновление таймера для отправки данных каждые 10 секунд
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateMoistureLevel();
                publishSensorData();
            }
        }, 0, 10000);
    }

    private void publishSensorData() {
        try {
            String data = String.valueOf(moistureLevel);
            client.publish(SENSOR_TOPIC, new MqttMessage(data.getBytes()));
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void updateMoistureLevel() {
        if (pumpOn) {
            moistureLevel = Math.min(moistureLevel + 5, 100); // Влажность увеличивается, если насос включен
        } else {
            moistureLevel = Math.max(moistureLevel - 2, 0); // Влажность уменьшается со временем
        }
        soilMoistureSlider.setValue(moistureLevel);
        moistureLabel.setText("Soil Moisture: " + moistureLevel);

        // Автоматический запуск насоса при низком уровне влажности
        if (moistureLevel < 20 && !pumpOn) {
            pumpOn = true;
            pumpStatusLabel.setText("Pump is ON (Auto)");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new IoTDeviceSimulator().setVisible(true);
            }
        });
    }
}

