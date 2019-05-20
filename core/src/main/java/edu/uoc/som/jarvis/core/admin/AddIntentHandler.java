package edu.uoc.som.jarvis.core.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.intent.IntentDefinition;
import edu.uoc.som.jarvis.intent.IntentFactory;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.Header;
import org.apache.http.NameValuePair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.nonNull;

public class AddIntentHandler extends AdminRestHandler {

    public AddIntentHandler(JarvisCore jarvisCore) {
        super(jarvisCore);
    }

    @Override
    public JsonElement handle(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                              @Nullable JsonElement content) {
        String message;
        if(nonNull(content)) {
            JsonObject contentObject = content.getAsJsonObject();
            String intentName = contentObject.get("name").getAsString();
            IntentDefinition intentDefinition = IntentFactory.eINSTANCE.createIntentDefinition();
            intentDefinition.setName(intentName);
            JsonArray inputArray = contentObject.get("inputs").getAsJsonArray();
            for(JsonElement input : inputArray) {
                intentDefinition.getTrainingSentences().add(input.getAsString());
            }
            this.jarvisCore.adminLibrary.getEventDefinitions().add(intentDefinition);
            Log.info("added intnet {0}", intentDefinition.getName());
            message = "Created intent " + intentDefinition.getName();
            try {
                this.jarvisCore.adminLibrary.eResource().save(Collections.emptyMap());
            } catch(IOException e) {
                Log.error("An error occurred when saving the admin resource");
            }
        } else {
            Log.error("Cannot perform addIntent: the provided JSON object is null");
            message = "Cannot perform addIntent, the provided JSON object is null";
        }
        JsonObject result = new JsonObject();
        result.add("message", new JsonPrimitive(message));
        return result;
    }
}
