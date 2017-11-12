//import com.github.shyiko.dotenv.DotEnv;
import com.sun.org.apache.regexp.internal.RE;
import org.asynchttpclient.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import com.google.gson.Gson;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;


public class SlackBot {
    AsyncHttpClient client;
    String apiAddr = "https://slack.com/api/";
    String token = "";
    String botUser = "";
    String connectionString = "";
    WebSocket ws;

    public SlackBot() throws Exception {
        client = new DefaultAsyncHttpClient();
        token = AppEnv.get("BOT_TOKEN");
        botUser = AppEnv.get("BOT_USER");
        connectionString = this.getConnectionString();
    }

    public String getConnectionString() throws Exception {
        Response res = makeApiRequest("rtm.connect");
        Gson gson = new Gson();
        GsonConnectionRes resData = gson.fromJson(res.getResponseBody(), GsonConnectionRes.class);
        return resData.url;
    }

    public void initWS() throws Exception {
        ws = this.client.prepareGet(this.connectionString).execute(new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(new WSListener(this)).build()).get();
    }

    public Response makeApiRequest(String path) throws Exception {
        return makeApiRequest(path, null);
    }

    public Response makeApiRequest(String path, HashMap<String, String> query) throws Exception {
        String queryString;
        if (query != null) {
            StringBuffer queryBuffer = new StringBuffer();
            for (String key : query.keySet()) {
                queryBuffer.append("&").append(key).append("=").append(query.get(key));
            }
            queryString = queryBuffer.toString();
        }
        else queryString = "";
        String link = apiAddr + path + "?token=" + token + queryString;
        Future<Response> future = client.prepareGet(link).execute();
////      We can manually catch an exception:
//        Response res;
//        try {
//            res = future.get();
//        } catch(Exception e) {
////            throw new Exception("Unsuccsessfull request.");
//            return "";
//        }
////      Or we can rely on `throws Exception` statement
        Response res = future.get();
        return res;
    }
}

class WSListener implements WebSocketListener {
    Gson gson;
    SlackBot caller;
    WebSocket ws;
    
    public WSListener(SlackBot pmCaller) {
        gson = new Gson();
        caller = pmCaller;
    }
    
    public void onTextFrame(String payload, boolean finalFragment, int rsv) {
        System.out.println("Frame: " + payload);
        String type = gson.fromJson(payload, GsonSlackMessageType.class).type;
        switch (type) {
            case "message":
                try {
                    this.onMessage(payload);
                } catch (Exception e) {
                    System.out.println("Error while sending request message." + payload);
                }
                break;
            default:
                System.out.println("Other frame: " + payload);
        }
    }

    public void onOpen(WebSocket webSocket) {
        System.out.println("Open.");
        ws = webSocket;
    }

    public void onClose(WebSocket webSocket, int i, String s) {
        System.out.println("Closed.");
    }

    public void onError(Throwable throwable) {
        System.out.println("Error: " + throwable.getMessage());
    }

    private void onMessage(String payload) throws Exception {
        GsonSlackMessage message = gson.fromJson(payload, GsonSlackMessage.class);
        // If message from bot itself, ignore it:
        if (message.user.equals(caller.botUser)) return;
        System.out.println("Message: " + message.text);
        String userName = getUser(message.user).user.profile.real_name;
        if (userName.isEmpty()) userName = "неизвестный";
        GsonSlackMessage newMessage = new GsonSlackMessage();
        newMessage.user = caller.botUser;
        newMessage.text = "Привет, " + userName + "!";
        newMessage.channel = message.channel;
        String newMsgJson = gson.toJson(newMessage);
        ws.sendTextFrame(newMsgJson);
    }

    private GsonSlackUser getUser(String userId) throws Exception {
        HashMap<String, String> queryMap = new HashMap<String, String>() {{
            // Read more about this method of array initialization:
            // http://wiki.c2.com/?DoubleBraceInitialization
            put("user", userId);
        }};
        Response res = caller.makeApiRequest("users.info", queryMap);
        GsonSlackUser user = gson.fromJson(res.getResponseBody(), GsonSlackUser.class);
        return user;
    }
}

class GsonConnectionRes {
    String url = "";
}

class GsonSlackMessageType {
    String type = "";
}

class GsonSlackMessage {
    String type = "message";
    String channel = "";
    String user = "";
    String text = "";
    String ts = "";
    String source_team = "";
    String team = "";
}

class GsonSlackUser {
    class User {
        class Profile {
            String real_name = "";
        }

        Profile profile;
        String id = "";
    }

    User user;
}
