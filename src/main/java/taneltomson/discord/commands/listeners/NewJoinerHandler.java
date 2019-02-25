package taneltomson.discord.commands.listeners;


import java.util.Collections;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserJoinEvent;
import sx.blah.discord.handle.impl.events.guild.member.UserRoleUpdateEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import taneltomson.discord.common.service.CustomCommandService;
import taneltomson.discord.data.ChannelIds;
import taneltomson.discord.util.EmojiHelper;
import taneltomson.discord.util.MessageHelper;
import taneltomson.discord.util.UserHelper;


@Slf4j
public class NewJoinerHandler {
    private static final Long RULES_CHANNEL = 402778535191904260L;
    private static final Long SQB_INFO_CHANNEL = 511697693043589121L;
    private static final Long NOT_READ_RULES_ROLE = 513681624211390465L;
    private static final Long NOT_READ_SQB_INFO_ROLE = 520916408930926602L;
    private static final Long COMPETITIVE_ROLE = 405891181495451659L;
    private static final Long CONFIRM_READ_RULES_POST_ID = 513686704297607192L;
    private static final Long CONFIRM_READ_SQB_INFO_POST_ID = 520913927824211979L;
    private static final ReactionEmoji CONFIRM_EMOJI = ReactionEmoji.of("\uD83D\uDC4D");
    private static final String READ_SQB_INFO_PRIVATE_MESSAGE = ""
            + "There is one more step you need to complete to start playing squadron battles "
            + "with us.\n"
            + "\n"
            + "You have been given a temporary role which prevents you from joining voice rooms "
            + "for squadron battles but allows you to read the necessary information "
            + "on how we run squadron battles: etiquette, rules and guides.\n"
            + "\n"
            + "Head to the Try Hards server and read through the channel #sqb-info.\n"
            + "\n"
            + "Once you're done reading you need to add a thumbs up reaction to the last post in "
            + "the #sqb-info channel. After that you'll get your role to join squadron battles "
            + "voice rooms.\n"
            + "\n"
            + "If you have any questions, please do not hesitate to ask any officers or above "
            + "either directly by PM or in #general-chat.\n"
            + "\n";
    private static final String WELCOME_PRIVATE_MESSAGE = ""
            + "Welcome to Try Hard Coalition. You have been set a role on the server that "
            + "prevents you from seeing any of our channels except  #Squadron-Rules. This is "
            + "mandatory reading for all guests & new recruits joining both main (xTHCx) and "
            + "casual (vTHCv) Squadrons. When you have entered and read through the channel "
            + "#Squadron-Rules you must then acknowledge that you read and agree with them by "
            + "clicking the reaction at the end. This will open the whole server for you to "
            + "access. If you have any questions, please contact an officer or above.  In "
            + "addition, if you are joining xTHCx, please read on to understand the Probation "
            + "process.\n"
            + "\n"
            + "Probation system (Only applicable for the competitive Squadron xTHCx):\n"
            + "\n"
            + "New recruits are required to complete a 2 week probation period. During this time,"
            + " your maturity, general behaviour and ability to work in a team are assessed. The "
            + "following ranks can complete assessments:\n"
            + "\n"
            + "- Sergeant\n"
            + "- Platoon Captain\n"
            + "- Officer\n"
            + "- Commander\n"
            + "\n"
            + "Assessments can be completed in any game mode and all that is required is to Squad"
            + " with one of these ranks when playing.\n"
            + "\n"
            + "You should remind the ranked member to complete your assessment when you finished "
            + "playing because they will not always remember to do one. You should be provided "
            + "with a copy.\n"
            + "\n"
            + "There is a limit to 1 assessment per day.\n"
            + "\n"
            + "A minimum of 4 assessments are required to pass the probation period.\n"
            + "\n"
            + "New recruits that fail the probation period are dismissed from the Squadron.\n"
            + "\n"
            + "The purpose of the probation period is to ensure that those joining the community "
            + "are mature enough, active enough & behave within the rules.\n"
            + "\n";
    private static final String READ_SQB_INFO_BUT_NOT_RULES_PRIVATE_MESSAGE = ""
            + "You've indicated that you've read the squadron battles info, however you have not "
            + "yet indicated that you have read our squadron rules.\n"
            + "\n"
            + "Please read the squadron rules first (refer to the earlier message that was "
            + "sent to you from the bot) and add the reaction in #squadron-rules.\n"
            + "\n"
            + "Once you've done that indicate that you've read the SQB info again.\n"
            + "\n";
    private static final String READ_RULES_MESSAGE = "User %s has stated they have read the rules. "
            + "They now have full access to the discord. They still need to be assigned a role "
            + "(Guest, Try Hard Competitive or Try Hard Casual; plus probation if appropriate).";
    private static final String READ_SQB_INFO_MESSAGE = "User %s has stated they have read the "
            + "sqb info. They now have full access to SQB voice comms.";
    private static final String NOT_READ_RULES_WHEN_READ_SQB_INFO = "User %s has stated they have "
            + "read the sqb info but they have not yet read the rules. Will force them to read "
            + "the rules first.";
    private static final String ADDED_TEMP_SQB_ROLE_MESSAGE = "User %s has been given a temporary "
            + "role that forces them to read the #sqb-info channel.";

    private final CustomCommandService customCommandService;
    private final IDiscordClient client;

    public NewJoinerHandler(CustomCommandService ccs, IDiscordClient client) {
        log.debug("NewJoinerHandler created.");
        this.client = client;
        this.customCommandService = ccs;
    }

    @EventSubscriber
    public void onReadyEvent(final ReadyEvent event) {
        EmojiHelper.addEmojiIfNone(getRulesReactionMsg(), CONFIRM_EMOJI);
        EmojiHelper.removeDisallowedEmojis(getRulesReactionMsg(),
                                           Collections.singletonList(CONFIRM_EMOJI));

        EmojiHelper.addEmojiIfNone(getSqbInfoReactionMsg(), CONFIRM_EMOJI);
        EmojiHelper.removeDisallowedEmojis(getSqbInfoReactionMsg(),
                                           Collections.singletonList(CONFIRM_EMOJI));
    }

    @EventSubscriber
    public void onUserJoinEvent(final UserJoinEvent event) {
        final IUser user = event.getUser();
        log.debug("A new user joined discord server: {}", user);

        UserHelper.addRoleToUser(user, event.getGuild().getRoleByID(NOT_READ_RULES_ROLE));
        log.debug("Assigned role that person hasn't read the rules.");

        MessageHelper.trySendPrivateMessage(user, WELCOME_PRIVATE_MESSAGE,
                                            event.getClient(), event.getGuild());
        if (customCommandService.hasCommand("welcome")) {
            final IChannel generalChannel = event.getClient()
                                                 .getChannelByID(ChannelIds.GENERAL_TEXT_ID);
            MessageHelper.sendMessage(generalChannel,
                                      customCommandService.findCommand("welcome").getValue());
        }
    }

    @EventSubscriber
    public void onUserRoleUpdateEvent(final UserRoleUpdateEvent event) {
        final IRole competitiveRole = event.getClient().getRoleByID(COMPETITIVE_ROLE);
        final IRole notReadSqbInfoRole = event.getClient().getRoleByID(NOT_READ_SQB_INFO_ROLE);

        final IUser user = event.getUser();
        final boolean userHadCompetitiveRole = event.getOldRoles().contains(competitiveRole);
        final boolean userGotCompetitiveRole = event.getNewRoles().contains(competitiveRole);
        final boolean userReadSqbInfo = client.getChannelByID(SQB_INFO_CHANNEL)
                                              .fetchMessage(CONFIRM_READ_SQB_INFO_POST_ID)
                                              .getReactionByEmoji(CONFIRM_EMOJI)
                                              .getUserReacted(user);

        log.info("Got userRoleUpdateEvent: user: {}, oldRoles: {}, newRoles: {}",
                 user.getDisplayName(event.getGuild()),
                 event.getOldRoles().stream().map(IRole::getName).toArray(),
                 event.getNewRoles().stream().map(IRole::getName).toArray());

        if (!userHadCompetitiveRole && userGotCompetitiveRole && !userReadSqbInfo) {
            UserHelper.removeRoleFromUser(user, competitiveRole);
            UserHelper.addRoleToUser(user, notReadSqbInfoRole);

            MessageHelper.trySendPrivateMessage(user, READ_SQB_INFO_PRIVATE_MESSAGE,
                                                event.getClient(), event.getGuild());

            final String addedTempRoleMsg = String.format(ADDED_TEMP_SQB_ROLE_MESSAGE,
                                                          user.getDisplayName(event.getGuild()));
            MessageHelper.sendMessage(event.getClient().getChannelByID(ChannelIds.BOT_TEST_TEXT_ID),
                                      addedTempRoleMsg);
        }
    }

    @EventSubscriber
    public void onReactionAddedEvent(final ReactionAddEvent event) {
        final IUser user = event.getUser();

        if (user.isBot()) {
            return;
        }

        if (CONFIRM_READ_RULES_POST_ID.equals(event.getMessage().getLongID())) {
            handleReadRulesReaction(event, user);
        } else if (CONFIRM_READ_SQB_INFO_POST_ID.equals(event.getMessage().getLongID())) {
            handleReadSqbInfoReaction(event, user);
        }
    }

    private void handleReadRulesReaction(ReactionAddEvent event, IUser user) {
        final ReactionEmoji emoji = event.getReaction().getEmoji();

        if (CONFIRM_EMOJI.equals(emoji)) {
            final IDiscordClient client = event.getClient();
            final IRole notReadRulesRole = client.getRoleByID(NOT_READ_RULES_ROLE);
            final IChannel botTestChannel = client.getChannelByID(ChannelIds.BOT_TEST_TEXT_ID);

            if (user.getRolesForGuild(event.getGuild()).contains(notReadRulesRole)) {
                final String readRulesMsg = String.format(
                        READ_RULES_MESSAGE, user.getDisplayName(event.getGuild()));
                MessageHelper.sendMessage(botTestChannel, readRulesMsg);
            }

            UserHelper.removeRoleFromUser(user, notReadRulesRole);
        } else {
            getRulesReactionMsg().removeReaction(user, emoji);
        }
    }

    private void handleReadSqbInfoReaction(ReactionAddEvent event, IUser user) {
        final ReactionEmoji emoji = event.getReaction().getEmoji();

        if (CONFIRM_EMOJI.equals(emoji)) {
            final IDiscordClient client = event.getClient();
            final IChannel botTestChannel = client.getChannelByID(ChannelIds.BOT_TEST_TEXT_ID);

            final List<IRole> userRoles = user.getRolesForGuild(event.getGuild());

            if (userRoles.contains(client.getRoleByID(NOT_READ_RULES_ROLE))) {
                getSqbInfoReactionMsg().removeReaction(user, emoji);

                final String msg = String.format(NOT_READ_RULES_WHEN_READ_SQB_INFO,
                                                 user.getDisplayName(event.getGuild()));
                MessageHelper.sendMessage(botTestChannel, msg);

                MessageHelper.trySendPrivateMessage(user,
                                                    READ_SQB_INFO_BUT_NOT_RULES_PRIVATE_MESSAGE,
                                                    event.getClient(), event.getGuild());
            } else {
                if (userRoles.contains(client.getRoleByID(NOT_READ_SQB_INFO_ROLE))) {
                    final String readSqbInfoMsg = String.format(READ_SQB_INFO_MESSAGE,
                                                                user.getDisplayName(
                                                                        event.getGuild()));
                    MessageHelper.sendMessage(botTestChannel, readSqbInfoMsg);
                }

                UserHelper.removeRoleFromUser(user, client.getRoleByID(NOT_READ_SQB_INFO_ROLE));
                UserHelper.addRoleToUser(user, client.getRoleByID(COMPETITIVE_ROLE));
            }
        } else {
            getSqbInfoReactionMsg().removeReaction(user, emoji);
        }
    }

    private IMessage getRulesReactionMsg() {
        return client.getChannelByID(RULES_CHANNEL)
                     .fetchMessage(CONFIRM_READ_RULES_POST_ID);
    }

    private IMessage getSqbInfoReactionMsg() {
        return client.getChannelByID(SQB_INFO_CHANNEL)
                     .fetchMessage(CONFIRM_READ_SQB_INFO_POST_ID);
    }
}
