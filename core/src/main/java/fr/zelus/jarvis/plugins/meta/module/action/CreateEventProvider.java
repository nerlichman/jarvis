package fr.zelus.jarvis.plugins.meta.module.action;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.module.EventProviderDefinition;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.plugins.meta.module.MetaModule;

import static java.util.Objects.nonNull;

public class CreateEventProvider extends JarvisAction<MetaModule> {

    private String eventProviderName;

    private String moduleName;

    public CreateEventProvider(MetaModule containingModule, JarvisSession session, String eventProviderName, String
            moduleName) {
        super(containingModule, session);
        this.eventProviderName = eventProviderName;
        this.moduleName = moduleName;
    }

    @Override
    public Object compute() {
        Log.info("Creating EventProvider {0} in Module {1}", eventProviderName, moduleName);
        EventProviderDefinition eventProviderDefinition = ModuleFactory.eINSTANCE.createEventProviderDefinition();
        eventProviderDefinition.setName(eventProviderName);
        Module module = this.module.getRegisteredModule(moduleName);
        if(nonNull(module)) {
            module.getEventProviderDefinitions().add(eventProviderDefinition);
            this.module.registerEventProvider(eventProviderDefinition);
        } else {
            Log.error("Cannot retrieve the Module {0}", moduleName);
        }
        return null;
    }
}
