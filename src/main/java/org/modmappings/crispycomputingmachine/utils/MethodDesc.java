package org.modmappings.crispycomputingmachine.utils;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MethodDesc {

    private static final Logger LOGGER = LogManager.getLogger();

    private final List<String> args;
    private final String       returnType;

    public MethodDesc(final List<String> args, final String returnType) {
        this.args = args;
        this.returnType = returnType;
    }

    public MethodDesc(final String desc) {
        args = Lists.newArrayList();

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

    @Override
    public String toString() {
        return "("
                               + String.join("", getArgs())
                               + ")"
                               + getReturnType();
    }

    public MethodDesc remap(final Function<String, Optional<String>> remappingFunction) {
        final List<String> remappedArgs =
                        getArgs().stream().map(RemappableType::new).map(r -> r.remap(remappingFunction)).map(RemappableType::getType).collect(Collectors.toList());
        final String remappedReturnType = new RemappableType(getReturnType()).remap(remappingFunction).getType();
        return new MethodDesc(remappedArgs, remappedReturnType);
    }
}
