package taneltomson.discord.commands.listeners;


import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import taneltomson.discord.util.EmojiHelper;
import taneltomson.discord.util.UserHelper;

import static taneltomson.discord.data.ChannelIds.GUILD_ID;


@Slf4j
public class SquibsGroupChanger {
    private final static Long SQB_TIMEZONES_CHANNEL = 478844839056637973L;
    private final static Long MESSAGE_TO_WATCH = 478844905708453928L;

    private final static Long US_ROLE = 478892223807619083L;
    private final static Long EU_ROLE = 478892366468481034L;

    private final static ReactionEmoji EU_EMOJI = ReactionEmoji.of("\uD83C\uDDEA\uD83C\uDDFA");
    private final static ReactionEmoji US_EMOJI = ReactionEmoji.of("\uD83C\uDDFA\uD83C\uDDF8");
    private final IDiscordClient client;

    public SquibsGroupChanger(IDiscordClient client) {
        this.client = client;
    }

    @EventSubscriber
    public void onReadyEvent(final ReadyEvent event) {
        EmojiHelper.removeDisallowedEmojis(getWatchedMessage(),
                                           Arrays.asList(EU_EMOJI, US_EMOJI));

        EmojiHelper.addEmojiIfNone(getWatchedMessage(), EU_EMOJI);
        EmojiHelper.addEmojiIfNone(getWatchedMessage(), US_EMOJI);
    }

    @EventSubscriber
    public void onReactionAddedOrRemovedEvent(final ReactionEvent event) {
        if (!isBot(event) && reactedToWatchedMessage(event.getMessage())) {
            final IUser user = event.getUser();
            final ReactionEmoji emoji = event.getReaction().getEmoji();

            if (event instanceof ReactionAddEvent) {
                if (EU_EMOJI.equals(emoji)) {
                    UserHelper.addRoleToUser(user, getRole(event, EU_ROLE));
                } else if (US_EMOJI.equals(emoji)) {
                    UserHelper.addRoleToUser(user, getRole(event, US_ROLE));
                } else {
                    getWatchedMessage().removeReaction(user, emoji);
                }
            } else if (event instanceof ReactionRemoveEvent) {
                if (EU_EMOJI.equals(emoji)) {
                    UserHelper.removeRoleFromUser(user, getRole(event, EU_ROLE));
                } else if (US_EMOJI.equals(emoji)) {
                    UserHelper.removeRoleFromUser(user, getRole(event, US_ROLE));
                }
            }
        }
    }

    private boolean isBot(ReactionEvent event) {
        return event.getUser().isBot();
    }

    private IMessage getWatchedMessage() {
        return client.getChannelByID(SQB_TIMEZONES_CHANNEL).fetchMessage(MESSAGE_TO_WATCH);
    }

    private IRole getRole(Event event, Long role) {
        return event.getClient().getGuildByID(GUILD_ID).getRoleByID(role);
    }

    private boolean reactedToWatchedMessage(IMessage message) {
        return MESSAGE_TO_WATCH.equals(message.getLongID());
    }
}
