package com.controller;

import com.common.def.ServiceResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class BaseController
{


    @ExceptionHandler
    public ServiceResponse buildError()
    {

        return new ServiceResponse();
    }

}
