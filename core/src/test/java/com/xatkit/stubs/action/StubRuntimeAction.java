package com.xatkit.stubs.action;

import com.xatkit.core.platform.RuntimePlatform;
import com.xatkit.core.platform.action.RuntimeAction;
import com.xatkit.core.session.JarvisSession;

public class StubRuntimeAction extends RuntimeAction {

    private boolean actionProcessed;

    public StubRuntimeAction(RuntimePlatform runtimePlatform) {
        super(runtimePlatform, new JarvisSession("id"));
    }

    public boolean isActionProcessed() {
        return actionProcessed;
    }

    @Override
    public Object compute() {
        this.actionProcessed = true;
        return null;
    }
}
