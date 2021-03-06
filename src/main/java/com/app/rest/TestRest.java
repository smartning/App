package com.app.rest;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import com.app.action.factory.TestActionFactory;
import com.app.domain.output.test.TestInfoOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Path("/test")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class TestRest {
    private static final Logger logger = LoggerFactory.getLogger(TestRest.class);
    @GET
    @Path("/info")
    public TestInfoOutput test(@QueryParam("flag") boolean flag) throws Exception {
        return TestActionFactory.getTestInfoAction(flag).execute();
    }

    @POST
    @Path("/hook")
    public String hookTest(Map<String, Object> body){
        if (body != null)
            logger.info("receiver result is{}", body.toString());
        return "receiver hook result";
    }
}
