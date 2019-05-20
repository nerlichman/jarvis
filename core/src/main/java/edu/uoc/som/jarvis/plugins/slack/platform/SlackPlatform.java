package edu.uoc.som.jarvis.plugins.slack.platform;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.team.TeamInfoRequest;
import com.github.seratch.jslack.api.methods.response.team.TeamInfoResponse;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.JarvisException;
import edu.uoc.som.jarvis.core.platform.RuntimePlatform;
import edu.uoc.som.jarvis.core.platform.action.RuntimeAction;
import edu.uoc.som.jarvis.core.session.JarvisSession;
import edu.uoc.som.jarvis.plugins.chat.platform.ChatPlatform;
import edu.uoc.som.jarvis.plugins.slack.SlackUtils;
import edu.uoc.som.jarvis.plugins.slack.platform.action.PostMessage;
import edu.uoc.som.jarvis.plugins.slack.platform.io.SlackOAuthRestHandler;
import org.apache.commons.configuration2.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.nonNull;

/**
 * A {@link RuntimePlatform} class that connects and interacts with the Slack API.
 * <p>
 * This runtimePlatform manages a connection to the Slack API, and provides a set of
 * {@link RuntimeAction}s to interact with the Slack API:
 * <ul>
 * <li>{@link edu.uoc.som.jarvis.plugins.slack.platform.action.Reply}: reply to a user input</li>
 * <li>{@link PostMessage}: post a message to a given Slack channel</li>
 * </ul>
 * <p>
 * This class is part of jarvis' core platform, and can be used in an execution model by importing the
 * <i>SlackPlatform</i> package.
 */
public class SlackPlatform extends ChatPlatform {

    /**
     * The {@link Map} storing the registered Slack bot API tokens.
     * <p>
     * This {@link Map} is populated with a single entry from the provided {@link Configuration} if the bot is
     * designed to be installed in a single Slack workspace, or from the {@link #registerTeamToken(String, String)}
     * method when receiving new installation events.
     *
     * @see #getSlackToken(String)
     * @see #SlackPlatform(JarvisCore, Configuration)
     */
    private Map<String, String> slackTokens;

    /**
     * The {@link Slack} API client used to post messages.
     */
    private Slack slack;

    /**
     * Constructs a new {@link SlackPlatform} from the provided {@link JarvisCore} and {@link Configuration}.
     * <p>
     * The provided {@link Configuration} can contain a Slack bot token used to setup an initial Slack team and
     * access token. This is used in development environments to quickly prototype bots. Note that the
     * {@link SlackPlatform} accepts multiple access tokens that can be registered through the
     * {@link #registerTeamToken(String, String)} method.
     *
     * @param jarvisCore    the {@link JarvisCore} instance associated to this runtimePlatform
     * @param configuration the {@link Configuration} used to initialize the {@link SlackPlatform}
     * @throws NullPointerException if the provided {@code jarvisCore} or {@code configuration} is {@code null}
     * @throws JarvisException      if the provided {@code configuration} contains an invalid Slack bot token
     */
    public SlackPlatform(JarvisCore jarvisCore, Configuration configuration) {
        super(jarvisCore, configuration);
        slackTokens = new HashMap<>();
        slack = new Slack();
        String localSlackToken = configuration.getString(SlackUtils.SLACK_TOKEN_KEY);
        if (nonNull(localSlackToken)) {
            try {
                TeamInfoResponse teamInfoResponse =
                        slack.methods().teamInfo(TeamInfoRequest.builder().token(localSlackToken).build());
                String teamId = teamInfoResponse.getTeam().getId();
                this.registerTeamToken(teamId, localSlackToken);
            } catch (SlackApiException | IOException e) {
                throw new JarvisException("Cannot access the slack team from the provided token, see attached " +
                        "exception", e);
            }
        }
        String clientId = configuration.getString(SlackUtils.SLACK_CLIENT_ID);
        String clientSecret = configuration.getString(SlackUtils.SLACK_CLIENT_SECRET);

        this.jarvisCore.getJarvisServer().registerRestEndpoint("/slack", new SlackOAuthRestHandler(this, clientId,
                clientSecret));
    }

    /**
     * Returns the Slack bot API token associated to the provided {@code teamId}.
     *
     * @return the Slack bot API token associated to the provided {@code teamId} if it exists, {@code null} otherwise
     */
    public String getSlackToken(String teamId) {
        return slackTokens.get(teamId);
    }

    /**
     * Returns the Slack API client used to post messages.
     *
     * @return the Slack API client used to post messages
     */
    public Slack getSlack() {
        return slack;
    }

    /**
     * Registers the provided {@code token} used to access the given {@code teamId}.
     *
     * @param teamId the identifier of the team that can be accessed by the provided {@code token}
     * @param token  the Slack access token
     * @see #getSlackToken(String)
     */
    public void registerTeamToken(String teamId, String token) {
        this.slackTokens.put(teamId, token);
    }

    /**
     * Returns the {@link JarvisSession} associated to the provided {@code channel}.
     *
     * @param channel the {@code channel} identifier to retrieve the {@link JarvisSession} from
     * @return the {@link JarvisSession} associated to the provided {@code channel}
     */
    public JarvisSession createSessionFromChannel(String channel) {
        return this.jarvisCore.getOrCreateJarvisSession(channel);
    }
}
