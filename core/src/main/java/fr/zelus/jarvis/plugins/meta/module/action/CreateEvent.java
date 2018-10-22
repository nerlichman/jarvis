package fr.zelus.jarvis.plugins.meta.module.action;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.intent.IntentFactory;
import fr.zelus.jarvis.module.EventProviderDefinition;
import fr.zelus.jarvis.plugins.meta.module.MetaModule;

import static java.util.Objects.nonNull;

public class CreateEvent extends JarvisAction<MetaModule> {

    private String eventName;

    private String eventProviderName;

    public CreateEvent(MetaModule containingModule, JarvisSession session, String eventName, String eventProviderName) {
        super(containingModule, session);
        this.eventName = eventName;
        this.eventProviderName = eventProviderName;
    }

    @Override
    public Object compute() {
        Log.info("Creating EventDefinition {0} in EventProvider {1}", eventName, eventProviderName);
        EventDefinition eventDefinition = IntentFactory.eINSTANCE.createEventDefinition();
        eventDefinition.setName(eventName);
        EventProviderDefinition eventProviderDefinition = this.module.getRegisteredEventProvider(eventProviderName);
        if(nonNull(eventProviderDefinition)) {
            this.module.registerEventDefinition(eventDefinition);
            eventProviderDefinition.getEventDefinitions().add(eventDefinition);
        } else {
            Log.error("Cannot retrieve the EventProvider {0}", eventProviderName);
        }
        return null;
    }
}
