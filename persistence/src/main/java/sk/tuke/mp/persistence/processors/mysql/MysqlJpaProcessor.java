package sk.tuke.mp.persistence.processors.mysql;

import sk.tuke.mp.persistence.processors.JpaProcessor;

public interface MysqlJpaProcessor extends JpaProcessor {

  String ESCAPING_CHAR = "`";

  default String normalize(String var) {
    return (ESCAPING_CHAR + var + ESCAPING_CHAR).toUpperCase();
  }
}
