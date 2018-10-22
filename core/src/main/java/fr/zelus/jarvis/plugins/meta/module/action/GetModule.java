package fr.zelus.jarvis.plugins.meta.module.action;

import fr.zelus.jarvis.core.JarvisAction;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.meta.module.MetaModule;

public class GetModule extends JarvisAction<MetaModule> {

    private String moduleName;

    public GetModule(MetaModule containingModule, JarvisSession session, String moduleName) {
        super(containingModule, session);
        this.moduleName = moduleName;
    }

    @Override
    public Object compute() {
        return module.getRegisteredModule(moduleName);
    }
}
