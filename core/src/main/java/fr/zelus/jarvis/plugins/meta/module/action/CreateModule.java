package fr.zelus.jarvis.plugins.meta.module.action;

import fr.inria.atlanmod.commons.log.Log;
import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.module.Module;
import fr.zelus.jarvis.module.ModuleFactory;
import fr.zelus.jarvis.plugins.meta.module.MetaModule;

public class CreateModule extends JarvisAction<MetaModule> {

    private String moduleName;

    public CreateModule(MetaModule containingModule, JarvisSession session, String moduleName) {
        super(containingModule, session);
        this.moduleName = moduleName;
    }

    @Override
    public Object compute() {
        Log.info("Creating Module {0}", moduleName);
        Module module = ModuleFactory.eINSTANCE.createModule();
        module.setName(moduleName);
        this.module.registerModule(module);
        return null;
    }
}
