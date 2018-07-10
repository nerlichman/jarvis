package fr.zelus.jarvis.plugins.log.module.action;

import fr.inria.atlanmod.commons.log.Level;
import fr.zelus.jarvis.core.session.JarvisSession;
import fr.zelus.jarvis.plugins.log.module.LogModule;

/**
 * A {@link LogAction} that logs the provided message as an error.
 */
public class LogError extends LogAction {

    /**
     * Constructs a new {@link LogError} action from the provided {@code containingModule}, {@code session}, and {@code
     * message}.
     *
     * @param containingModule the {@link LogModule} containing this action
     * @param session          the {@link JarvisSession} associated to this action
     * @param message          the message to log
     * @throws NullPointerException if the provided {@code containingModule}, {@code session}, or {@code message} is
     * {@code null}
     */
    public LogError(LogModule containingModule, JarvisSession session, String message) {
        super(containingModule, session, message, Level.ERROR);
    }
}
