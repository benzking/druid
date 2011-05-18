package com.alibaba.druid.sql.parser;

import junit.framework.TestCase;

public class SQLLexerTest2 extends TestCase {

    public void test_lexer() throws Exception {
        String sql = "SELECT * FROM T WHERE F1 = ? ORDER BY F2";
        SQLLexer lexer = new SQLLexer(sql);
        for (;;) {
            lexer.nextToken();
            Token tok = lexer.token();

            if (tok == Token.IDENTIFIER) {
                System.out.println(tok.name() + "\t\t" + lexer.stringVal);
            } else {
                System.out.println(tok.name() + "\t\t\t" + tok.name);
            }

            if (tok == Token.EOF) {
                break;
            }
        }
    }
}