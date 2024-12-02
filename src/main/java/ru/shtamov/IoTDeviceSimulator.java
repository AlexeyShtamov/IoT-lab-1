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
    private JCheckBox autoModeCheckBox;
    private boolean pumpOn = false;
    private boolean autoMode = true; // По умолчанию автоматический режим
    private Timer timer;
    private int moistureLevel = 50;

    private final String BROKER_URL = "tcp://test.mosquitto.org:1883";
    private final String SENSOR_TOPIC = "sensor/soilMoisture";
    private final String ACTUATOR_TOPIC = "actuator/pump";
    private MqttClient client;

    public IoTDeviceSimulator() {
        setTitle("IoT Device Simulator");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(5, 1));

        moistureLabel = new JLabel("Soil Moisture: " + moistureLevel);
        add(moistureLabel);

        soilMoistureSlider = new JSlider(0, 100, moistureLevel);
        soilMoistureSlider.setEnabled(false);
        add(soilMoistureSlider);

        pumpStatusLabel = new JLabel("Pump is OFF");
        add(pumpStatusLabel);

        pumpButton = new JButton("Toggle Pump");
        pumpButton.setEnabled(false); // Кнопка включена только в ручном режиме
        pumpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!autoMode) { // Логика ручного режима
                    pumpOn = !pumpOn;
                    pumpStatusLabel.setText(pumpOn ? "Pump is ON (Manual)" : "Pump is OFF (Manual)");
                }
            }
        });
        add(pumpButton);

        autoModeCheckBox = new JCheckBox("Automatic Mode", autoMode);
        autoModeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                autoMode = autoModeCheckBox.isSelected();
                pumpButton.setEnabled(!autoMode); // Включаем кнопку только в ручном режиме
                pumpStatusLabel.setText(pumpOn ?
                        (autoMode ? "Pump is ON (Auto)" : "Pump is ON (Manual)") :
                        (autoMode ? "Pump is OFF (Auto)" : "Pump is OFF (Manual)"));
            }
        });
        add(autoModeCheckBox);

        // Таймер для обновления показаний каждые 10 секунд
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateMoistureLevel();
                publishSensorData();
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

        // Логика автоматического режима
        if (autoMode) {
            if (moistureLevel < 20 && !pumpOn) {
                pumpOn = true;
                pumpStatusLabel.setText("Pump is ON (Auto)");
            } else if (moistureLevel > 50 && pumpOn) {
                pumpOn = false;
                pumpStatusLabel.setText("Pump is OFF (Auto)");
            }
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
