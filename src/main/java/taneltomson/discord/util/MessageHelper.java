package taneltomson.discord.util;


import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RequestBuffer;
import taneltomson.discord.data.ChannelIds;


@Slf4j
public class MessageHelper extends RequestHelper {
    public static void sendMessage(IChannel channel, String message) {
        queueRequest(() -> channel.sendMessage(message));
    }

    public static void trySendPrivateMessage(IUser user, String message, IDiscordClient client,
                                       IGuild guild) {
        RequestBuffer.request(() -> {
            try {
                user.getOrCreatePMChannel().sendMessage(message);
                log.debug("Sent the welcome PM to user.");
            } catch (DiscordException e) {
                client.getChannelByID(ChannelIds.BOT_TEST_TEXT_ID)
                      .sendMessage("There was a problem sending welcome PM to "
                                           + user.getDisplayName(guild) + ". Please send "
                                           + "the following message to them manually.");
                client.getChannelByID(ChannelIds.BOT_TEST_TEXT_ID)
                      .sendMessage(message);
            }
        });
    }
}
