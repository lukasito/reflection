package sk.tuke.mp.persistence.processors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.persistence.Entity;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.stream.Collectors;


@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DDLProcessor extends AbstractProcessor {

  private final JpaProcessor entityProcessor;

  public DDLProcessor() {
    entityProcessor = new DDLEntityProcessor(
      new DDLColumnProcessor(),
      new DDLConstraintProcessor(processingEnv)
    );
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
      String sql = roundEnv.getElementsAnnotatedWith(Entity.class).stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(entityProcessor)
        .collect(Collectors.joining(";\n"));
      writeSqlToFile(sql);
    } catch (ProcessingException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), e.element());
      return false;
    }
    return true;
  }

  private void writeSqlToFile(String sql) {
    try {
      FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "ddl.sql");
      Writer writer = fo.openWriter();
      writer.append(sql);
      writer.close();
    } catch (IOException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "IO error while generating ddl");
    }
  }
}
