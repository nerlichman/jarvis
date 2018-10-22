package fr.zelus.jarvis.plugins.meta.module;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisCore;
import fr.zelus.jarvis.core.JarvisModule;
import fr.zelus.jarvis.intent.EventDefinition;
import fr.zelus.jarvis.module.EventProviderDefinition;
import fr.zelus.jarvis.module.Module;

import java.util.HashMap;
import java.util.Map;

public class MetaModule extends JarvisModule {

    private Map<String, Module> registeredModules;

    private Map<String, EventProviderDefinition> registeredEventProviders;

    private Map<String, EventDefinition> registeredEventDefinitions;

    public MetaModule(JarvisCore jarvisCore) {
        super(jarvisCore);
        this.registeredModules = new HashMap<>();
        this.registeredEventProviders = new HashMap<>();
        this.registeredEventDefinitions = new HashMap<>();
    }

    public void registerModule(Module module) {
        Log.info("Registering Module {0}", module.getName());
        this.registeredModules.put(module.getName(), module);
    }

    public Module getRegisteredModule(String moduleName) {
        return this.registeredModules.get(moduleName);
    }

    public void registerEventProvider(EventProviderDefinition eventProvider) {
        this.registeredEventProviders.put(eventProvider.getName(), eventProvider);
    }

    public EventProviderDefinition getRegisteredEventProvider(String eventProviderName) {
        return this.registeredEventProviders.get(eventProviderName);
    }

    public void registerEventDefinition(EventDefinition eventDefinition) {
        this.registeredEventDefinitions.put(eventDefinition.getName(), eventDefinition);
    }

    public EventDefinition getRegisteredEventDefinition(String eventDefinitionName) {
        return this.registeredEventDefinitions.get(eventDefinitionName);
    }
}
