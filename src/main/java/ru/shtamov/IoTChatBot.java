package ru.shtamov;

import org.eclipse.paho.client.mqttv3.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class IoTChatBot extends TelegramLongPollingBot {

    private final String MQTT_BROKER_URL = "tcp://test.mosquitto.org:1883";
    private final String SENSOR_TOPIC = "sensor/soilMoisture";
    private final String ACTUATOR_TOPIC = "actuator/pump";
    private MqttClient mqttClient;
    private boolean pumpOn = false;

    public IoTChatBot() {
        try {
            // Настройка MQTT-клиента
            mqttClient = new MqttClient(MQTT_BROKER_URL, MqttClient.generateClientId());
            mqttClient.connect();

            // Подписка на топик телеметрии
            mqttClient.subscribe(SENSOR_TOPIC, (topic, message) -> {
                String data = new String(message.getPayload());
                System.out.println("Received telemetry: " + data);
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String userMessage = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            try {
                switch (userMessage.toLowerCase()) {
                    case "/start":
                        sendMessage(chatId, "Добро пожаловать! Я IoT-бот. Команды:\n" +
                                "/telemetry - получить текущие данные телеметрии\n" +
                                "/pump_on - включить насос\n" +
                                "/pump_off - выключить насос");
                        break;
                    case "/telemetry":
                        sendTelemetry(chatId);
                        break;
                    case "/pump_on":
                        controlPump(chatId, true);
                        break;
                    case "/pump_off":
                        controlPump(chatId, false);
                        break;
                    default:
                        sendMessage(chatId, "Неизвестная команда. Используйте /start для списка команд.");
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendTelemetry(String chatId) {
        try {
            String data = "Текущая влажность почвы: 50%";
            sendMessage(chatId, data);
        } catch (Exception e) {
            sendMessage(chatId, "Не удалось получить телеметрию.");
            e.printStackTrace();
        }
    }

    private void controlPump(String chatId, boolean turnOn) {
        try {
            pumpOn = turnOn;
            String command = turnOn ? "ON" : "OFF";
            mqttClient.publish(ACTUATOR_TOPIC, new MqttMessage(command.getBytes()));
            sendMessage(chatId, "Насос " + (turnOn ? "включен" : "выключен") + ".");
        } catch (MqttException e) {
            sendMessage(chatId, "Ошибка управления насосом.");
            e.printStackTrace();
        }
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "internetveshey_bot";
    }

    @Override
    public String getBotToken() {
        return "7886583736:AAH3BLqtRyDm0n85xPtSjtrvhkxVuOb69B8";
    }

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new IoTChatBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
