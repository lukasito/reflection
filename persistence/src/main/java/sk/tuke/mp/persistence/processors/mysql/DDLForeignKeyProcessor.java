package sk.tuke.mp.persistence.processors.mysql;

import sk.tuke.mp.persistence.processors.CompileTimeProcessingException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.tools.Diagnostic;

class DDLForeignKeyProcessor implements MysqlCompileTimeJpaProcessor {

  private final ProcessingEnvironment processingEnvironment;

  DDLForeignKeyProcessor(ProcessingEnvironment processingEnvironment) {
    this.processingEnvironment = processingEnvironment;
  }

  @Override
  public String apply(Element element) {
    ManyToOne manyToOne = element.getAnnotation(ManyToOne.class);
    if (manyToOne == null) {
      return EMPTY_RESULT;
    } else {
      validate(element);
      String name = element.getAnnotation(JoinColumn.class).name();
      String foreignEntityName = getTargetEntityName(manyToOne);
      String tableName = element.getEnclosingElement().getAnnotation(Entity.class).name();
      return String.format("ALTER TABLE %s ADD CONSTRAINT fk_%s FOREIGN KEY (%s) REFERENCES %s(%s)",
        normalize(tableName),
        name,
        normalize(name),
        normalize(foreignEntityName),
        normalize("ID")
      );
    }
  }

  private void validate(Element element) {
    ManyToOne manyToOne = element.getAnnotation(ManyToOne.class);
    if (isTargetEntityUndefined(manyToOne)) {
      throw new CompileTimeProcessingException("@ManyToOne required targetEntity to be set!", element);
    }

    String foreignEntityName = getTargetEntityName(manyToOne);

    if (foreignEntityName != null && foreignEntityName.isEmpty()) {
      throw new CompileTimeProcessingException("@ManyToOne.targetEntity is not @Entity with name()", element);
    }

    JoinColumn joinColumn = element.getAnnotation(JoinColumn.class);
    if (joinColumn == null) {
      throw new CompileTimeProcessingException("@JoinColumn annotation missing on @ManyToOne entity", element);
    }

    String name = joinColumn.name();
    if (name.isEmpty()) {
      throw new CompileTimeProcessingException("@JoinColumn.name needs to be specified!", element);
    }
  }

  private boolean isTargetEntityUndefined(ManyToOne manyToOne) {
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
