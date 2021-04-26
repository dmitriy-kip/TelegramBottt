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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bot extends TelegramLongPollingBot {

    private String phoneNumber;
    private boolean isNumber = false;
    private boolean isAddress = false;
    private boolean isMeter = false;
    private boolean isPH = false;
    private String chatId;
    private String currentAddress;
    private String currentService;
    private String currentMeterId;
    private String currentPH;
    private String authId;


    public static void main(String[] args) {
        try {
            TelegramBotsApi botApi = new TelegramBotsApi(DefaultBotSession.class);
            botApi.registerBot(new Bot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            String txt = "";
            String chatId = update.getMessage().getChatId().toString();
            String message = update.getMessage().getText();
            SendMessage sendMessage = new SendMessage();

            if (isNumber){
                isNumber = false;

                //надо написать валидацию
                phoneNumber = rightNumber(update.getMessage().getText()).trim();
                firstHttpRequest(phoneNumber);
                return;
            }

            if (isPH){
                isPH = false;

                //надо написать валидацию
                String ph = update.getMessage().getText().trim();
                return;
            }

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
            if (isAddress){
                isAddress = false;
                String[] infoAddress = update.getCallbackQuery().getData().split("/");
                currentAddress = infoAddress[1];
                thirdHttpRequest(infoAddress[0]);
                return;
            }
            if (isMeter){
                isMeter = false;
                isPH = true;

                String[] infoMeter = update.getCallbackQuery().getData().split("/");
                currentMeterId = infoMeter[0];
                currentService = infoMeter[1];
                currentPH = infoMeter[2];

                sendMsg(update.getCallbackQuery().getMessage().getChatId().toString(), "Адрес: " + currentAddress + "\nУслуга: " +
                        currentService + "\nТекущие показания: " + currentPH + "\nВведите ваши показания:", new SendMessage());
                return;
            }
            callBack(update);
        }

    }

    private void callBack(Update update) {
        String request = update.getCallbackQuery().getData();
        chatId = update.getCallbackQuery().getMessage().getChatId().toString();

        if (request.equals("no")){
            isNumber = true;
            String str = "Введите ваш номер телефона";
            sendMsg(update.getCallbackQuery().getMessage().getChatId().toString(), str, new SendMessage());
        }
        if (request.equals("yes")){
            firstHttpRequest(phoneNumber);
        }

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

    private void getAddress(HashMap<String,String> mapAddress){
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите адрес:");

        InlineKeyboardMarkup inline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        for (Map.Entry<String,String> entry : mapAddress.entrySet()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(entry.getValue());
            button.setCallbackData(entry.getKey() + "/" + entry.getValue());
            List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
            keyboardButtonsRow1.add(button);
            rowList.add(keyboardButtonsRow1);
        }
        inline.setKeyboard(rowList);
        sendMessage.setReplyMarkup(inline);

        isAddress = true;

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            //log.log(Level.SEVERE, "Exception: ", e.toString());
            e.printStackTrace();
        }
    }

    private void getMeter(HashMap<String, String> mapMeters) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText("Адрес: " + currentAddress + "\nВыберите услугу:");

        InlineKeyboardMarkup inline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        for (Map.Entry<String,String> entry : mapMeters.entrySet()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(entry.getValue());
            button.setCallbackData(entry.getKey() + "/" + entry.getValue());
            List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
            keyboardButtonsRow1.add(button);
            rowList.add(keyboardButtonsRow1);
        }
        inline.setKeyboard(rowList);
        sendMessage.setReplyMarkup(inline);

        isMeter = true;

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            //log.log(Level.SEVERE, "Exception: ", e.toString());
            e.printStackTrace();
        }
    }



    private synchronized void sendMsg(String chatId, String s, SendMessage sendMessage) {
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

    private void firstHttpRequest(String phoneNumber) {
        URI uri = null;
        try {
            uri = new URIBuilder("http://prog-matik.ru:8086/api/auth")
                    .addParameter("phone", phoneNumber)
                    .addParameter("app_id", "-1")
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpGet request = new HttpGet(uri.toString());

        //System.out.println(request.toString());

        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request))
        {
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONArray jsonArray = new JSONArray(json);
            JSONObject obj = jsonArray.getJSONObject(0);
            JSONArray arr2 = obj.getJSONArray("records");
            JSONObject obj2 = arr2.getJSONObject(0);
            authId = obj2.getString("auth_id");

            secondHttpRequest();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void secondHttpRequest(){
        URI uri = null;
        try {
            uri = new URIBuilder("http://prog-matik.ru:8086/api/lists/addresses")
                    .addParameter("auth_id", authId)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpGet request = new HttpGet(uri.toString());

        //System.out.println(request.toString());

        HashMap<String, String> address = new HashMap<>();
        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request))
        {
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONArray jsonArray = new JSONArray(json);
            JSONObject obj = jsonArray.getJSONObject(0);
            JSONArray arr2 = obj.getJSONArray("records");
            for (int i = 0; i < arr2.length(); i++) {
                JSONObject obj2 = arr2.getJSONObject(i);
                address.put(obj2.getString("id"), obj2.getString("address"));
            }

            getAddress(address);
            //System.out.println(address);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void thirdHttpRequest(String addressId) {
        URI uri = null;
        try {
            uri = new URIBuilder("http://prog-matik.ru:8086/api/lists/meters")
                    .addParameter("auth_id", authId)
                    .addParameter("address_id", addressId)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpGet request = new HttpGet(uri.toString());
        HashMap<String, String> meters = new HashMap<>();
        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request))
        {
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONArray jsonArray = new JSONArray(json);
            JSONObject obj = jsonArray.getJSONObject(0);
            JSONArray arr2 = obj.getJSONArray("records");
            for (int i = 0; i < arr2.length(); i++) {
                JSONObject obj2 = arr2.getJSONObject(i);
                meters.put(obj2.getString("meter_id"), obj2.getString("service") + "/" + obj2.getString("current_ph"));
            }

            getMeter(meters);
            //System.out.println(address);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String rightNumber(String str){
        return str.substring(2);
    }

    public String getBotUsername() {
        return "new_meters_sender_bot";
    }

    public String getBotToken() {
        return "1706869454:AAEgKQr1VCTDZwFgZUhp2qh5MY8Y4l26_3I";
    }
}

