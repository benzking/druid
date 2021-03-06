/*
 * Copyright 1999-2011 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.druid.sql.dialect.oracle.parser;

import static com.alibaba.druid.sql.parser.CharTypes.isIdentifierChar;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.druid.sql.parser.Keywords;
import com.alibaba.druid.sql.parser.Lexer;
import com.alibaba.druid.sql.parser.NotAllowCommentException;
import com.alibaba.druid.sql.parser.SQLParseException;
import com.alibaba.druid.sql.parser.Token;

import static com.alibaba.druid.sql.parser.LayoutCharacters.EOI;

public class OracleLexer extends Lexer {

    public final static Keywords DEFAULT_ORACLE_KEYWORDS;

    static {
        Map<String, Token> map = new HashMap<String, Token>();
        
        map.putAll(Keywords.DEFAULT_KEYWORDS.getKeywords());

        map.put("BEGIN", Token.BEGIN);
        map.put("COLUMN", Token.COLUMN);
        map.put("COMMENT", Token.COMMENT);
        map.put("COMMIT", Token.COMMIT);
        map.put("CONNECT", Token.CONNECT);

        map.put("CROSS", Token.CROSS);
        map.put("CURSOR", Token.CURSOR);
        map.put("DECLARE", Token.DECLARE);
        map.put("ERRORS", Token.ERRORS);
        map.put("EXCEPTION", Token.EXCEPTION);

        map.put("EXCLUSIVE", Token.EXCLUSIVE);
        map.put("EXTRACT", Token.EXTRACT);
        map.put("GOTO", Token.GOTO);
        map.put("GRANT", Token.GRANT);
        map.put("IF", Token.IF);

        map.put("LIMIT", Token.LIMIT);
        map.put("LOCAL", Token.LOCAL);
        map.put("LOOP", Token.LOOP);
        map.put("MATCHED", Token.MATCHED);
        map.put("MERGE", Token.MERGE);

        map.put("MODE", Token.MODE);
        map.put("MODEL", Token.MODEL);
        map.put("NOWAIT", Token.NOWAIT);
        map.put("OF", Token.OF);
        map.put("PRIOR", Token.PRIOR);

        map.put("PROCEDURE", Token.PROCEDURE);
        map.put("REJECT", Token.REJECT);
        map.put("RETURNING", Token.RETURNING);
        map.put("SAVEPOINT", Token.SAVEPOINT);
        map.put("SESSION", Token.SESSION);

        map.put("SHARE", Token.SHARE);
        map.put("START", Token.START);
        map.put("SYSDATE", Token.SYSDATE);
        map.put("UNLIMITED", Token.UNLIMITED);
        map.put("USING", Token.USING);

        map.put("WAIT", Token.WAIT);
        map.put("WITH", Token.WITH);

        DEFAULT_ORACLE_KEYWORDS = new Keywords(map);
    }

    public OracleLexer(char[] input, int inputLength, boolean skipComment){
        super(input, inputLength, skipComment);
        super.keywods = DEFAULT_ORACLE_KEYWORDS;
    }

    public OracleLexer(String input){
        super(input);
        super.keywods = DEFAULT_ORACLE_KEYWORDS;
    }

    public void scanVariable() {
        if (ch == '@') {
            scanChar();
            token = Token.MONKEYS_AT;
            return;
        }

        if (ch != ':' && ch != '#' && ch != '$') {
            throw new SQLParseException("illegal variable");
        }

        mark = pos;
        bufPos = 1;
        char ch;

        boolean quoteFlag = false;
        boolean mybatisFlag = false;
        if (charAt(pos + 1) == '"') {
            pos++;
            bufPos++;
            quoteFlag = true;
        } else if (charAt(pos + 1) == '{') {
            pos++;
            bufPos++;
            mybatisFlag = true;
        }
        
        for (;;) {
            ch = charAt(++pos);

            if (!isIdentifierChar(ch)) {
                break;
            }

            bufPos++;
            continue;
        }
        
        if (quoteFlag) {
            if (ch != '"') {
                throw new SQLParseException("syntax error");
            }
            ++pos;
            bufPos++;
        } else if (mybatisFlag) {
            if (ch != '}') {
                throw new SQLParseException("syntax error");
            }
            ++pos;
            bufPos++;
        }

        this.ch = charAt(pos);

        stringVal = addSymbol();
        Token tok = keywods.getKeyword(stringVal);
        if (tok != null) {
            token = tok;
        } else {
            token = Token.VARIANT;
        }
    }

    public void scanComment() {
        if (ch != '/' && ch != '-') {
            throw new IllegalStateException();
        }

        mark = pos;
        bufPos = 0;
        scanChar();

        // /*+ */
        if (ch == '*') {
            scanChar();
            bufPos++;

            while (ch == ' ') {
                scanChar();
                bufPos++;
            }

            boolean isHint = false;
            int startHintSp = bufPos + 1;
            if (ch == '+') {
                isHint = true;
                scanChar();
                bufPos++;
            }

            for (;;) {
                if (ch == '*' && charAt(pos + 1) == '/') {
                    bufPos += 2;
                    scanChar();
                    scanChar();
                    break;
                }

                scanChar();
                bufPos++;
            }

            if (isHint) {
                stringVal = subString(mark + startHintSp, (bufPos - startHintSp) - 1);
                token = Token.HINT;
            } else {
                stringVal = subString(mark, bufPos);
                token = Token.MULTI_LINE_COMMENT;
            }

            if (token != Token.HINT && !isAllowComment()) {
                throw new NotAllowCommentException();
            }

            return;
        }

        if (!isAllowComment()) {
            throw new NotAllowCommentException();
        }

        if (ch == '/' || ch == '-') {
            scanChar();
            bufPos++;

            for (;;) {
                if (ch == '\r') {
                    if (charAt(pos + 1) == '\n') {
                        bufPos += 2;
                        scanChar();
                        break;
                    }
                    bufPos++;
                    break;
                } else if (ch == EOI) {
                    break;
                }

                if (ch == '\n') {
                    scanChar();
                    bufPos++;
                    break;
                }

                scanChar();
                bufPos++;
            }

            stringVal = subString(mark + 1, bufPos);
            token = Token.LINE_COMMENT;
            return;
        }
    }

    public void scanNumber() {
        mark = pos;

        if (ch == '-') {
            bufPos++;
            ch = charAt(++pos);
        }

        for (;;) {
            if (ch >= '0' && ch <= '9') {
                bufPos++;
            } else {
                break;
            }
            ch = charAt(++pos);
        }

        boolean isDouble = false;

        if (ch == '.') {
            if (charAt(pos + 1) == '.') {
                token = Token.LITERAL_INT;
                return;
            }
            bufPos++;
            ch = charAt(++pos);
            isDouble = true;

            for (;;) {
                if (ch >= '0' && ch <= '9') {
                    bufPos++;
                } else {
                    break;
                }
                ch = charAt(++pos);
            }
        }

        if (ch == 'e' || ch == 'E') {
            bufPos++;
            ch = charAt(++pos);

            if (ch == '+' || ch == '-') {
                bufPos++;
                ch = charAt(++pos);
            }

            for (;;) {
                if (ch >= '0' && ch <= '9') {
                    bufPos++;
                } else {
                    break;
                }
                ch = charAt(++pos);
            }

            isDouble = true;
        }

        if (ch == 'f' || ch == 'F') {
            token = Token.BINARY_FLOAT;
            scanChar();
            return;
        }

        if (ch == 'd' || ch == 'D') {
            token = Token.BINARY_DOUBLE;
            scanChar();
            return;
        }

        if (isDouble) {
            token = Token.LITERAL_FLOAT;
        } else {
            token = Token.LITERAL_INT;
        }
    }

}
