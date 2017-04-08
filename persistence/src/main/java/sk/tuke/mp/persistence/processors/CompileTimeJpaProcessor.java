package sk.tuke.mp.persistence.processors;

import javax.lang.model.element.Element;
import java.util.function.Function;
import java.util.function.Predicate;

public interface CompileTimeJpaProcessor extends Function<Element, String> {

  String EMPTY_RESULT = "";
  Predicate<String> notEmptyResult = string -> !string.isEmpty();

  /**
   * @param element entity or columns
   * @return generated sql
   * @throws CompileTimeProcessingException
   */
  @Override
  String apply(Element element);

  String normalize(String string);
}
