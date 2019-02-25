package taneltomson.discord.commands.listeners;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import taneltomson.discord.commands.listeners.util.WatchPointsTimerTask;


@Slf4j
class TestableWatchPointsTimerTask extends WatchPointsTimerTask {
    public TestableWatchPointsTimerTask(IDiscordClient client) {
        super(client);
    }

    @Override
    protected void sendResponse(String message) {
        throw new ResponseSentException(message);
    }

    public class ResponseSentException extends RuntimeException {
        @Getter private final String messageSent;

        public ResponseSentException(String message) {
            this.messageSent = message;
        }
    }
}
