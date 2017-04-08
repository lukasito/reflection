package sk.tuke.mp.persistence.processors.mysql;

import sk.tuke.mp.persistence.processors.CompileTimeJpaProcessor;

public interface MysqlCompileTimeJpaProcessor extends CompileTimeJpaProcessor {

  String ESCAPING_CHAR = "`";

  default String normalize(String var) {
    return (ESCAPING_CHAR + var + ESCAPING_CHAR).toUpperCase();
  }
}
