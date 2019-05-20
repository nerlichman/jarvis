package edu.uoc.som.jarvis.plugins.slack.platform.io;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.oauth.OAuthAccessRequest;
import com.github.seratch.jslack.api.methods.response.oauth.OAuthAccessResponse;
import com.google.gson.JsonElement;
import edu.uoc.som.jarvis.core.server.JsonRestHandler;
import edu.uoc.som.jarvis.plugins.slack.SlackUtils;
import edu.uoc.som.jarvis.plugins.slack.platform.SlackPlatform;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;
import org.apache.http.NameValuePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class SlackOAuthRestHandler implements JsonRestHandler {

    private SlackPlatform slackPlatform;

    private String clientId;

    private String clientSecret;

    public SlackOAuthRestHandler(SlackPlatform slackPlatform, String clientId, String clientSecret) {
        this.slackPlatform = slackPlatform;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public JsonElement handle(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params, @Nullable JsonElement content) {
        if(isNull(clientId) ||isNull(clientSecret)) {
            Log.info("Cannot handle the request: the provided configuration doesn't define a Slack App client ID and " +
                    "client secret");
        }
        String code = null;
        for(NameValuePair parameter : params) {
            if(parameter.getName().equals("code")) {
                code = parameter.getValue();
            }
        }
        OAuthAccessResponse oAuthAccessResponse = null;
        try {
            oAuthAccessResponse = new Slack().methods().oauthAccess(OAuthAccessRequest.builder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .code(code)
                    .build());
        } catch(SlackApiException | IOException e) {
            e.printStackTrace();
        }
        Log.info("Got access token! {0}", oAuthAccessResponse.getAccessToken());
        if(nonNull(oAuthAccessResponse.getBot()) && nonNull(oAuthAccessResponse.getBot().getBotAccessToken())) {
            slackPlatform.registerTeamToken(oAuthAccessResponse.getTeamId(),
                    oAuthAccessResponse.getBot().getBotAccessToken());
            Log.info("Got bot access token! {0}", oAuthAccessResponse.getBot().getBotAccessToken());
            Configuration configuration = new BaseConfiguration();
            configuration.addProperty(SlackUtils.SLACK_TOKEN_KEY, oAuthAccessResponse.getBot().getBotAccessToken());
            SlackIntentProvider slackIntentProvider = new SlackIntentProvider(slackPlatform, configuration);
            slackPlatform.startEventProvider(slackIntentProvider);
        }
        return null;
    }
}
