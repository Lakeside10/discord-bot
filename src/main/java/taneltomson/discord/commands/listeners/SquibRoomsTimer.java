package taneltomson.discord.commands.listeners;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import taneltomson.discord.commands.data.ASquadType;
import taneltomson.discord.commands.listeners.util.UserWaiting;
import taneltomson.discord.data.ChannelIds;

import static taneltomson.discord.commands.data.ASquadType.LOW;
import static taneltomson.discord.commands.data.ASquadType.MID;
import static taneltomson.discord.commands.data.ASquadType.TOP;
import static taneltomson.discord.commands.data.ASquadType.UPPER;
import static taneltomson.discord.data.ChannelIds.BOT_TEST_TEXT_ID;
import static taneltomson.discord.data.ChannelIds.GENERAL_TEXT_ID;
import static taneltomson.discord.data.ChannelIds.SQUIB_WAITING_ROOM_ID;


@Slf4j
public class SquibRoomsTimer {
    private static final List<Long> TEXT_CHANNELS_TO_USE = Arrays.asList(BOT_TEST_TEXT_ID,
                                                                         GENERAL_TEXT_ID);
    private static final String COMMAND_STRING = "!waiting";
    private static final String COMMAND_TO_SET_A_STRING = "!seta ";

    private static final Map<ASquadType, Long> A_SQUAD_GROUP_IDS = new HashMap<>();
    private static final List<String> hrmRoles = Arrays.asList("commander", "officer",
                                                               "platoon captain");
    private static final List<String> airCommanderRoles = Arrays.asList("air commander");
    private static final List<String> groundCommanderRoles = Arrays.asList("ground commander");
    private static final List<String> airForceRoles = Arrays.asList("sqb air force");
    private static final List<String> antiAirRoles = Arrays.asList("spaa", "spaa commander");
    private static ASquadType currentActiveASquadType = LOW;


    static {
        A_SQUAD_GROUP_IDS.put(LOW, 414151854214348801L);
        A_SQUAD_GROUP_IDS.put(MID, 414152056325144586L);
        A_SQUAD_GROUP_IDS.put(UPPER, 414152172641714176L);
        A_SQUAD_GROUP_IDS.put(TOP, 414152246184509460L);
    }


    private final List<UserWaiting> waitingTimes = new ArrayList<>();

    @EventSubscriber
    public void onUserJoin(final UserVoiceChannelJoinEvent event) {
        final IVoiceChannel voiceChannel = event.getVoiceChannel();

        if (voiceChannel.getLongID() == SQUIB_WAITING_ROOM_ID) {
            handleUserJoinedWaitingRoom(event.getUser());
        }
    }

    @EventSubscriber
    public void onUserLeave(final UserVoiceChannelLeaveEvent event) {
        final IVoiceChannel voiceChannel = event.getVoiceChannel();

        if (voiceChannel.getLongID() == SQUIB_WAITING_ROOM_ID) {
            handleUserLeftWaitingRoom(event.getUser());
        }
    }

    @EventSubscriber
    public void onUserMove(final UserVoiceChannelMoveEvent event) {
        final IVoiceChannel oldChannel = event.getOldChannel();
        final IVoiceChannel newChannel = event.getNewChannel();

        if (oldChannel.getLongID() == SQUIB_WAITING_ROOM_ID) {
            handleUserLeftWaitingRoom(event.getUser());
        } else if (newChannel.getLongID() == SQUIB_WAITING_ROOM_ID) {
            handleUserJoinedWaitingRoom(event.getUser());
        }
    }

    @EventSubscriber
    public void onMessageReceived(final MessageReceivedEvent event) {
        final IChannel channel = event.getChannel();

        if (TEXT_CHANNELS_TO_USE.contains(channel.getLongID())) {
            if (COMMAND_STRING.equals(event.getMessage().getContent())) {
                final StringBuilder sb = new StringBuilder();

                if (waitingTimes.isEmpty()) {
                    sb.append("Noone in the waiting room :(");
                } else {
                    sb.append("Squibs waiting room waiting times:\n");

                    waitingTimes.forEach((userWaiting) -> {
                        final IUser user = userWaiting.getUser();
                        final IGuild guild = event.getGuild();

                        sb.append(user.getDisplayName(event.getGuild()));

                        String additionalRoles = "";

                        if (isHrm(user, guild)) {
                            additionalRoles += "HRM";
                        }
                        if (isAirCommander(user, guild)) {
                            additionalRoles = addRole(additionalRoles, "AC");
                        }
                        if (isGroundCommander(user, guild)) {
                            additionalRoles = addRole(additionalRoles, "GC");
                        }
                        if (isAirForce(user, guild)) {
                            additionalRoles = addRole(additionalRoles, "AF");
                        }
                        if (isATeam(user, guild)) {
                            additionalRoles = addRole(additionalRoles, "A-team");
                        }
                        if (isAntiAir(user, guild)) {
                            additionalRoles = addRole(additionalRoles, "AA");
                        }

                        if (!additionalRoles.isEmpty()) {
                            sb.append(" (");
                            sb.append(additionalRoles);
                            sb.append(")");
                        }
                        sb.append(" - ");
                        sb.append(userWaiting.getWaitedTime());
                        sb.append("\n");
                    });
                }

                event.getChannel().sendMessage(sb.toString());
            }
        }
    }

    @EventSubscriber
    public void onStartUp(final ReadyEvent event) {
        event.getClient().getChannelByID(ChannelIds.BOT_TEST_TEXT_ID)
             .sendMessage("Current A Squad Type is: " + currentActiveASquadType.toString());
    }

    @EventSubscriber
    public void onChangeCurrentActiveASquadTypeReceived(final MessageReceivedEvent event) {
        if (!ChannelIds.BOT_TEST_TEXT_ID.equals(event.getChannel().getLongID())) {
            return;
        }

        final String content = event.getMessage().getContent();

        if (content.toLowerCase().startsWith(COMMAND_TO_SET_A_STRING)) {
            final String desiredValue = content.toLowerCase().replace(COMMAND_TO_SET_A_STRING, "");
            final Optional<ASquadType> desiredType = Arrays
                    .stream(ASquadType.values())
                    .filter(aSquadType -> aSquadType.toString().equals(desiredValue.toUpperCase()))
                    .findFirst();

            if (desiredType.isPresent()) {
                currentActiveASquadType = desiredType.get();
                event.getChannel().sendMessage("Changed A-squad to " + currentActiveASquadType);
            } else {
                final StringBuilder sb = new StringBuilder();

                sb.append("Value ")
                  .append(desiredValue)
                  .append(" not found. Options are:");

                Arrays.stream(ASquadType.values()).
                        forEach(aSquadType -> {
                            sb.append(aSquadType.toString());
                            sb.append("\n");
                        });

                event.getChannel().sendMessage(sb.toString());
            }
        }
    }

    private String addRole(String additionalRoles, String name) {
        additionalRoles += !additionalRoles.isEmpty() ? ", " : "";
        additionalRoles += name;
        return additionalRoles;
    }

    private boolean isATeam(IUser user, IGuild guild) {
        return user.getRolesForGuild(guild).stream()
                   .anyMatch(role -> A_SQUAD_GROUP_IDS.get(currentActiveASquadType)
                                                      .equals(role.getLongID()));
    }

    private boolean isHrm(IUser user, IGuild guild) {

        return user.getRolesForGuild(guild).stream()
                   .anyMatch(role -> hrmRoles.contains(role.getName().toLowerCase()));
    }

    private boolean isAirCommander(IUser user, IGuild guild) {

        return user.getRolesForGuild(guild).stream()
                   .anyMatch(role -> airCommanderRoles.contains(role.getName().toLowerCase()));
    }

    private boolean isGroundCommander(IUser user, IGuild guild) {

        return user.getRolesForGuild(guild).stream()
                   .anyMatch(role -> groundCommanderRoles.contains(role.getName().toLowerCase()));
    }

    private boolean isAirForce(IUser user, IGuild guild) {

        return user.getRolesForGuild(guild).stream()
                   .anyMatch(role -> airForceRoles.contains(role.getName().toLowerCase()));
    }

    private boolean isAntiAir(IUser user, IGuild guild) {
        return user.getRolesForGuild(guild).stream()
                   .anyMatch(role -> antiAirRoles.contains(role.getName().toLowerCase()));
    }

    private void handleUserLeftWaitingRoom(IUser user) {
        log.info("User: {}, moved away from squibs waiting room", user.getName());

        waitingTimes.removeIf(userWaiting -> userWaiting.getUser().getLongID() == user.getLongID());
    }

    private void handleUserJoinedWaitingRoom(IUser user) {
        log.info("User: {}, joined squibs waiting room", user.getName());

        waitingTimes.add(new UserWaiting(user, LocalDateTime.now()));
    }

}
