package taneltomson.discord;


import java.util.Random;

import lombok.extern.slf4j.Slf4j;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventDispatcher;
import sx.blah.discord.util.DiscordException;
import taneltomson.discord.commands.listeners.CustomCommandListener;
import taneltomson.discord.commands.listeners.GiveawayListener;
import taneltomson.discord.commands.listeners.InspireListener;
import taneltomson.discord.commands.listeners.NewJoinerHandler;
import taneltomson.discord.commands.listeners.SquibsGroupChanger;
import taneltomson.discord.commands.listeners.SquibsPointsWatcher;
import taneltomson.discord.commands.listeners.VegetableListener;
import taneltomson.discord.common.service.CustomCommandService;
import taneltomson.discord.commands.listeners.SquibsCommandListener;
import taneltomson.discord.commands.listeners.SquibRoomsTimer;


@Slf4j
public class BotApplication {
    public static void main(String[] args) {
        final IDiscordClient client = createClient(BotConfiguration.getBotToken());
        final EventDispatcher dispatcher = client.getDispatcher();

        final CustomCommandService ccs = new CustomCommandService("botJpaUnit");

        dispatcher.registerListener(new VegetableListener());
        dispatcher.registerListener(new InspireListener());

        dispatcher.registerListener(new SquibRoomsTimer());
        dispatcher.registerListener(new SquibsCommandListener());
        dispatcher.registerListener(new SquibsPointsWatcher(client));
        dispatcher.registerListener(new SquibsGroupChanger(client));

        dispatcher.registerListener(new CustomCommandListener(ccs));

        dispatcher.registerListener(new GiveawayListener(new Random()));

        dispatcher.registerListener(new NewJoinerHandler(ccs, client));

        try {
            client.login();
        } catch (DiscordException e) {
            log.error("Failed to login to discord client - failed to start", e);
            throw new RuntimeException(e);
        }
    }

    private static IDiscordClient createClient(String token) {
        final ClientBuilder clientBuilder = new ClientBuilder();
        clientBuilder.withToken(token);

        try {
            return clientBuilder.build();
        } catch (DiscordException e) {
            log.error("Failed to create a discord client", e);
            throw new RuntimeException(e);
        }
    }
}
