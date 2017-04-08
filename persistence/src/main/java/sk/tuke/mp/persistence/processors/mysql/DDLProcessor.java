package sk.tuke.mp.persistence.processors.mysql;

import sk.tuke.mp.persistence.processors.JpaProcessor;
import sk.tuke.mp.persistence.processors.ProcessingException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.persistence.Entity;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;


@SupportedAnnotationTypes("javax.persistence.Entity")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DDLProcessor extends AbstractProcessor {

  private JpaProcessor entityProcessor;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
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
        .collect(Collectors.joining(";\n\n"));
      writeSqlToFile(sql);
    } catch (ProcessingException e) {
      printProcessingError(e);
      return false;
    } catch (IOException e) {
      printWarning(e);
    }
    return true;
  }

  private void writeSqlToFile(String sql) throws IOException {
    FileObject fo = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "ddl.sql");
    try (Writer writer = fo.openWriter()) {
      writer.append(sql);
    }
  }

  private void printProcessingError(ProcessingException e) {
    processingEnv.getMessager().printMessage(ERROR, e.getMessage(), e.element());
  }

  private void printWarning(Exception e) {
    processingEnv.getMessager().printMessage(WARNING, "Exception occured while generating ddl: " + e.getMessage());
  }
}
