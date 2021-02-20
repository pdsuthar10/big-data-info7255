package com.info7255.demo.controller;

import com.info7255.demo.exception.BadRequestException;
import com.info7255.demo.exception.ConflictException;
import com.info7255.demo.exception.ETagParseException;
import com.info7255.demo.exception.ResourceNotFoundException;
import com.info7255.demo.service.ETagService;
import com.info7255.demo.service.PlanService;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PlanController {
    private PlanService planService;
    private ETagService eTagService;

    public PlanController( PlanService planService, ETagService eTagService ) {
        this.planService = planService;
        this.eTagService = eTagService;
    }

    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPlan( @RequestBody String planObject ) {
        JSONObject plan = new JSONObject(planObject);
        JSONObject schemaJSON = new JSONObject(new JSONTokener(PlanController.class.getResourceAsStream("/plan-schema.json")));
        Schema schema = SchemaLoader.load(schemaJSON);
        try {
            schema.validate(plan);
        } catch (ValidationException e) {
            throw new BadRequestException(e.getMessage());
        }

        if ( planService.isKeyPresent(plan.getString("objectId")) ) throw new ConflictException("Plan already exists!");

        String objectId = planService.createPlan(plan);
        String eTag = eTagService.getETag(plan);
        MultiValueMap<String, String> headersToSend = new LinkedMultiValueMap<>();
        headersToSend.add("ETag", eTag);

        return new ResponseEntity<>("{\"objectId\": \"" + objectId + "\"}", headersToSend, HttpStatus.CREATED);
    }

    @GetMapping(value = "/plan/{objectId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPlan( @PathVariable String objectId, @RequestHeader HttpHeaders headers ) {
        if ( !planService.isKeyPresent(objectId) ) throw new ResourceNotFoundException("Object with the given objectID not found!");

        JSONObject object = planService.getPlan(objectId);
        String eTag = eTagService.getETag(object);
        MultiValueMap<String, String> headersToSend = new LinkedMultiValueMap<>();
        headersToSend.add("ETag", eTag);


        List<String> ifNoneMatch;
        try {
            ifNoneMatch = headers.getIfNoneMatch();
        } catch (Exception e) {
            throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
        }

        return (eTagService.verifyETag(object, ifNoneMatch)) ?
                (new ResponseEntity<>(null, headersToSend, HttpStatus.NOT_MODIFIED))
                :
                (new ResponseEntity<>(object.toString(), headersToSend, HttpStatus.OK));
    }

    @DeleteMapping("/plan/{objectId}")
    public ResponseEntity<?> deletePlan( @PathVariable String objectId ) {
        if ( !planService.isKeyPresent(objectId) ) throw new ResourceNotFoundException("Plan not found!");
        planService.deletePlan(objectId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
