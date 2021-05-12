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

    private boolean isNumber = false;
    private boolean isAddress = false;
    private boolean isMeter = false;
    private boolean isPH = false;

    private String currentAddress;
    private String currentMeterId;
    private String currentAddressId;



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
            String chatId = update.getMessage().getChatId().toString();
            String message = update.getMessage().getText();
            SendMessage sendMessage = new SendMessage();

            if (message.equals("/start")) {
                isNumber = false;
                isPH = false;
                ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
                replyKeyboardMarkup.setOneTimeKeyboard(false);
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

                sendMessage.setReplyMarkup(replyKeyboardMarkup);
                String txt = "Здравствуйте! Для того что бы внести показания, нам нужно посмотреть ваш номер телефона. Разрешите нам посмотреть его, пожалуйста.";
                sendMsg(chatId, txt, sendMessage);
            }

            if (message.equals("Другой номер")){
                isNumber = false;
                isPH = false;
                newPhoneNumber(chatId);
                return;
            }

            if (message.equals("Сдать показания")){
                isNumber = false;
                isPH = false;

                String phone = new DatabaseHandler().getPhone(chatId);
                if (phone != null) {
                    firstHttpRequest(phone, chatId);
                } else {
                    sendMsg(chatId, "Что то пошло не так, начните заново используя комманду /start .", new SendMessage());
                }
                return;
            }

            //ожидаем номер телефона
            if (isNumber){
                isNumber = false;

                String number = update.getMessage().getText().trim();
                if (number.charAt(0) != '+'){
                    isNumber = true;
                    sendMsg(chatId, "Я вас не понимаю, введите номер телефона по шаблону: +79001234567. Попробуйте еще раз.",
                            new SendMessage());
                    return;
                }
                for (int i = 1; i < number.length(); i++) {
                    char c = number.charAt(i);
                    if (!(c >= '0' && c <= '9')) {
                        isPH = true;
                        sendMsg(chatId, "Я вас не понимаю, введите номер телефона по шаблону: +79001234567. Попробуйте еще раз.",
                                new SendMessage());
                        return;
                    }
                }

                addOrUpdatePhone(number.substring(2), chatId);

                firstHttpRequest(number.substring(2), chatId);
                return;
            }

            //ожидаем показания
            if (isPH){
                isPH = false;

                String ph = update.getMessage().getText().trim();
                //проверка на число
                for (int i = 0; i < ph.length(); i++) {
                    char c = ph.charAt(i);
                    if (!((c >= '0' && c <= '9') || c == '.' || c == ',')) {
                        isPH = true;
                        sendMsg(chatId, "Вы ввели не числовое значение. Попробуйте еще раз.", new SendMessage());
                        return;
                    }
                }
                ph = ph.replaceAll(",",".");
                //проверка на то что запятых или точек не больше одной
                if (ph.length() - ph.replaceAll("\\.","").length() > 1){
                    isPH = true;
                    sendMsg(chatId, "Вы ввели не корректное значение. Попробуйте еще раз.", new SendMessage());
                    return;
                }
                fourthHttpRequest(ph, chatId, currentMeterId);
                return;
            }

        }
        if (update.hasMessage() && update.getMessage().hasContact()){
            String chatId = update.getMessage().getChatId().toString();
            String number = "+" + update.getMessage().getContact().getPhoneNumber();
            getPhoneNumber(chatId, number);
        }
        if (update.hasCallbackQuery()){

            if (isAddress){
                isAddress = false;
                String[] infoAddress = update.getCallbackQuery().getData().split("/");
                String authId = infoAddress[1];
                currentAddress = infoAddress[2];
                currentAddressId = infoAddress[0];
                thirdHttpRequest(update.getCallbackQuery().getMessage().getChatId().toString(), currentAddress, authId);
                return;
            }
            if (isMeter){
                isMeter = false;
                isPH = true;

                String[] infoMeter = update.getCallbackQuery().getData().split("/");
                currentMeterId = infoMeter[0];
                String currentService = infoMeter[1];
                String currentPH = infoMeter[2];

                sendMsg(update.getCallbackQuery().getMessage().getChatId().toString(), "Адрес: " + currentAddress + "\nУслуга: " +
                        currentService + "\nТекущие показания: " + currentPH + "\nВведите ваши показания:", new SendMessage());
                return;
            }
            callBack(update);
        }

    }

    private void callBack(Update update) {
        String request = update.getCallbackQuery().getData();

        if (request.equals("no")){
            /* isNumber = true;
            String str = "Введите ваш номер телефона";
            sendMsg(update.getCallbackQuery().getMessage().getChatId().toString(), str, new SendMessage());*/

            newPhoneNumber(update.getCallbackQuery().getMessage().getChatId().toString());
        }
        if (request.equals("yes")){
            String phone = update.getCallbackQuery().getMessage().getChatId().toString().substring(2);
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();

            addOrUpdatePhone(phone, chatId);

            firstHttpRequest(phone, chatId);
        }

    }

    private void addOrUpdatePhone(String phone, String chatId){
        DatabaseHandler databaseHandler = new DatabaseHandler();
        if (databaseHandler.getPhone(chatId) != null) {
            databaseHandler.updatePhone(phone, chatId);
        } else {
            databaseHandler.insert(phone, chatId);
        }
    }

    private void newPhoneNumber(String chatId){
        isNumber = true;
        String str = "Введите ваш номер телефона";
        sendMsg(chatId, str, new SendMessage());
    }

    private void getPhoneNumber(String chatId, String number) {
        SendMessage sendMessage1 = new SendMessage();
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();

        KeyboardButton keyboardButton = new KeyboardButton();
        keyboardButton.setText("Другой номер");

        KeyboardButton keyboardButton1 = new KeyboardButton();
        keyboardButton1.setText("Сдать показания");

        row1.add(keyboardButton);
        row1.add(keyboardButton1);
        keyboardRowList.add(row1);
        replyKeyboardMarkup.setKeyboard(keyboardRowList);
        sendMessage1.setReplyMarkup(replyKeyboardMarkup);
        sendMessage1.setChatId(chatId);
        sendMessage1.setText(number);

        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText("Это ваш номер телефона?");

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

        sendMessage.setReplyMarkup(inline);
        try {
            execute(sendMessage1);
            execute(sendMessage);
        } catch (TelegramApiException e) {
            //log.log(Level.SEVERE, "Exception: ", e.toString());
            e.printStackTrace();
        }
    }

    private void getAddress(HashMap<String,String> mapAddress, String chatId){
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

    private void getMeter(HashMap<String, String> mapMeters, String chatId, String currentAddress) {
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

    private void firstHttpRequest(String phoneNumber, String chatId) {
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

        //System.out.println(request.toString());

        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request))
        {
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONArray jsonArray = new JSONArray(json);
            JSONObject obj = jsonArray.getJSONObject(0);
            JSONArray arr2 = obj.getJSONArray("records");
            JSONObject obj2 = arr2.getJSONObject(0);
            String authId = obj2.getString("auth_id");

            secondHttpRequest(chatId, authId);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void secondHttpRequest(String chatId, String authId){
        URI uri = null;
        try {
            uri = new URIBuilder("http://172.16.0.227:8086/api/lists/addresses")
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
            switch (obj.getString("msg")){
                case "OK":
                    JSONArray arr2 = obj.getJSONArray("records");
                    for (int i = 0; i < arr2.length(); i++) {
                        JSONObject obj2 = arr2.getJSONObject(i);
                        address.put(obj2.getString("id") + "/" + authId, obj2.getString("address"));
                    }
                    getAddress(address, chatId);
                    break;
                case "Addresses list not received":
                    sendMsg(chatId, "Вы не зарегистрированы в системе, пожалуйста зарегистрируйтесь через личный кабинет.",
                            new SendMessage());
                    break;
                default:
                    sendMsg(chatId, "Что-то пошло не так, пожалуйста сообщите об этом в вашу УК.",
                        new SendMessage());
                    break;
            }

            //System.out.println(address);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void thirdHttpRequest(String chatId, String currentAddress, String authId) {
        URI uri = null;
        try {
            uri = new URIBuilder("http://172.16.0.227:8086/api/lists/meters")
                    .addParameter("auth_id", authId)
                    .addParameter("address_id", currentAddressId)
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
            switch (obj.getString("msg")) {
                case "OK":
                    JSONArray arr2 = obj.getJSONArray("records");
                    for (int i = 0; i < arr2.length(); i++) {
                        JSONObject obj2 = arr2.getJSONObject(i);
                        meters.put(obj2.getString("meter_id"), obj2.getString("service") + "/" + obj2.getString("current_ph"));
                    }
                    getMeter(meters, chatId, currentAddress);
                    break;
                case "Meters list not received":
                    sendMsg(chatId, "На этом адресе нет счетчиков или у них кончился срок поверки.",
                            new SendMessage());
                    break;
                default:
                    sendMsg(chatId, "Что-то пошло не так, пожалуйста сообщите об этом в вашу УК.",
                            new SendMessage());
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fourthHttpRequest(String ph, String chatId, String currentMeterId){
        URI uri = null;
        try {
            uri = new URIBuilder("http://172.16.0.227:8086/api/sayind")
                    .addParameter("address_id", currentAddressId)
                    .addParameter("meter_id", currentMeterId)
                    .addParameter("ind", ph)
                    .build();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        HttpGet request = new HttpGet(uri.toString());
        try(CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(request))
        {
            String json = EntityUtils.toString(response.getEntity(), "UTF-8");
            JSONArray jsonArray = new JSONArray(json);
            JSONObject obj = jsonArray.getJSONObject(0);
            String msg = obj.getString("msg");
            switch (msg){
                case "OK":
                    sendMsg(chatId, "Показания приняты!", new SendMessage());
                    break;
                case "Charge of one of meters already exists":
                    sendMsg(chatId, "По данному счетчику уже есть показания.", new SendMessage());
                    break;
                default:
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getBotUsername() {
        return "meters_sender_bot";
    }

    public String getBotToken() {
        return "1736097569:AAGjJUdLlUoABBMnks8mIaeAk63opV-K_dg";
    }
}

