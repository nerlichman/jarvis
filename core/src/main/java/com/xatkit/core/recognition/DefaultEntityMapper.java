package com.xatkit.core.recognition;


import com.xatkit.intent.EntityDefinition;

/**
 * An {@link EntityMapper} that uses RegExp to match system entities.
 * <p>
 * This class matches all the system entities using the same RegExp ((\D)+), meaning that system entities can only be
 * matched from a single word.
 * <p>
 * Custom entities can be registered using {@link #addEntityMapping(EntityDefinition, String)} and associated to the
 * RegExp pattern that will be used to retrieve them.
 */
public class DefaultEntityMapper extends EntityMapper {

    /**
     * Constructs a {@link DefaultEntityMapper} that uses RegExp to match system entities.
     */
    public DefaultEntityMapper() {
        super();
        this.setFallbackEntityMapping("(\\S)+");
    }
}
