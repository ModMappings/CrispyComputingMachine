package org.modmappings.crispycomputingmachine.model.mappingtoy;

import net.minecraftforge.lex.mappingtoy.Utils;
import org.modmappings.crispycomputingmachine.utils.MethodRef;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;

public class MappingToyJarMetaData {
    private static final Handle LAMBDA_METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);

    public interface IAccessible {
        int getAccess();

        default boolean isInterface() {
            return ((getAccess() & Opcodes.ACC_INTERFACE) != 0);
        }

        default boolean isAbstract() {
            return ((getAccess() & Opcodes.ACC_ABSTRACT) != 0);
        }

        default boolean isSynthetic() {
            return ((getAccess() & Opcodes.ACC_SYNTHETIC) != 0);
        }

        default boolean isAnnotation() {
            return ((getAccess() & Opcodes.ACC_ANNOTATION) != 0);
        }

        default boolean isEnum() {
            return ((getAccess() & Opcodes.ACC_ENUM) != 0);
        }

        default boolean isPackagePrivate() {
            return (getAccess() & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) == 0;
        }

        default boolean isPublic() {
            return (getAccess() & Opcodes.ACC_PUBLIC) != 0;
        }

        default boolean isPrivate() {
            return (getAccess() & Opcodes.ACC_PRIVATE) != 0;
        }

        default boolean isProtected() {
            return (getAccess() & Opcodes.ACC_PROTECTED) != 0;
        }

        default boolean isStatic() {
            return (getAccess() & Opcodes.ACC_STATIC) != 0;
        }

        default boolean isFinal() {
            return (getAccess() & Opcodes.ACC_FINAL) != 0;
        }
    }

    public class ClassInfo implements IAccessible {
        private final transient String name;
        private final String superName;
        private final List<String> interfaces;
        private final Integer access;
        private final String signature;
        private final Map<String, FieldInfo> fields;
        private final Map<String, MethodInfo> methods;
        private transient boolean resolved = false;

        private ClassInfo(ClassNode node) {
            this.name = node.name;
            this.superName = "java/lang/Object".equals(node.superName) ? null : node.superName;
            this.interfaces = node.interfaces != null && !node.interfaces.isEmpty() ? new ArrayList<>(node.interfaces) : null;
            this.access = node.access == 0 ? null : node.access;
            this.signature = node.signature;

            if (node.fields == null || node.fields.isEmpty()) {
                this.fields = null;
            } else {
                this.fields = new TreeMap<>();
                node.fields.stream().forEach(fld -> this.fields.put(fld.name, new FieldInfo(fld)));
            }

            if (node.methods == null || node.methods.isEmpty()) {
                this.methods = null;
            } else {
                //Gather Lambda methods so we can skip them in bouncers?
                Set<String> lambdas = new HashSet<>();
                for (MethodNode m : node.methods) {
                    for (AbstractInsnNode asn : (Iterable<AbstractInsnNode>)() -> m.instructions.iterator()) {
                        if (asn instanceof InvokeDynamicInsnNode) {
                            InvokeDynamicInsnNode idn = (InvokeDynamicInsnNode)asn;
                            if (LAMBDA_METAFACTORY.equals(idn.bsm) && idn.bsmArgs != null && idn.bsmArgs.length == 3 && idn.bsmArgs[1] instanceof Handle) {
                                Handle target = ((Handle)idn.bsmArgs[1]);
                                lambdas.add(target.getOwner() + '/' + target.getName() + target.getDesc());
                            }
                        }
                    }
                }

                this.methods = new TreeMap<>();
                node.methods.forEach(mtd -> {
                    String key = mtd.name + mtd.desc;
                    this.methods.put(key, new MethodInfo(mtd, lambdas.contains(this.name + '/' + key)));
                });
            }
        }

        public String getSuper() {
            return this.superName == null && !"java/lang/Object".equals(this.name) ? "java/lang/Object" : this.superName;
        }

        public String getName() {
            return name;
        }

        public List<String> getInterfaces() {
            return this.interfaces == null ? Collections.emptyList() : this.interfaces;
        }

        public String getSignature() {
            return this.signature == null ? "" : this.signature;
        }

        public Map<String, FieldInfo> getFields() {
            return this.fields == null ? Collections.emptyMap() : this.fields;
        }

        public Map<String, MethodInfo> getMethods() {
            return methods == null ? Collections.emptyMap() : this.methods;
        }

        public boolean isResolved() {
            return resolved;
        }

        @Override
        public int getAccess() {
            return access == null ? 0 : access;
        }

        @Override
        public String toString() {
            return Utils.getAccess(getAccess()) + ' ' + this.name;
        }

        public class FieldInfo implements IAccessible {
            private final transient String name;
            private final transient String desc;
            private final Integer access;
            private final String signature;
            private String force;

            private FieldInfo(FieldNode node) {
                this.name = node.name;
                this.desc = node.desc;
                this.access = node.access == 0 ? null : node.access;
                this.signature = node.signature;
            }

            public void forceName(String name) {
                this.force = name;
            }

            @Override
            public int getAccess() {
                return access == null ? 0 : access;
            }

            @Override
            public String toString() {
                return Utils.getAccess(getAccess()) + ' ' + this.desc + ' ' + this.name;
            }

            public String getSignature() {
                return this.signature == null ? "" : this.signature;
            }

            public String getForce() {
                return this.force == null ? "" : this.force;
            }
        }

        public class MethodInfo implements IAccessible {
            private final transient String name;
            private final transient String desc;
            private final Integer access;
            private final String signature;
            private final Bounce bouncer;
            private String force;
            private Set<MethodRef> overrides;

            private MethodInfo(MethodNode node, boolean lambda) {
                this.name = node.name;
                this.desc = node.desc;
                this.access = node.access == 0 ? null : node.access;
                this.signature = node.signature;

                Bounce bounce = null;
                if (!lambda && (node.access & (Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE)) != 0 && (node.access & Opcodes.ACC_STATIC) == 0) {
                    AbstractInsnNode start = node.instructions.getFirst();
                    if (start instanceof LabelNode && start.getNext() instanceof LineNumberNode)
                        start = start.getNext().getNext();

                    if (start instanceof VarInsnNode) {
                        VarInsnNode n = (VarInsnNode)start;
                        if (n.var == 0 && n.getOpcode() == Opcodes.ALOAD) {
                            AbstractInsnNode end = node.instructions.getLast();
                            if (end instanceof LabelNode)
                                end = end.getPrevious();

                            if (end.getOpcode() >= Opcodes.IRETURN && end.getOpcode() <= Opcodes.RETURN)
                                end = end.getPrevious();

                            if (end instanceof MethodInsnNode) {
                                Type[] args = Type.getArgumentTypes(node.desc);
                                int var = 1;
                                int index = 0;
                                start = start.getNext();
                                while (start != end) {
                                    if (start instanceof VarInsnNode) {
                                        if (((VarInsnNode)start).var != var || index + 1 > args.length) {
                                            //Arguments are switched around, so seems like lambda!
                                            end = null;
                                            break;
                                        }
                                        var += args[index++].getSize();
                                    } else if (start.getOpcode() == Opcodes.INSTANCEOF || start.getOpcode() == Opcodes.CHECKCAST) {
                                        //Valid!
                                    } else {
                                        // Anything else is invalid in a bouncer {As far as I know}, so we're most likely a lambda
                                        end = null;
                                        break;
                                    }
                                    start = start.getNext();
                                }

                                MethodInsnNode mtd = (MethodInsnNode)end;
                                if (end != null && mtd.owner.equals(this.name) && Type.getArgumentsAndReturnSizes(node.desc) == Type.getArgumentsAndReturnSizes(mtd.desc))
                                    bounce = new Bounce(new MethodRef(mtd.owner, mtd.name, mtd.desc));
                            }
                        }
                    }
                }
                this.bouncer = bounce;
            }

            @Override
            public int getAccess() {
                return access == null ? 0 : access;
            }

            public void forceName(String value) {
                this.force = value;
            }

            public void setOverrides(Set<MethodRef> value) {
                this.overrides = value.isEmpty() ? null : value;
            }

            public Set<MethodRef> getOverrides() {
                return this.overrides == null ? Collections.emptySet() : this.overrides;
            }

            public String getSignature() {
                return this.signature == null ? "" : this.signature;
            }

            public String getForce() {
                return this.force == null ? "" : this.force;
            }

            public String getName() {
                return this.name;
            }

            public String getDesc()
            {
                return desc;
            }

            @Override
            public String toString() {
                return Utils.getAccess(getAccess()) + ' ' + this.name + ' ' + this.desc;
            }
        }
    }


    private static class Bounce {
        private final MethodRef target;
        private MethodRef owner;

        private Bounce(MethodRef target) {
            this.target = target;
        }

        public void setOwner(MethodRef value) {
            this.owner = value;
        }

        @Override
        public String toString() {
            return this.target + " -> " + this.owner;
        }
    }
}
