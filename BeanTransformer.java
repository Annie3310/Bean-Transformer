import annotation.PropertyTransformer;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bean transformer, from multi types to multi types
 *
 * @author Jinyi Wang
 * @date 2023/2/17 15:43
 */
public class BeanTransformer {
    private static final String DIFFERENT_TYPE_EXCEPTION_TEXT = "Different field type, field: %s, source field type: %s, target field type: %s";
    private static final String DIFFERENT_READ_METHOD_RETURN_TYPE = "Different getter return type, field: %s";
    private static final String DIFFERENT_WRITE_METHOD_PARAM_TYPE = "Different setter param type, field: %s";
    private static final String CREATE_PROPERTY_DESCRIPTOR_ERROR = "An Exception occurred while creating property descriptor";
    private static final String CONSTRUCTOR_IS_NOT_PUBLIC = "Class: %s Constructor is not public";
    private static final String INVOKE_CONSTRUCTOR_ERROR = "An Exception occurred while Invoking constructor, Class: %s";

    /**
     * Marks whether the chain is getter or setter
     */
    public enum METHOD_TYPE {
        READ,
        WRITE
    }

    /**
     * transform objects from some types into some other types
     *
     * @param sources source objects
     * @param targets target objects
     */
    public static void process(List<?> sources, Object... targets) {
        // build a map of (source obj: (property linker: read/write method chain))
        Map<Class<?>, ?> sourceClassObjMap = sources.stream().collect(Collectors.toMap(Object::getClass, v -> v, (o, t) -> o));
        List<Class<?>> sourceClasses = new ArrayList<>(sourceClassObjMap.keySet());
        Map<Class<?>, Map<String, LinkedList<Method>>> sourceClassLinkerMethodChainMap = MethodChainBuilder.buildMethodChain(sourceClasses, METHOD_TYPE.READ);

        // build a map of (target type class: (property linker: read/write method chain))
        Map<Class<?>, ?> targetClassObjMap = Stream.of(targets).collect(Collectors.toMap(Object::getClass, v -> v, (o, t) -> o));
        List<Class<?>> targetClasses = new ArrayList<>(targetClassObjMap.keySet());
        Map<Class<?>, Map<String, LinkedList<Method>>> targetClassLinkerMethodChainMap = MethodChainBuilder.buildMethodChain(targetClasses, METHOD_TYPE.WRITE);

        Map<String, LinkedList<Method>> targetMap = new HashMap<>();
        for (Map<String, LinkedList<Method>> value : targetClassLinkerMethodChainMap.values()) {
            targetMap.putAll(value);
        }

        Set<String> removeList = checkField(targetMap);
        // remove duplicate linker
        for (String s : removeList) {
            for (Map<String, LinkedList<Method>> value : sourceClassLinkerMethodChainMap.values()) {
                value.remove(s);
            }
            for (Map<String, LinkedList<Method>> value : targetClassLinkerMethodChainMap.values()) {
                value.remove(s);
            }
        }

        Map<Object, Map<String, LinkedList<Method>>> sourceObjLinkerMethodChainMap = new HashMap<>();
        for (Map.Entry<Class<?>, Map<String, LinkedList<Method>>> entry : sourceClassLinkerMethodChainMap.entrySet()) {
            sourceObjLinkerMethodChainMap.put(sourceClassObjMap.get(entry.getKey()), entry.getValue());
        }

        Map<Object, Map<String, LinkedList<Method>>> targetObjLinkerMethodChainMap = new HashMap<>();
        for (Map.Entry<Class<?>, Map<String, LinkedList<Method>>> entry : targetClassLinkerMethodChainMap.entrySet()) {
            targetObjLinkerMethodChainMap.put(targetClassObjMap.get(entry.getKey()), entry.getValue());
        }

        for (Map.Entry<Object, Map<String, LinkedList<Method>>> targetObjLinkerMethodEntry : targetObjLinkerMethodChainMap.entrySet()) {
            Object obj = targetObjLinkerMethodEntry.getKey();

            for (Map.Entry<String, LinkedList<Method>> targetLinkerMethodChainEntry : targetObjLinkerMethodEntry.getValue().entrySet()) {
                String linker = targetLinkerMethodChainEntry.getKey();
                LinkedList<Method> setterChain = targetLinkerMethodChainEntry.getValue();
                if (setterChain == null) {
                    setterChain = new LinkedList<>();
                }
                for (Map.Entry<Object, Map<String, LinkedList<Method>>> objLinkerChainEntry : sourceObjLinkerMethodChainMap.entrySet()) {
                    Map<String, LinkedList<Method>> sourceLinkerMethodChainMap = objLinkerChainEntry.getValue();
                    if (!sourceLinkerMethodChainMap.containsKey(linker)) {
                        continue;
                    }
                    Object readValue = MethodChainBuilder.executeMethodChain(sourceLinkerMethodChainMap.get(linker), objLinkerChainEntry.getKey(), null);

                    MethodChainBuilder.executeMethodChain(setterChain, obj, readValue);
                }
            }
        }
    }

    /**
     * check if same linkers have different type
     *
     * @param targetMap target linker to method chain map
     * @return duplicate linker set
     */
    private static Set<String> checkField(Map<String, LinkedList<Method>> targetMap) {
        HashSet<LinkedList<Method>> distinctTargetSet = new HashSet<>();
        HashSet<String> removeList = new HashSet<>();
        for (Map.Entry<String, LinkedList<Method>> entry : targetMap.entrySet()) {
            LinkedList<Method> value = entry.getValue();
            String key = entry.getKey();
            if (distinctTargetSet.contains(value)) {
                removeList.add(key);
            } else {
                distinctTargetSet.add(value);
            }
        }
        return removeList;
    }

    /**
     * TODO
     * Check the type of the filed, whether the param type and return type are consistent
     *
     * @param sourceType source type
     * @param targetType target type
     */
    private static void checkDescriptor(String name, Class<?> sourceType, Class<?> targetType) {
        // 字段类型是否一致
        PropertyDescriptor source;
        PropertyDescriptor target;

        try {
            source = new PropertyDescriptor(name, sourceType);
            target = new PropertyDescriptor(name, targetType);
        } catch (IntrospectionException e) {
            throw new RuntimeException(CREATE_PROPERTY_DESCRIPTOR_ERROR, e);
        }

        if (!Objects.equals(sourceType, targetType)) {
            throw new IllegalArgumentException(String.format(DIFFERENT_TYPE_EXCEPTION_TEXT, name, sourceType.getTypeName(), targetType.getTypeName()));
        }

        // source、target 的 getter 检查
        Method sourceReadMethod = source.getReadMethod();
        Method targetReadMethod = target.getReadMethod();
        // getter 返回类型是否相同
        if (!Objects.equals(sourceReadMethod.getReturnType(), targetReadMethod.getReturnType())) {
            throw new IllegalArgumentException(String.format(DIFFERENT_READ_METHOD_RETURN_TYPE, name));
        }

        // source、target 的 setter 检查
        Method sourceWriteMethod = source.getWriteMethod();
        Method targetWriteMethod = target.getWriteMethod();
        // setter 入参是否相同
        if (!Objects.equals(sourceWriteMethod.getParameterCount(), targetWriteMethod.getParameterCount()) &&
                !Arrays.equals(sourceWriteMethod.getParameterTypes(), targetWriteMethod.getParameterTypes())
        ) {
            throw new IllegalArgumentException(String.format(DIFFERENT_WRITE_METHOD_PARAM_TYPE, name));
        }
    }


    public static class MethodChainBuilder {
        private static final String INVOKE_METHOD_CHAIN_NULL_POINTER = "Method: %s returned null value and the chain still has values";
        private static final String DUPLICATE_LINKER_VALUE = "Duplicate linker value: %s in class: %s";


        /**
         * Build getter or setter Chain from some classes
         *
         * @param classes    source classes
         * @param methodType Marks whether the chain is getter or setter
         * @return class: (linker: method chain)
         */
        public static Map<Class<?>, Map<String, LinkedList<Method>>> buildMethodChain(List<Class<?>> classes, METHOD_TYPE methodType) {
            checkDuplicate(classes);
            LinkedList<Method> chain = new LinkedList<>();
            Map<Class<?>, Map<String, LinkedList<Method>>> result = new LinkedHashMap<>();
            for (Class<?> aClass : classes) {
                Map<String, LinkedList<Method>> methodMap = new LinkedHashMap<>();
                buildMethodChain(aClass, chain, methodMap, methodType);
                result.put(aClass, methodMap);
            }
            return result;
        }

        /**
         * check if source objects contain duplicate linkers
         *
         * @param classes classes of source objects
         */
        private static void checkDuplicate(List<Class<?>> classes) {
            Set<String> set = new HashSet<>();
            for (Class<?> aClass : classes) {
                for (Field field : aClass.getDeclaredFields()) {
                    PropertyTransformer annotation = field.getAnnotation(PropertyTransformer.class);
                    if (annotation != null) {
                        String[] value = annotation.value();
                        if (!Arrays.equals(PropertyTransformer.DEFAULT_VALUE, value)) {
                            for (String s : value) {
                                if (set.contains(s)) {
                                    throw new IllegalArgumentException(String.format(DUPLICATE_LINKER_VALUE, s, aClass.getName()));
                                } else {
                                    set.add(s);
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * build a method chain from source class, in other words, find a path of a tree from the root node to all leaf nodes
         *
         * @param clazz     class
         * @param chain     invoke chain from root to leaf
         * @param methodMap map linker to the invoke chain
         */
        private static void buildMethodChain(Class<?> clazz, LinkedList<Method> chain, Map<String, LinkedList<Method>> methodMap, METHOD_TYPE methodType) {
            // traverse all fields of the param "clazz"
            for (Field field : clazz.getDeclaredFields()) {
                // get all fields which contains annotation "PropertyTransformer"
                if (field.isAnnotationPresent(PropertyTransformer.class)) {
                    PropertyTransformer values = field.getAnnotation(PropertyTransformer.class);
                    String[] value = values.value();
                    if (Arrays.equals(value, PropertyTransformer.DEFAULT_VALUE)) {
                        addMethodToChain(chain, field, METHOD_TYPE.READ);
                        buildMethodChain(field.getType(), chain, methodMap, methodType);
                    } else {
                        // if the value of the annotation is not default, the field is the leaf node, build the complete chain
                        buildMethodChain(field, chain, methodMap, value[0], methodType);
                    }
                    if (value.length > 1) {
                        for (int i = 1; i < value.length; i++) {
                            methodMap.put(value[i], methodMap.get(value[0]));
                        }
                    }
                }
            }

            /*
             when a loop ends, it indicates that all fields in the nested field have been traversed, after that pop the last mode from the chain
             e.g. A field of type Test2 is declared in Test1, The fields 'userId' and 'deptId' are declared in Test2
             invoke chain now is Test1.getTest2.getUserId, when the leaf node has been built completely, it will pop from the chain

             now the chain is Test1.getTest2

             when traversing to deptId, add 'getDeptId' to the chain

             now the chain is Test1.getTest2.getDeptId

             pop the getDeptId

             when the Test2 have been traversed, pop it

             now the chain is Test1.
            */
            if (!chain.isEmpty()) {
                chain.removeLast();
            }
        }

        /**
         * build a complete chain
         *
         * @param field                leaf node
         * @param methodChain          complete chain
         * @param methodMap            map linker to method chain
         * @param transformerTypeValue linker
         * @param methodType           whether the type of the method is a getter or setter
         */
        private static void buildMethodChain(Field field, LinkedList<Method> methodChain, Map<String, LinkedList<Method>> methodMap, String transformerTypeValue, METHOD_TYPE methodType) {
            addMethodToChain(methodChain, field, methodType);
            methodMap.put(transformerTypeValue, new LinkedList<>(methodChain));
            // remove current leaf node from the method chain, fallback to parent node, let the processor find other sibling node
            methodChain.removeLast();
        }

        /**
         * invoke the method chain
         *
         * @param methodChain method chain
         * @param caller      An object who will invoke the method
         * @return Result of the Last method of the chain
         */
        public static Object executeMethodChain(LinkedList<Method> methodChain, Object caller, Object value) {
            // get the method that will be invoked
            Method method = methodChain.pollFirst();
            Object invoke;
            try {
                Objects.requireNonNull(method, "Param method is null");
                // if the number of the params is 0, the method is a getter
                // if it is 1, the method is setter
                // otherwise throw an Exception
                if (method.getParameterCount() == 0) {
                    invoke = method.invoke(caller);
                } else {
                    invoke = method.invoke(caller, value);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("An Exception occurs when a method invoked by reflect", e);
            }

            boolean empty = methodChain.isEmpty();
            // if the invoke chain is not empty, recursively invoke itself
            if (!empty) {
                if (invoke == null) {
                    try {
                        invoke = invokeConstructor(method.getReturnType());
                        Method setter = getSetterFromGetter(method, caller.getClass());
                        setter.invoke(caller, invoke);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException("Can not get setter from getter: " + method.getName(), e);
                    } catch (Exception e) {
                        throw new NullPointerException(String.format(INVOKE_METHOD_CHAIN_NULL_POINTER, method.getName()));
                    }
                }
                return executeMethodChain(methodChain, invoke, value);
            } else {
                return invoke;
            }
        }

        /**
         * Add a method to the end of the invoke chain
         *
         * @param chain      invoke chain
         * @param field      field of the getter / setter
         * @param methodType READ / WRITE method, the difference will be reflected in whether the last function of the invoke chain is read or written
         */
        private static void addMethodToChain(LinkedList<Method> chain, Field field, METHOD_TYPE methodType) {
            try {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), field.getDeclaringClass());
                if (METHOD_TYPE.READ.equals(methodType)) {
                    chain.add(propertyDescriptor.getReadMethod());
                } else {
                    chain.add(propertyDescriptor.getWriteMethod());
                }
            } catch (IntrospectionException e) {
                // An Exception occurs when executing introspection, may getter or setter not found
                throw new RuntimeException(e);
            }
        }

        private static Object invokeConstructor(Class<?> aClass) {
            try {
                Constructor<?> constructor = aClass.getConstructor();
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    throw new IllegalAccessException(String.format(CONSTRUCTOR_IS_NOT_PUBLIC, aClass.getName()));
                }
                return constructor.newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(String.format(INVOKE_CONSTRUCTOR_ERROR, aClass.getName()));
            }
        }

        private static final String GET_PREFIX = "get";
        private static final String IS_PREFIX = "is";
        private static final String SET_METHOD_NAME_TEMPLATE = "set%s";

        /**
         * get setter method from getter method
         *
         * @param getter getter
         * @param caller class which declared the getter and setter
         * @return setter
         * @throws NoSuchMethodException throw this exception when there is no setter in caller
         */
        private static Method getSetterFromGetter(Method getter, Class<?> caller) throws NoSuchMethodException {
            String methodName = getter.getName();
            if (methodName.startsWith(GET_PREFIX)) {
                methodName = String.format(SET_METHOD_NAME_TEMPLATE, methodName.substring(GET_PREFIX.length()));
            } else if (methodName.startsWith(IS_PREFIX)) {
                methodName = String.format(SET_METHOD_NAME_TEMPLATE, methodName.substring(IS_PREFIX.length()));
            }
            // other condition is impossible
            return caller.getMethod(methodName, getter.getReturnType());
        }
    }
}
