package taneltomson.discord;


import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


public class BotConfiguration {
    private static final ResourceBundle PROPERTIES =
            PropertyResourceBundle.getBundle("bot", Locale.ROOT);

    private static String getString(String key) {
        return PROPERTIES.getString(key);
    }

    public static String getBotToken() {
        return getString("botToken");
    }
}
