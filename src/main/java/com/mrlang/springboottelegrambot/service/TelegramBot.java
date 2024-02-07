package com.mrlang.springboottelegrambot.service;

import com.mrlang.springboottelegrambot.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;

    @Autowired
    public TelegramBot(BotConfig config) {
        super(config.getToken());
        this.config = config;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();

        if (msg != null && msg.hasText()) {
            switch (msg.getText()) {
                case "/start":
                    startCommandReceived(update);
                    break;
                default:
                    sendMessage(msg.getChatId(), "Sorry, command was not recognized");
            }
        }
    }

    private void startCommandReceived(Update update) {
        Message msg = update.getMessage();
        String answer = "Hi, " + msg.getChat().getFirstName() +", nice to meet you!";

        sendMessage(msg.getChatId(), answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text(textToSend)
                .build();

        try {
            execute(sm);
        } catch (TelegramApiException e) {

        }
    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }
}
