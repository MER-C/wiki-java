package org.wikibase.data;

/**
 * A Wikidata claim.
 * 
 * @author acstroe
 *
 */
public class Claim {

    private String id;
    private String type;
    private Rank rank;
    private Property property;

    private WikibaseDataType value;

    public WikibaseDataType getValue() {
        return value;
    }

    public void setValue(WikibaseDataType value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Claim other = (Claim) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Rank getRank() {
        return rank;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    @Override
    public String toString() {
        return "Claim [id=" + id + ", property=" + property + ", value=" + value + "]";
    }
}
