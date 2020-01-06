package com.ass;

import com.exception.*;

public class AssFact // Short for AssertFactory
{
    public static AssertUtil NOT_FOUND = new AssertUtil(NotFoundException.class);
    public static AssertUtil BAD_ARGUMENT = new AssertUtil(BadArgumentException.class);
    public static AssertUtil DUPLICATE = new AssertUtil(DuplicateEntityException.class);
    public static AssertUtil VALIDATION = new AssertUtil(ValidationException.class);
    public static AssertUtil SERVICE_ERROR = new AssertUtil(ServiceRuntimeException.class);
    public static AssertUtil AUTHORIZATION = new AssertUtil(NotAuthorizedException.class);
    public static AssertUtil THROTTLE = new AssertUtil(ThrottleException.class);
}
