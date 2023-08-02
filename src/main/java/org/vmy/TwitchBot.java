package org.vmy;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;

public class TwitchBot {

    private static TwitchBot singleton;
    TwitchClient twitchClient = null;

    private TwitchBot() {
    }

    public static TwitchBot getSingletonInstance() {
        if (singleton == null) {
            singleton = new TwitchBot();
        }
        return singleton;
    }

    public static void main(final String[] args) throws Exception {
        final TwitchBot bot = TwitchBot.getSingletonInstance();
        bot.sendMessage("Twitch bot sending a test message.");
    }

    protected void sendMessage(final String msg) throws Exception {
        final String channelName = Parameters.getInstance().twitchChannelName;
        final String accessToken = Parameters.getInstance().twitchBotToken;

        if ("".equals(channelName) || "".equals(accessToken)) {
            return;
        }

        try {
            if (twitchClient == null) {
                twitchClient = TwitchClientBuilder.builder()
                  .withEnableHelix(false)
                  .withEnableChat(true)
                  .withChatAccount(new OAuth2Credential("twitch", accessToken))
                  .withDefaultAuthToken(new OAuth2Credential("twitch", accessToken))
                  .build();
            }
            twitchClient.getChat().sendMessage(channelName, msg);
            System.out.println("Twitch msg sent to " + channelName + ".");
            Thread.sleep(3000);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    protected void finalize() {
        if (twitchClient != null) {
            twitchClient.close();
        }
    }
}
