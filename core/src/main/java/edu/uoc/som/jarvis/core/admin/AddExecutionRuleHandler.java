package edu.uoc.som.jarvis.core.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.uoc.som.jarvis.common.CommonFactory;
import edu.uoc.som.jarvis.common.StringLiteral;
import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.execution.ActionInstance;
import edu.uoc.som.jarvis.execution.ExecutionFactory;
import edu.uoc.som.jarvis.execution.ExecutionRule;
import edu.uoc.som.jarvis.execution.ParameterValue;
import edu.uoc.som.jarvis.intent.EventDefinition;
import edu.uoc.som.jarvis.platform.ActionDefinition;
import edu.uoc.som.jarvis.platform.Parameter;
import edu.uoc.som.jarvis.platform.PlatformDefinition;
import fr.inria.atlanmod.commons.log.Log;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static java.util.Objects.nonNull;

public class AddExecutionRuleHandler extends AdminRestHandler {

    public AddExecutionRuleHandler(JarvisCore jarvisCore) {
        super(jarvisCore);
    }

    @Override
    public JsonElement handle(@Nonnull List<Header> headers, @Nonnull List<NameValuePair> params,
                              @Nullable JsonElement content) {
        String message = "meh";
        if(nonNull(content)) {
            JsonObject contentObject = content.getAsJsonObject();
            ExecutionRule executionRule = ExecutionFactory.eINSTANCE.createExecutionRule();
            String eventName = contentObject.get("onEvent").getAsString();
            EventDefinition eventDefinition = null;
            ResourceSet rSet = this.jarvisCore.getExecutionResourceSet();
            for(Resource resource : rSet.getResources()) {
                Iterator<EObject> resourceContents = resource.getAllContents();
                while(resourceContents.hasNext()) {
                    EObject eObject = resourceContents.next();
                    if(eObject instanceof EventDefinition) {
                        if(((EventDefinition) eObject).getName().equals(eventName)) {
                            eventDefinition = (EventDefinition) eObject;
                        }
                    }
                }
            }
            if(nonNull(eventDefinition)) {
                JsonArray doArray = contentObject.getAsJsonArray("do");
                for(JsonElement doInstruction : doArray) {
                    JsonObject doInstructionObject = doInstruction.getAsJsonObject();
                    String platformName = doInstructionObject.get("platform").getAsString();
                    String actionName = doInstructionObject.get("action").getAsString();
                    PlatformDefinition platformDefinition;
                    ActionDefinition actionDefinition = null;
                    for(Resource resource : rSet.getResources()) {
                        if(resource.getContents().get(0) instanceof PlatformDefinition) {
                            PlatformDefinition resourcePlatform = (PlatformDefinition) resource.getContents().get(0);
                            if(resourcePlatform.getName().equals(platformName)) {
                                platformDefinition = resourcePlatform;
                                actionDefinition = resourcePlatform.getActions(actionName).get(0);
                            }
                        }
                    }
                    if(nonNull(actionDefinition)) {
                        ActionInstance actionInstance = ExecutionFactory.eINSTANCE.createActionInstance();
                        actionInstance.setAction(actionDefinition);
                        executionRule.getInstructions().add(actionInstance);
                        JsonArray parameterArray = doInstructionObject.getAsJsonArray("parameters");
                        for(JsonElement parameterElement : parameterArray) {
                            JsonObject parameterObject = parameterElement.getAsJsonObject();
                            String parameterName = parameterObject.get("name").getAsString();
                            String parameterValue = parameterObject.get("value").getAsString();
                            Parameter actionParameter =
                                    actionDefinition.getParameters().stream().filter(p -> p.getKey().equals(parameterName)).findAny().get();
                            ParameterValue execParameterValue = ExecutionFactory.eINSTANCE.createParameterValue();
                            execParameterValue.setParameter(actionParameter);
                            StringLiteral stringLiteral = CommonFactory.eINSTANCE.createStringLiteral();
                            stringLiteral.setValue(parameterValue);
                            execParameterValue.setExpression(stringLiteral);
                            actionInstance.getValues().add(execParameterValue);
                        }
                    } else {
                        Log.error("Cannot find the action {0}", actionName);
                    }
                }
            } else {
                Log.error("Cannot find the event {0}", eventName);
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
