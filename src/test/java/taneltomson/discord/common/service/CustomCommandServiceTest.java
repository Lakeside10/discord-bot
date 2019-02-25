package taneltomson.discord.common.service;


import java.time.LocalDate;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import taneltomson.discord.TestConfiguration;
import taneltomson.discord.common.model.Command;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;


@Slf4j
public class CustomCommandServiceTest extends DatabaseTestHelper {
    private CustomCommandService service;

    @Before
    public void setUp() {
        super.setUp();

        service = new CustomCommandService(TestConfiguration.getTestPUName());
    }

    @Test
    public void testCreateAndFindCommand() {
        final Command command = new Command().setCallKey("testASD")
                                             .setValue("testValue")
                                             .setCreated(LocalDate.now());

        service.addCommand(command);

        final Command foundCommand = service.findCommand("testasd");
        assertThat(foundCommand, is(notNullValue()));
        assertThat(foundCommand.getCallKey(), is("testASD"));
        assertThat(foundCommand.getValue(), is("testValue"));
        assertThat(foundCommand.getCreated(), is(LocalDate.now()));
    }

    @Test
    public void testHasCommandWithName() {
        assertThat(service.hasCommand("TESTName3"), is(false));

        final Command command = new Command().setCallKey("TESTName3")
                                             .setValue("testValue")
                                             .setCreated(LocalDate.now());
        service.addCommand(command);

        assertThat(service.hasCommand("testname3"), is(true));
    }

    @Test
    public void testDeleteCommandWithName() {
        final Command command = new Command().setCallKey("TESTName4")
                                             .setValue("testValue")
                                             .setCreated(LocalDate.now());
        service.addCommand(command);

        assertThat(service.hasCommand("TESTName4"), is(true));
        service.deleteCommand("testname4");
        assertThat(service.hasCommand("TESTName4"), is(false));
    }

    @After
    public void tearDown() {
        super.tearDown();

        service.close();
    }
}