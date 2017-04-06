package sk.tuke.mp.persistence.processors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.tools.Diagnostic;
import java.util.Optional;

class DDLConstraintProcessor implements JpaProcessor {

  private final ProcessingEnvironment processingEnvironment;

  DDLConstraintProcessor(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

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
      String foreignEntityName = getTargetEntityName(manyToOne);
      return Optional.of("FOREIGN KEY (" + name + ")" + " REFERENCES " + foreignEntityName + "(id)");
    }
  }

  private void validate(Element element) {
    ManyToOne manyToOne = element.getAnnotation(ManyToOne.class);
    if (isTargetEntityDefined(manyToOne)) {
      throw new ProcessingException("@ManyToOne required targetEntity to be set!", element);
    }

    String foreignEntityName = getTargetEntityName(manyToOne);

    if (foreignEntityName.isEmpty()) {
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

  private boolean isTargetEntityDefined(ManyToOne manyToOne) {
    try {
      manyToOne.targetEntity();
    } catch (MirroredTypeException e) {
      TypeMirror typeMirror = processingEnvironment.getTypeUtils().asElement(e.getTypeMirror()).asType();
      return typeMirror.getKind() == TypeKind.VOID;
    }
    return false;
  }

  private String getTargetEntityName(ManyToOne manyToOne) {
    try {
      manyToOne.targetEntity();
      return null;
    } catch (MirroredTypeException e) {
      Element element = processingEnvironment.getTypeUtils().asElement(e.getTypeMirror());
      processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, "annotation:", element);
      Entity annotation = element.getAnnotation(Entity.class);
      processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE, "annotation:" + annotation, element);
      return annotation.name();
    }
  }
}
