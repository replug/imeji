/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo.predefinedMetadata;

import java.net.URI;

import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.j2j.annotations.j2jDataType;
import de.mpg.j2j.annotations.j2jId;
import de.mpg.j2j.annotations.j2jResource;

@j2jResource("http://imeji.org/terms/metadata")
@j2jDataType("http://imeji.org/terms/metadata#conePerson")
@j2jId(getMethod = "getId", setMethod = "setId")
public class ConePerson extends Metadata
{
    @j2jResource("http://xmlns.com/foaf/0.1/person")
    private Person person;
    @j2jResource("http://imeji.org/terms/coneId")
    private URI coneId;
    @j2jResource("http://imeji.org/terms/statement")
    private URI statement;

    public ConePerson()
    {
    }

    public ConePerson(Person pers)
    {
        this.person = pers;
    }

    public Person getPerson()
    {
        return person;
    }

    public void setPerson(Person person)
    {
        this.person = person;
    }

    public URI getConeId()
    {
        return coneId;
    }

    public void setConeId(URI coneId)
    {
        this.coneId = coneId;
    }

    @Override
    public URI getStatement()
    {
        return statement;
    }

    @Override
    public void setStatement(URI namespace)
    {
        this.statement = namespace;
    }

    @Override
    public void copy(Metadata metadata)
    {
        if (metadata instanceof ConePerson)
        {
            this.person = ((ConePerson)metadata).getPerson();
            this.coneId = ((ConePerson)metadata).getConeId();
            this.statement = metadata.getStatement();
        }
    }

    @Override
    public String asFulltext()
    {
        return person.AsFullText();
    }
}
