package taneltomson.discord.util;


import sx.blah.discord.util.RequestBuffer;
import sx.blah.discord.util.RequestBuffer.IVoidRequest;


public class RequestHelper {
    protected static void queueRequest(IVoidRequest request) {
        RequestBuffer.request(request);
    }
}
