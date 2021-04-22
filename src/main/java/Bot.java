import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class Bot extends TelegramLongPollingBot {
    private String txt;
    private String phoneNumber;
    private boolean isNumber = false;


    public static void main(String[] args) {
        try {
            TelegramBotsApi botApi = new TelegramBotsApi(DefaultBotSession.class);
            botApi.registerBot(new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        if (isNumber){
            isNumber = false;

            //надо написать валидацию
            phoneNumber = rightNumber(update.getMessage().getText());
            firstHttpRequest(phoneNumber);
        }
        if (update.hasMessage() && update.getMessage().hasText()){
            String chatId = update.getMessage().getChatId().toString();
            String message = update.getMessage().getText();
            SendMessage sendMessage = new SendMessage();

            if (message.equals("/start")) {
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                replyKeyboardMarkup.setOneTimeKeyboard(true);
                sendMessage.setReplyMarkup(replyKeyboardMarkup);
                replyKeyboardMarkup.setSelective(true);
                replyKeyboardMarkup.setResizeKeyboard(true);

                List<KeyboardRow> keyboardRowList = new ArrayList<>();
                KeyboardRow row1 = new KeyboardRow();

                KeyboardButton keyboardButton = new KeyboardButton();
                keyboardButton.setText("Разрешаю!");
                keyboardButton.setRequestContact(true);

                row1.add(keyboardButton);
                keyboardRowList.add(row1);
                replyKeyboardMarkup.setKeyboard(keyboardRowList);
                txt = "Здравствуйте! Для того что бы внести показания, нам нужно посмотреть ваш номер телефона. Разрешите нам посмотреть его, пожалуйста.";
            }

            sendMsg(chatId, txt, sendMessage);
        }
        if (update.hasMessage() && update.getMessage().hasContact()){
            String chatId = update.getMessage().getChatId().toString();
            getPhoneNumber(chatId, update.getMessage().getContact().getPhoneNumber());
        }
        if (update.hasCallbackQuery()){
            callBack(update);
        }

    }

    private void firstHttpRequest(String phoneNumber) {
        URI uri = null;
        try {
            uri = new URIBuilder("http://172.16.0.227:8086/api/auth")
                    .addParameter("phone", phoneNumber)
                    .addParameter("app_id", "-1")
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpGet request = new HttpGet(uri.toString());
        request.addHeader("phone", phoneNumber);
        request.addHeader("app_id", "-1");

        System.out.println(request.toString());

        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request))
        {
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONArray jsonArray = new JSONArray(json);
            JSONObject obj = jsonArray.getJSONObject(0);
            JSONArray arr2 = obj.getJSONArray("records");
            JSONObject obj2 = arr2.getJSONObject(0);
            String authId = obj2.getString("auth_id");
            System.out.println(authId);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void callBack(Update update) {
        String request = update.getCallbackQuery().getData();

        if (request.equals("no")){
            isNumber = true;
            String str = "Введите ваш номер телефона";
            sendMsg(update.getCallbackQuery().getMessage().getChatId().toString(), str, new SendMessage());
        }
        if (request.equals("yes")){
            firstHttpRequest(phoneNumber);
        }

        /*SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
        sendMessage.setText(request);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            //log.log(Level.SEVERE, "Exception: ", e.toString());
            e.printStackTrace();
        }*/
    }

    private void getPhoneNumber(String chatId, String number) {
        phoneNumber = rightNumber(number);

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(number + "\nЭто ваш номер телефона?");

        InlineKeyboardMarkup inline = new InlineKeyboardMarkup();

        InlineKeyboardButton buttonYes = new InlineKeyboardButton();
        InlineKeyboardButton buttonNo = new InlineKeyboardButton();

        buttonYes.setText("Да");
        buttonYes.setCallbackData("yes");
        buttonNo.setText("Нет");
        buttonNo.setCallbackData("no");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(buttonYes);
        keyboardButtonsRow1.add(buttonNo);
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
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


    public synchronized void sendMsg(String chatId, String s, SendMessage sendMessage) {
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(s);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            //log.log(Level.SEVERE, "Exception: ", e.toString());
            e.printStackTrace();
        }

    }

    public String rightNumber(String str){
        return str.substring(2);
    }

    public String getBotUsername() {
        return "new_meters_sender_bot";
    }

    public String getBotToken() {
        return "1706869454:AAEgKQr1VCTDZwFgZUhp2qh5MY8Y4l26_3I";
    }
}

