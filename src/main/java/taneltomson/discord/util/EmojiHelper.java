package taneltomson.discord.util;


import java.util.List;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IMessage;


@Slf4j
public class EmojiHelper extends RequestHelper {
    public static void removeDisallowedEmojis(IMessage message, List<ReactionEmoji> allowedEmojis) {
        log.debug("removeDisallowedEmojis");

        message.getReactions().forEach(reaction -> {
            final ReactionEmoji emoji = reaction.getEmoji();

            if (!allowedEmojis.contains(emoji)) {
                reaction.getUsers().forEach(user -> {
                    log.debug("removeDisallowedEmojis - Removing irrelevant reaction - user: {}, "
                                      + "emoji: {}", user.getName(), emoji.toString());
                    queueRequest(() -> message.removeReaction(user, emoji));
                });
            }
        });
    }

    public static void addEmojiIfNone(IMessage message, ReactionEmoji emoji) {
        log.debug("addEmojiIfNone - emoji: {}", emoji.toString());

        if (message.getReactionByEmoji(emoji) == null) {
            log.debug("addEmojiIfNone - Adding emoji: {}", emoji.toString());
            queueRequest(() -> message.addReaction(emoji));
        }
    }
}
