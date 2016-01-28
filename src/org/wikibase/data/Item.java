package org.wikibase.data;

/**
 * A data type for a wikibase property representing another entity.
 * 
 * @author acstroe
 *
 */
public class Item extends WikibaseDataType {
    private Entity ent = null;

    public Entity getEnt() {
        return ent;
    }

    public Item(Entity ent) {
        super();
        this.ent = ent;
    }
}
