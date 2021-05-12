import java.sql.*;

public class DatabaseHandler {
    private Connection connect(){
        Connection connection = null;
        String url = "jdbc:sqlite:C:/Users/feodor/IdeaProjects/TelegramBottt/telegramPhone.db";
        try{
            connection = DriverManager.getConnection(url);
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void insert(String phone, String chatId){
        String sql = "INSERT INTO phones(phone, chatId) VALUES (?,?)";
        try (Connection connection = this.connect(); PreparedStatement prst = connection.prepareStatement(sql)) {
            prst.setString(1,phone);
            prst.setString(2,chatId);
            prst.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public String getPhone(String chatId){
        String result = null;
        String sql = "SELECT phone FROM phones WHERE chatId = ?";
        try (Connection connection = this.connect(); PreparedStatement prst = connection.prepareStatement(sql)) {
            prst.setString(1, chatId);
            ResultSet rs = prst.executeQuery();
            if (!rs.isClosed()) {
                result = rs.getString("phone");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return result;
    }

    public void updatePhone(String phone, String chatId){
        String sql = "UPDATE phones SET phone = ? WHERE chatId = ?";
        try (Connection connection = this.connect(); PreparedStatement prst = connection.prepareStatement(sql)) {
            prst.setString(1,phone);
            prst.setString(2,chatId);
            prst.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}