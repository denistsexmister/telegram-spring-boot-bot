package com.mrlang.springboottelegrambot.service;

import com.mrlang.springboottelegrambot.config.BotConfig;
import com.mrlang.springboottelegrambot.model.Ads;
import com.mrlang.springboottelegrambot.model.AdsRepository;
import com.mrlang.springboottelegrambot.model.User;
import com.mrlang.springboottelegrambot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_MESSAGE = "Error occurred: ";
    final BotConfig config;
    final UserRepository userRepository;
    final AdsRepository adsRepository;

    static final String HELP_TEXT = """
            This bot is created to demonstrate Spring capabilities.

            You can execute commands from the main menu or by typing a command:

            Type /start to see a welcome message

            Type /mydata to see stored data about yourself
            
            Type /deletedata to delete stored data about yourself
            
            Type /settings to set bot preferences

            Type /help to see this message again""";

    @Autowired
    public TelegramBot(BotConfig config, UserRepository userRepository, AdsRepository adsRepository) {
        super(config.getToken());
        this.config = config;
        this.userRepository = userRepository;
        this.adsRepository = adsRepository;

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
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message msg = update.getMessage();

            if (msg.getText().contains("/send") &&
                    Objects.equals(config.getOwnerId(), msg.getChatId())) {
                String textToSend = EmojiParser.parseToUnicode(msg.getText().substring(msg.getText().indexOf(" ")));
                for (User user : userRepository.findAll()) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {

                switch (msg.getText()) {
                    case "/start":
                        registerUser(msg);
                        startCommandReceived(update);
                        break;
                    case "/help":
                        prepareAndSendMessage(msg.getChatId(), HELP_TEXT);
                        break;
                    case "/register":
                        register(msg.getChatId());
                        break;
                    default:
                        prepareAndSendMessage(msg.getChatId(), "Sorry, command was not recognized");
                }
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();

            if (callbackQuery.getData().equals(YES_BUTTON)) {
                executeEditMessageText("You pressed Yes button!",
                        callbackQuery.getMessage().getChatId(),
                        ((Message) callbackQuery.getMessage()).getMessageId());

            } else if (callbackQuery.getData().equals(NO_BUTTON)) {
                executeEditMessageText("You pressed No button!",
                        callbackQuery.getMessage().getChatId(),
                        ((Message) callbackQuery.getMessage()).getMessageId());

            }
        }
    }

    private void executeEditMessageText(String msg, long chatId, int msgId) {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(msgId)
                .text(msg)
                .build();

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            log.error(ERROR_MESSAGE + e.getMessage());
        }
    }

    private void register(long chatId) {
        InlineKeyboardMarkup keyboardMarkup = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                                .text("Yes")
                                .callbackData(YES_BUTTON)
                                .build(),
                             InlineKeyboardButton.builder()
                                     .text("No")
                                     .callbackData(NO_BUTTON)
                                     .build()))
                .build();
        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text("Do you really want to register?")
                .replyMarkup(keyboardMarkup)
                .build();

        executeMessage(sm);
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
        List<KeyboardRow> rowList = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("weather"));
        row.add(new KeyboardButton("get random joke"));

        rowList.add(row);

        row = new KeyboardRow();
        row.add(new KeyboardButton("register"));
        row.add(new KeyboardButton("check my data"));
        row.add(new KeyboardButton("delete my data"));

        rowList.add(row);

        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text(textToSend)
                .replyMarkup(new ReplyKeyboardMarkup(rowList))
                .build();

        executeMessage(sm);
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage sm = SendMessage.builder()
                .chatId(chatId)
                .text(textToSend)
                .build();

        executeMessage(sm);
    }

    @Override
    public String getBotUsername() {
        return config.getName();
    }

    private void executeMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(ERROR_MESSAGE + e.getMessage());
        }
    }

    @Scheduled(cron = "${bot.ads.cron.scheduler}")
    private void sendAds() {
        Iterable<Ads> ads = adsRepository.findAll();
        Iterable<User> users = userRepository.findAll();

        for (Ads ad : ads) {
            for (User user : users) {
                log.debug("Send ad \"" + ad.getAdText() + "\" to user " + user.getFirstName());

                prepareAndSendMessage(user.getChatId(), ad.getAdText());
            }
        }

    }
}
