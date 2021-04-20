import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    private SendMessage sendMessage = new SendMessage();
    private String txt;


    public static void main(String[] args) {
        try {
            TelegramBotsApi botApi = new TelegramBotsApi(DefaultBotSession.class);
            botApi.registerBot(new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        String chatId = update.getMessage().getChatId().toString();

        if (update.getMessage().hasText()){
            String message = update.getMessage().getText();
            if (message.equals("/start")) {
                txt = "Здравствуйте! Для того что бы получить ваши показания, нам нужно посмотреть ваш номер телефона. Разрешите нам посмотреть его, пожалуйста.";
            }

            startMsg(chatId, txt);
        }
        if (update.getMessage().hasContact()){
            sendMsg(chatId, update.getMessage().getContact().getPhoneNumber());
        }
        if (update.hasCallbackQuery()){

        }

        /*if (update.getMessage().getContact() != null)
            sendMsg(chatId, update.getMessage().getContact().getPhoneNumber());
        else {
            if (message.equals("/start"))
                txt = "Здравствуйте! Для того что бы получить ваши показания, нам нужно посмотреть ваш номер телефона. Разрешите нам посмотреть его, пожалуйста.";
            else txt = "Что то пошло не так, но чуть чуть работает";
            startMsg(chatId, txt);
        }*/
    }

    private void sendMsg(String chatId, String phoneNumber) {
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(phoneNumber + "\nЭто ваш номер телефона?");

        InlineKeyboardMarkup inline = new InlineKeyboardMarkup();

        InlineKeyboardButton buttonYes = new InlineKeyboardButton();
        InlineKeyboardButton buttonNo = new InlineKeyboardButton();

        buttonYes.setText("Да");
        buttonYes.setCallbackData("yes");
        buttonNo.setText("Нет");
        buttonNo.setCallbackData("no");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<InlineKeyboardButton>();
        keyboardButtonsRow1.add(buttonYes);
        keyboardButtonsRow1.add(buttonNo);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<List<InlineKeyboardButton>>();
        rowList.add(keyboardButtonsRow1);
        inline.setKeyboard(rowList);

        /*ReplyKeyboardRemove rr = new ReplyKeyboardRemove();
        rr.setRemoveKeyboard(true);
        sendMessage.setReplyMarkup(rr);*/
        sendMessage.setReplyMarkup(inline);


        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            //log.log(Level.SEVERE, "Exception: ", e.toString());
            e.printStackTrace();
        }
    }


    public synchronized void startMsg(String chatId, String s) {

        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRowList = new ArrayList<KeyboardRow>();
        KeyboardRow row1 = new KeyboardRow();

        KeyboardButton keyboardButton = new KeyboardButton();
        keyboardButton.setText("Разрешаю!");
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
        return "new_meters_sender_bot";
    }

    public String getBotToken() {
        return "1706869454:AAEgKQr1VCTDZwFgZUhp2qh5MY8Y4l26_3I";
    }
}

