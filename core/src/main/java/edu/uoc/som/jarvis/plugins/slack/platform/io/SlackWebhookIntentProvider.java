package edu.uoc.som.jarvis.plugins.slack.platform.io;

import com.google.gson.JsonElement;
import edu.uoc.som.jarvis.core.platform.io.EventInstanceBuilder;
import edu.uoc.som.jarvis.core.platform.io.JsonEventMatcher;
import edu.uoc.som.jarvis.core.platform.io.JsonWebhookEventProvider;
import edu.uoc.som.jarvis.plugins.slack.platform.SlackPlatform;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.Header;

public class SlackWebhookIntentProvider extends JsonWebhookEventProvider<SlackPlatform> {

    public SlackWebhookIntentProvider(SlackPlatform runtimePlatform, Configuration configuration) {
        super(runtimePlatform, configuration);
    }

    @Override
    protected void handleParsedContent(JsonElement parsedContent, Header[] headers) {

    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait();
            } catch(InterruptedException e) {

            }
        }
    }
}
