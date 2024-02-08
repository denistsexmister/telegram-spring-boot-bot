package com.mrlang.springboottelegrambot.service;

import com.mrlang.springboottelegrambot.config.BotConfig;
import com.mrlang.springboottelegrambot.model.User;
import com.mrlang.springboottelegrambot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    final UserRepository userRepository;

    static final String HELP_TEXT = """
            This bot is created to demonstrate Spring capabilities.

            You can execute commands from the main menu or by typing a command:

            Type /start to see a welcome message

            Type /mydata to see stored data about yourself
            
            Type /deletedata to delete stored data about yourself
            
            Type /settings to set bot preferences

            Type /help to see this message again""";

    @Autowired
    public TelegramBot(BotConfig config, UserRepository userRepository) {
        super(config.getToken());
        this.config = config;
        this.userRepository = userRepository;

        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "get stored user data"));
        listOfCommands.add(new BotCommand("/deletedata", "delete stored user data"));
        listOfCommands.add(new BotCommand("/help", "show command info and usages"));
        listOfCommands.add(new BotCommand("/settings", "set bot preferences"));

        try {
            execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message msg = update.getMessage();

        if (msg != null && msg.hasText()) {
            switch (msg.getText()) {
                case "/start":
                    registerUser(msg);
                    startCommandReceived(update);
                    break;
                case "/help":
                    sendMessage(msg.getChatId(), HELP_TEXT);
                    break;
                default:
                    sendMessage(msg.getChatId(), "Sorry, command was not recognized");
            }
        }
    }

    private void registerUser(Message msg) {
        if (!userRepository.existsById(msg.getChatId())) {
            User user = new User();

            user.setChatId(msg.getChatId());
            user.setFirstName(msg.getChat().getFirstName());
            user.setLastName(msg.getChat().getLastName());
            user.setUserName(msg.getChat().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("New user saved: " + user);
        }
    }

    private void startCommandReceived(Update update) {
        Message msg = update.getMessage();
        String answer = EmojiParser.parseToUnicode(
                "Hi, " + msg.getChat().getFirstName() +", nice to meet you!" + ":heart_eyes:");
        log.info("Replied to user " + msg.getChat().getFirstName());

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
            log.error("Error occurred: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }
}
