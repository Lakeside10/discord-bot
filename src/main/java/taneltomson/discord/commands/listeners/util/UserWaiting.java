package taneltomson.discord.commands.listeners.util;


import java.time.Duration;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.handle.obj.IUser;


@Slf4j
@AllArgsConstructor
public class UserWaiting {
    @Getter private final IUser user;
    private final LocalDateTime waitingStartTime;

    public String getWaitedTime() {
        final Duration waitingDuration = Duration.between(waitingStartTime, LocalDateTime.now());
        final long waitedSeconds = waitingDuration.getSeconds();
        log.info("Waited seconds: {}", waitedSeconds);

        final String formatted = String.format("%dh %02dm %02ds", waitedSeconds / 3600,
                                               (waitedSeconds % 3600) / 60, (waitedSeconds % 60));
        log.info("Formatted string: {}", formatted);

        return formatted;
    }
}
