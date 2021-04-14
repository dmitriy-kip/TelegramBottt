import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    public static void main(String[] args) {
        ApiContextInitializer.init(); // Инициализируем апи
        TelegramBotsApi botapi = new TelegramBotsApi();
        try {
            botapi.registerBot(new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        String message = update.getMessage().getText();
        System.out.println(update.getMessage().getContact());
        String txt;
        if (update.getMessage().getContact() != null)
            sendMsg(update.getMessage().getChatId().toString(), update.getMessage().getContact().getPhoneNumber());
        else {
            if (message.equals("/start"))
                txt = "Работает!";
            else txt = "Что то пошло не так, но чуть чуть работает";
            sendMsg(update.getMessage().getChatId().toString(), txt);
        }
    }

    public synchronized void sendMsg(String chatId, String s) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRowList = new ArrayList<KeyboardRow>();
        KeyboardRow row1 = new KeyboardRow();

        KeyboardButton keyboardButton = new KeyboardButton();
        keyboardButton.setText("Телефон");
        keyboardButton.setRequestContact(true);

        row1.add(keyboardButton);
        keyboardRowList.add(row1);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            //log.log(Level.SEVERE, "Exception: ", e.toString());
            e.printStackTrace();
        }

    }



    public String getBotUsername() {
        return "meter_reading_sender_bot";
    }

    public String getBotToken() {
        return "1794091980:AAHyVYjIGjZpSB3aFDkgMaY5Zyt5gHGW61k";
    }
}

