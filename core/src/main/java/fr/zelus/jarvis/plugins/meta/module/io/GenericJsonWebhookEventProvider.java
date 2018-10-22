package fr.zelus.jarvis.plugins.meta.module.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventInstance;
import fr.zelus.jarvis.io.EventInstanceBuilder;
import fr.zelus.jarvis.io.JsonWebhookEventProvider;
import fr.zelus.jarvis.plugins.meta.module.MetaModule;
import org.apache.http.Header;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GenericJsonWebhookEventProvider extends JsonWebhookEventProvider<MetaModule> {

    private static final String PAYLOAD_CONTEXT_KEY = "payload";

    private static final String HEADERS_CONTEXT_KEY = "headers";

    private Gson gson;

    public GenericJsonWebhookEventProvider(MetaModule containingModule) {
        super(containingModule);
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    protected void handleParsedContent(JsonElement parsedContent, Header[] headers) {
        // TODO remove this SessionID and think about a solution to create sessions that can be accessed by client
        // intents
        JarvisSession session = this.jarvisCore.getOrCreateJarvisSession("DBD0T4JNA");
        EventInstance eventInstance = EventInstanceBuilder.newBuilder(this.jarvisCore.getEventDefinitionRegistry())
                .setEventDefinitionName("Received_Request")
                .setOutContextValue(PAYLOAD_CONTEXT_KEY, gson.toJson(parsedContent))
                .setOutContextValue(HEADERS_CONTEXT_KEY, printHeaders(headers))
                .build();
        this.jarvisCore.getOrchestrationService().handleEventInstance(eventInstance, session);
    }

    private String printHeaders(Header[] headers) {
        List<String> toStringList = StreamSupport.stream(Arrays.asList(headers).spliterator(), false)
                .map(o -> o.getName() + " = " + o.getValue())
                .collect(Collectors.toList());
        return String.join("\n", toStringList);
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                wait();
            } catch (InterruptedException e) {

            }
        }
    }
}
