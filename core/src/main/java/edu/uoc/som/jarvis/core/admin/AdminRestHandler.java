package edu.uoc.som.jarvis.core.admin;

import edu.uoc.som.jarvis.core.JarvisCore;
import edu.uoc.som.jarvis.core.server.JsonRestHandler;

public abstract class AdminRestHandler implements JsonRestHandler {

    protected JarvisCore jarvisCore;

    public AdminRestHandler(JarvisCore jarvisCore) {
        this.jarvisCore = jarvisCore;
    }
}
