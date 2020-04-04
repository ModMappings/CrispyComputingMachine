package org.modmappings.crispycomputingmachine.utils;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodDesc {

    private final List<String> args = Lists.newArrayList();
    private final String returnType;

    public MethodDesc(final String desc) {

        int cursor = 0;
        char c;

        if ((c = desc.charAt(cursor++)) != '(') {
            throw new IllegalArgumentException(desc);
        }
        StringBuilder argBuilder = new StringBuilder();

        while ((c = desc.charAt(cursor++)) != ')') {
            switch (c) {
                case 'V':
                case 'I':
                case 'C':
                case 'Z':
                case 'D':
                case 'F':
                case 'J':
                case 'B':
                case 'S':
                    argBuilder.append(c);
                    break;
                case '[':
                    argBuilder.append(c);
                    continue;
                case 'L':
                    while (true) {
                        argBuilder.append(c);
                        if (c == ';') {
                            break;
                        }
                        c = desc.charAt(cursor++);
                    }
                    break;
                default:
                    throw new IllegalArgumentException(desc);
            }

            args.add(argBuilder.toString());
            argBuilder.setLength(0);
        }

        returnType = desc.substring(cursor);
    }

    public List<String> getArgs() {
        return args;
    }

    public String getReturnType() {
        return returnType;
    }
}
