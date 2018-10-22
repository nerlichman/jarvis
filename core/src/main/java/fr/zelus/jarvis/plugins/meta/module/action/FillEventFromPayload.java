package fr.zelus.jarvis.plugins.meta.module.action;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.JarvisException;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.plugins.meta.module.MetaModule;

public class FillEventFromPayload extends JarvisAction<MetaModule> {

    private JsonParser parser;

    private JsonElement jsonPayload;

    private String eventName;

    private String outContextName;

    public FillEventFromPayload(MetaModule containingModule, JarvisSession session, String json,
                                String eventName, String outContextName) {
        super(containingModule, session);
        parser = new JsonParser();
        Log.info("Parsing JSON");
        Log.info("{0}", json);
        try {
            this.jsonPayload = parser.parse(json);
        } catch(JsonSyntaxException e) {
            throw new JarvisException("Cannot parse the provided JSON payload", e);
        }
        this.eventName = eventName;
        this.outContextName = outContextName;
    }

    @Override
    public Object compute() {
        EventDefinition eventDefinition = module.getRegisteredEventDefinition(eventName);
        JsonObject rootObject = jsonPayload.getAsJsonObject();
        for(String key : rootObject.keySet()) {
            Log.info("Found key: {0}", key);
        }
        return null;
    }
}
