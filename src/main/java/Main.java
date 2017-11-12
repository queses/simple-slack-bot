public class Main {
    public static void main(String[] args) {
        SlackBot bot;
        try {
            bot = new SlackBot();
            bot.initWS();
        } catch (Exception e) {
            System.out.print(e.getMessage());
            return;
        }
    }
}
