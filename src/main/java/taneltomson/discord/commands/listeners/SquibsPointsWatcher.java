package taneltomson.discord.commands.listeners;


import java.util.Collections;
import java.util.List;
import java.util.Timer;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import taneltomson.discord.commands.listeners.util.WatchPointsTimerTask;
import taneltomson.discord.util.MessageHelper;

import static taneltomson.discord.data.ChannelIds.BOT_TEST_TEXT_ID;


@Slf4j
public class SquibsPointsWatcher extends CommandListener {
    private static final int TIMER_INITIAL_DELAY_MS = 0;
    private static final int TIMER_PERIOD_MS = 1000 * 15;

    private final IDiscordClient client;
    private final Timer timer = new Timer();
    private WatchPointsTimerTask timerTask;

    public SquibsPointsWatcher(IDiscordClient client) {
        this.client = client;
    }

    @EventSubscriber
    public void onReadyEvent(final ReadyEvent event) {
        scheduleTimerTask();
    }

    private void scheduleTimerTask() {
        timerTask = new WatchPointsTimerTask(client);
        timer.schedule(timerTask, TIMER_INITIAL_DELAY_MS, TIMER_PERIOD_MS);

        log.info("Scheduled timer to start after {}s and watch points every {}s.",
                 TIMER_INITIAL_DELAY_MS / 1000, TIMER_PERIOD_MS / 1000);
        MessageHelper.sendMessage(client.getChannelByID(BOT_TEST_TEXT_ID),
                                  "Started to watch squibs points. Checking every "
                                          + TIMER_PERIOD_MS / 1000 + " seconds.");
    }

    @Override
    protected List<Long> getAllowedChannelIds() {
        return Collections.singletonList(BOT_TEST_TEXT_ID);
    }

    @Override
    protected void handleCommand(String command, String arguments, MessageReceivedEvent event) {
        if ("watchpoints".equals(command)) {
            if (timerTask != null) {
                log.debug("Cancelling current timer task.");
                timerTask.cancel();
            }

            MessageHelper.sendMessage(client.getChannelByID(BOT_TEST_TEXT_ID),
                                      "Restarting SQB points watcher.");
            scheduleTimerTask();
        }
    }
}
