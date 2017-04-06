package sk.tuke.mp.persistence.processors;

import javax.lang.model.element.Element;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Optional;

public class ConstraintProcessor implements JpaProcessor {

  @Override
  public String apply(Element element) {
    return processConstraints(element);
  }

  private String processConstraints(Element element) {
    return processPrimaryKey(element)
      .orElse("");
  }

  private Optional<String> processPrimaryKey(Element element) {
    Id id = element.getAnnotation(Id.class);
    if (id == null) {
      return processForeignKey(element);
    } else {
      return Optional.of("PRIMARY KEY (id)");
    }
  }

  private Optional<String> processForeignKey(Element element) {
    ManyToOne manyToOne = element.getAnnotation(ManyToOne.class);
    if (manyToOne == null) {
      return Optional.empty();
    } else {
      validate(element);
      String name = element.getAnnotation(JoinColumn.class).name();
      String foreignEntityName = manyToOne.targetEntity().<Entity>getDeclaredAnnotation(Entity.class).name();
      return Optional.of("FOREIGN KEY (" + name + ")" + " REFERENCES " + foreignEntityName + "(id)");
    }
  }

  private void validate(Element element) {
    ManyToOne manyToOne = element.getAnnotation(ManyToOne.class);
    if (manyToOne.targetEntity() == void.class) {
      throw new ProcessingException("@ManyToOne required targetEntity to be set!", element);
    }

    Entity foreignEntity = manyToOne.targetEntity().<Entity>getDeclaredAnnotation(Entity.class);
    if (foreignEntity.name().isEmpty()) {
      throw new ProcessingException("@ManyToOne.targetEntity is not @Entity with name()", element);
    }

    JoinColumn joinColumn = element.getAnnotation(JoinColumn.class);
    if (joinColumn == null) {
      throw new ProcessingException("@JoinColumn annotation missing on @ManyToOne entity", element);
    }

    String name = joinColumn.name();
    if (name.isEmpty()) {
      throw new ProcessingException("@JoinColumn.name needs to be specified!", element);
    }
  }
}
