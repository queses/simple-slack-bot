import org.asynchttpclient.*;

import java.awt.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.concurrent.Future;
import com.google.gson.Gson;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;


public class SlackBot {
    AsyncHttpClient client;
    WebSocket ws;
    WebSocketListener wsl;
    final Integer wsReconnectionStep = 1;
    final String apiAddr = "https://slack.com/api/";
    final String token;
    final String botUser;
    String connectionString = "";

    /**
     * Устанавливает шаблон сообщения.
     * {user} - сюда подставится имя пользователя
     */
    String textPattern = "Привет, {user}!";

    SlackBot() throws Exception {
        client = new DefaultAsyncHttpClient();
        token = AppEnv.get("BOT_TOKEN");
        botUser = AppEnv.get("BOT_USER");
        connectionString = this.getConnectionString();
        Timer timer = new Timer();
    }

    void initWS() throws Exception {
        wsl = new WSListener(this);
        ws = this.client.prepareGet(connectionString).execute(new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(wsl).build()).get();
    }

    void reconnectWS(String url) throws Exception {
        connectionString = url;
        ws.removeWebSocketListener(wsl);
        ws.sendCloseFrame();
        ws = null;
        initWS();
    }

    GsonSlackUser getUser(String userId) throws Exception {
        HashMap<String, String> queryMap = new HashMap<String, String>() {{
            // Read more about this method of array initialization:
            // http://wiki.c2.com/?DoubleBraceInitialization
            put("user", userId);
        }};
        Response res = makeApiRequest("users.info", queryMap);
        GsonSlackUser user = (new Gson()).fromJson(res.getResponseBody(), GsonSlackUser.class);
        return user;
    }

    private Response makeApiRequest(String path) throws Exception {
        return makeApiRequest(path, null);
    }

    private Response makeApiRequest(String path, HashMap<String, String> query) throws Exception {
        String queryString;
        if (query != null) {
            StringBuilder queryBuilder = new StringBuilder();
            for (String key : query.keySet()) {
                queryBuilder.append("&").append(key).append("=").append(query.get(key));
            }
            queryString = queryBuilder.toString();
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

    private String getConnectionString() throws Exception {
        Response res = makeApiRequest("rtm.connect");
        Gson gson = new Gson();
        GsonConnectionRes resData = gson.fromJson(res.getResponseBody(), GsonConnectionRes.class);
        return resData.url;
    }
}

class WSListener implements WebSocketListener {
    Gson gson;
    SlackBot caller;
    WebSocket ws;
    Integer reconnectFrameCounter = 0;
    
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
            case "reconnect_url":
                if (reconnectFrameCounter < caller.wsReconnectionStep) {
                    reconnectFrameCounter++;
                    break;
                }
                // else:
                try {
                    String url = gson.fromJson(payload, GsonConnectionRes.class).url;
                    caller.reconnectWS(url);
                } catch (Exception e) {
                    System.out.println("Error while reconnecting." + payload);
                }
                reconnectFrameCounter = 0;
                break;
            default:
                System.out.println("Other frame: " + payload);
        }
    }

    public void onOpen(WebSocket webSocket) {
        System.out.println("Open at " + (new java.util.Date()).toString());
        ws = webSocket;
    }

    public void onClose(WebSocket webSocket, int i, String s) {
        System.out.println("Closed at " + (new java.util.Date()).toString());
    }

    public void onError(Throwable throwable) {
        System.out.println("Error: " + throwable.getMessage());
    }

    private void onMessage(String payload) throws Exception {
        GsonSlackMessage message = gson.fromJson(payload, GsonSlackMessage.class);
        // If message from bot itself, ignore it:
        if (message.user.equals(caller.botUser)) return;
        System.out.println("Message: " + message.text);
        String userName = caller.getUser(message.user).user.profile.real_name;
        if (userName.isEmpty()) userName = "неизвестный";
        GsonSlackMessage newMessage = new GsonSlackMessage();
        newMessage.user = caller.botUser;
        newMessage.text = caller.textPattern.replace("{user}", userName);
        newMessage.channel = message.channel;
        String newMsgJson = gson.toJson(newMessage);
        ws.sendTextFrame(newMsgJson);
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
