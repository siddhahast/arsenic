package com.ass;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.common.def.ThreadLocalKey;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import com.dibs.service.common.ServiceResponse;
import com.dibs.service.common.response.ServiceResponseCode;

public class AssertUtil
{

    private final Class<? extends RuntimeException> exceptionClass;
    private Boolean hasStringConstuctor;

    private Pattern verticalKeyPattern = Pattern.compile("^[a-z]_\\d+");

    public AssertUtil()
    {
        this(RuntimeException.class);
    }

    public AssertUtil(Class<? extends RuntimeException> exceptionClass)
    {
        this.exceptionClass = exceptionClass;
    }

    public void isNotNull(Object o, Object... args)
    {
        if (o == null)
        {
            fail(hasArgs(args) ? (Object[]) args : new Object[] { "expected not null" });
        }
    }

    public void isNull(Object o, Object... args)
    {
        if (o != null)
        {
            fail(hasArgs(args) ? (Object[]) args : new Object[] { "expected null" });
        }
    }

    public void isTrue(boolean b, Object... args)
    {
        if (!b)
        {
            fail(hasArgs(args) ? (Object[]) args : new Object[] { "expected true condition" });
        }
    }

    public void isFalse(boolean b, Object... args)
    {
        if (b)
        {
            fail(hasArgs(args) ? (Object[]) args : new Object[] { "expected false condition" });
        }
    }

    public void hasText(CharSequence str, Object... args)
    {
        if (str == null || str.toString().trim().length() == 0)
        {
            fail((Object[]) (hasArgs(args) ? args : "expected text"));
        }
    }

    public void isNotEmpty(Iterable<?> iteratable, Object... args)
    {
        if (!hasArgs(args))
        {
            args = new String[] { "expected non empty iterable" };
        }

        isNotNull(iteratable, args);
        isTrue(iteratable.iterator().hasNext(), args);
    }

    public void isNotEmpty(Object[] array, Object... args)
    {
        if (!hasArgs(args))
        {
            args = new String[] { "expected non empty array" };
        }

        isNotNull(array, args);
        isTrue(array.length > 0, args);
    }

    public void isEqual(Object expected, Object actual, Object... args)
    {
        if (testEqual(expected, actual))
        {
            return;
        }

        if (args == null)
        {
            args = new String[] { "expected equality" };
        }

        Class[] types = getTypes(args);

        if (types == null && hasStringConstuctor())
        {
            fail(new Object[] { "expected:<" + expected + "> but was:<" + actual + ">" });
        }
        else if (types.length == 1 && types[0] == String.class && hasStringConstuctor())
        {
            fail(new Object[] { "[" + args[0] + "] expected:<" + expected + "> but was:<" + actual + ">" });
        }
        else
        {
            fail(args);
        }

    }

    public void isValidVerticalKey(CharSequence key, Object... args)
    {
        isValidRegex(key, verticalKeyPattern, args);
    }

    public void isValidRegex(CharSequence s, Pattern p, Object... args)
    {
        isNotNull(s);
        isNotNull(p);

        Matcher matcher = p.matcher(s);
        if (!matcher.matches())
        {
            fail(hasArgs(args) ? (Object[]) args : new Object[] { "expected regex match" });
        }
    }

    public boolean testEqual(Object expected, Object actual)
    {
        if (expected == null && actual == null)
        {
            return true;
        }

        if (expected != null && expected.equals(actual))
        {
            return true;
        }

        return false;
    }

    public <T> void hasResult(ServiceResponse<T> serviceResponse, Object... args)
    {
        isValid(serviceResponse, args);
        isNotNull(serviceResponse.getResult(), args);
    }

    public <T> void isValid(ServiceResponse<T> serviceResponse, Object... args)
    {
        isNotNull(serviceResponse, args);

        Object[] argsArray = args;

        if (serviceResponse.getMessage() != null)
        {
            if (argsArray == null || argsArray.length == 0)
            {
                argsArray = new Object[] { serviceResponse.getMessage() };
            }
            else if (argsArray[0].getClass() == String.class)
            {
                argsArray[0] = argsArray[0] + ": " + serviceResponse.getMessage();
            }

        }
        isNotNull(serviceResponse.getHttpCode(), argsArray);
        isTrue(serviceResponse.getHttpCode().isSuccess(), argsArray);
    }

    public void fail(Object... args)
    {
        try
        {
            args = arrayFormat(args);

            Class[] constructorTypes = getTypes(args);

            Constructor<? extends RuntimeException> constructor = null;

            if (constructorTypes == null)
            {
                constructor = exceptionClass.getConstructor();
            }
            else
            {
                constructor = getConstructor(constructorTypes);
            }

            if (constructor == null)
            {
                // fail case in which constructor with certain params doesn't exist
                throw exceptionClass.getConstructor().newInstance();
            }
            else
            {
                throw constructor.newInstance(args);
            }

        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException ex)
        {
            throw new RuntimeException("Error throwing assert exception", ex);
        }
    }

    private Constructor getConstructor(Class[] types)
    {
        try
        {
            Constructor constructor = null;
            if (types == null)
            {
                constructor = exceptionClass.getConstructor();
            }
            else
            {
                Constructor<?>[] constructors = exceptionClass.getConstructors();

                for (Constructor<?> currConstructor : constructors)
                {
                    if (isConstructorTypesMatch(currConstructor, types))
                    {
                        constructor = currConstructor;
                        break;
                    }
                }
            }

            return constructor == null ? exceptionClass.getConstructor() : constructor;
        }
        catch (Exception ex)
        {
            return null;
        }
    }
    private boolean isConstructorTypesMatch(Constructor<?> constructor, Class[] types)
    {
        Class<?>[] constructorParameterTypes = constructor.getParameterTypes();

        if (constructorParameterTypes.length != types.length)
        {
            return false;
        }

        for (int i = 0; i < constructorParameterTypes.length; i++)
        {
            if (!constructorParameterTypes[i].isAssignableFrom(types[i]))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean hasArgs(Object[] args)
    {
        return args != null && args.length > 0;
    }

    private static Class[] getTypes(Object[] args)
    {
        if (args == null || args.length == 0)
        {
            return null;
        }

        Class[] types = new Class[args.length];

        for (int i = 0; i < args.length; i++)
        {
            types[i] = args[i].getClass();
        }

        return types;

    }

    private boolean hasStringConstuctor()
    {
        try
        {
            if (hasStringConstuctor == null)
            {
                exceptionClass.getConstructor(String.class);
                this.hasStringConstuctor = true;
            }
        }
        catch (Exception ex)
        {
            this.hasStringConstuctor = false;
        }

        return hasStringConstuctor;
    }

    private Object[] arrayFormat(Object[] args)
    {
        Object[] result = args;

        if (args != null && args.length > 1 && args[0] != null && args[0].getClass().equals(String.class))
        {
            String firstStringArg = (String) args[0];

            if (firstStringArg.contains("{}"))
            {
                Object[] formatArgs = Arrays.copyOfRange(args, 1, args.length);
                FormattingTuple formattingTuple = MessageFormatter.arrayFormat(firstStringArg, formatArgs);
                result = new String[] { formattingTuple.getMessage() };
            }
        }

        return result;
    }

    /* builder methods to access extension to AssertUtil */
    // adding service code response
    public AssertUtil setServiceResponseCode(ServiceResponseCode serviceResponseCode)
    {
        return new ServiceResponseCodeAssertUtil(this, serviceResponseCode);
    }

    // wrapper classes to extend functionality
    private class ServiceResponseCodeAssertUtil extends AssertUtil
    {
        AssertUtil assertUtil;
        ServiceResponseCode serviceResponseCode;

        private ServiceResponseCodeAssertUtil(AssertUtil assertUtil, ServiceResponseCode serviceResponseCode)
        {
            this.assertUtil = assertUtil;
            this.serviceResponseCode = serviceResponseCode;
        }

        @Override
        public void fail(Object...objects)
        {
            ThreadLocalKey.SERVICE_RESPONSE_CODE.put(serviceResponseCode);

            assertUtil.fail(objects);
        }
    }
}
